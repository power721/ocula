package com.har01d.ocula

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.har01d.ocula.crawler.Crawler
import com.har01d.ocula.handler.*
import com.har01d.ocula.http.*
import com.har01d.ocula.listener.Listener
import com.har01d.ocula.listener.StatisticListener
import com.har01d.ocula.parser.Parser
import com.har01d.ocula.parser.SimpleParser
import com.har01d.ocula.queue.InMemoryRequestQueue
import com.har01d.ocula.queue.RequestQueue
import com.har01d.ocula.queue.enqueue
import com.har01d.ocula.util.defaultHttpHeaders
import com.har01d.ocula.util.defaultUserAgents
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


open class Spider<T>(private val parser: Parser<T>) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Spider::class.java)
    }

    val requests = mutableListOf<Request>()
    var userAgents: List<String> = defaultUserAgents
    var httpHeaders: Map<String, Collection<String>> = defaultHttpHeaders
    val httpProxies = mutableListOf<HttpProxy>()
    var proxyProvider: ProxyProvider = RandomProxyProvider(httpProxies)
    var authHandler: AuthHandler? = null
    val preHandlers = mutableListOf<PreHandler>()
    val postHandlers = mutableListOf<PostHandler>()
    val resultHandlers = mutableListOf<ResultHandler<T>>()
    val listeners = mutableListOf<Listener<T>>(StatisticListener())
    var httpClient: HttpClient = FuelHttpClient()
    var dedupHandler: DedupHandler = HashSetDedupHandler()
    var queueParser: RequestQueue = InMemoryRequestQueue()
    var queueCrawler: RequestQueue = InMemoryRequestQueue()
    var crawler: Crawler? = null
    var interval: Long = 500L
    var concurrency: Int = 0
    private var finished = false

    constructor(parser: Parser<T>, vararg urls: String) : this(parser) {
        requests += urls.map { Request(it) }
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg urls: String) : this(parser) {
        this.crawler = crawler
        requests += urls.map { Request(it) }
    }

    init {
        Configuration.setDefaults(object : Configuration.Defaults {
            private val mapper = jacksonObjectMapper().apply {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
            private val jsonProvider = JacksonJsonProvider(mapper)
            private val mappingProvider = JacksonMappingProvider(mapper)

            override fun jsonProvider() = jsonProvider

            override fun mappingProvider() = mappingProvider

            override fun options(): Set<Option> {
                return EnumSet.noneOf(Option::class.java)
            }
        })
    }

    fun addUrl(url: String) {
        requests += Request(url)
    }

    fun basicAuth(username: String, password: String) {
        authHandler = BasicAuthHandler(username, password)
    }

    fun tokenAuth(token: String, header: String = "Authorization") {
        authHandler = TokenAuthHandler(token, header)
    }

    fun formAuth(actionUrl: String, parameters: Parameters, block: (request: Request, response: Response) -> Unit = sessionHandler) {
        authHandler = FormAuthHandler(actionUrl, parameters, block)
    }

    fun httpProxy(hostname: String, port: Int) {
        httpProxies += HttpProxy(hostname, port)
    }

    fun downloadImages(directory: String) {
        resultHandlers += ImageResultHandler(directory)
        run()
    }

    fun finish() {
        finished = true
    }

    fun crawl(refer: String, vararg urls: String) = queueCrawler.enqueue(refer, *urls)

    fun crawl(refer: String, vararg requests: Request) = queueCrawler.enqueue(refer, *requests)

    fun follow(refer: String, vararg urls: String) = queueParser.enqueue(refer, *urls)

    fun follow(refer: String, vararg requests: Request) = queueParser.enqueue(refer, *requests)

    fun dispatch(request: Request) = httpClient.dispatch(request)

    fun run() = runBlocking {
        validate()
        prepare()

        listeners.forEach { it.onStart() }
        preHandle()

        if (crawler != null) {
            enqueue(queueCrawler)

            crawler!!.spider = this@Spider
            GlobalScope.launch {
                crawl(this)
            }
        } else {
            enqueue(queueParser)
        }

        parser.spider = this@Spider
        val jobs = mutableListOf<Job>()
        repeat(concurrency) {
            val job = GlobalScope.launch {
                parse(this)
            }
            jobs += job
        }
        jobs.forEach { it.join() }

        postHandle()
        listeners.forEach { it.onFinish() }
    }

    private fun validate() {
        if (requests.isEmpty()) {
            throw IllegalStateException("start url is required")
        }
    }

    private fun prepare() {
        if (concurrency == 0) {
            concurrency = if (crawler != null) {
                Runtime.getRuntime().availableProcessors()
            } else {
                1
            }
        }
        if (resultHandlers.isEmpty()) {
            resultHandlers += LogResultHandler
        }
        authHandler?.let {
            preHandlers += authHandler!!
        }
        httpClient.userAgents = userAgents
        httpClient.proxyProvider = proxyProvider
    }

    private fun enqueue(queue: RequestQueue) {
        requests.forEach {
            queue.push(it)
        }
    }

    open fun preHandle() {
        requests.forEach { request ->
            preHandlers.forEach {
                it.spider = this@Spider
                try {
                    it.handle(request)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    logger.warn("pre handle failed", e)
                }
            }
        }
    }

    open fun postHandle() {
        requests.forEach { request ->
            postHandlers.forEach {
                it.spider = this@Spider
                try {
                    it.handle(request)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    logger.warn("post handle failed", e)
                }
            }
        }
    }

    private fun setHeaders(request: Request, referer: String?) {
        if (referer != null && !request.headers.containsKey("Referer")) {
            request.headers["Referer"] = listOf(referer)
        }
        for (entry in httpHeaders) {
            if (!request.headers.containsKey(entry.key)) {
                request.headers[entry.key] = entry.value
            }
        }
    }

    private suspend fun crawl(scope: CoroutineScope) {
        var referer: String? = null
        while (scope.isActive) {
            val request = queueCrawler.take()
            try {
                if (!dedupHandler.handle(request)) {
                    continue
                }
                setHeaders(request, referer)
                val response = try {
                    dispatch(request)
                } catch (e: Exception) {
                    listeners.forEach { it.onDownloadFailed(request, e) }
                    throw e
                }
                referer = request.url
                listeners.forEach { it.onDownloadSuccess(request, response) }

                try {
                    crawler!!.handle(request, response)
                } catch (e: Exception) {
                    listeners.forEach { it.onCrawlFailed(request, e) }
                    throw e
                }
                listeners.forEach { it.onCrawlSuccess(request, response) }

                delay(interval)
            } catch (e: Exception) {
                listeners.forEach { it.onError(e) }
                logger.warn("Crawl pages failed", e)
            }

            if (finished && queueCrawler.isEmpty()) {
                break
            }
        }
    }

    private suspend fun parse(scope: CoroutineScope) {
        var referer: String? = null
        while (scope.isActive) {
            val request = queueParser.poll(1000L)
            if (request != null) {
                try {
                    if (!dedupHandler.handle(request)) {
                        continue
                    }
                    setHeaders(request, referer)
                    val response = try {
                        dispatch(request)
                    } catch (e: Exception) {
                        listeners.forEach { it.onDownloadFailed(request, e) }
                        throw e
                    }
                    referer = request.url
                    listeners.forEach { it.onDownloadSuccess(request, response) }

                    val result = try {
                        parser.parse(request, response)
                    } catch (e: Exception) {
                        listeners.forEach { it.onParseFailed(request, response, e) }
                        throw e
                    }
                    listeners.forEach { it.onParseSuccess(request, response, result) }

                    resultHandlers.forEach {
                        try {
                            it.handle(request, response, result)
                        } catch (e: Exception) {
                            listeners.forEach { it.onError(e) }
                            logger.warn("Handle result failed", e)
                        }
                    }

                    delay(interval)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    logger.warn("Parse page failed", e)
                }
            }

            if (finished && queueParser.isEmpty()) {
                break
            }
        }
    }
}

class SimpleSpider<T>(vararg url: String, parse: (request: Request, response: Response) -> T) : Spider<T>(SimpleParser(parse), *url)
