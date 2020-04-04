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
import com.har01d.ocula.util.normalizeUrl
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext


open class Spider<T>(private val parser: Parser<T>, configure: Spider<T>.() -> Unit = {}) : Config(), Context {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Spider::class.java)

        init {
            Configuration.setDefaults(object : Configuration.Defaults {
                private val mapper = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

                override fun jsonProvider() = JacksonJsonProvider(mapper)
                override fun mappingProvider() = JacksonMappingProvider(mapper)
                override fun options() = EnumSet.noneOf(Option::class.java)
            })
        }
    }

    val requests = mutableListOf<Request>()
    var userAgents: List<String> = http.defaultUserAgents
    var userAgentProvider: UserAgentProvider? = null
    var httpHeaders: Map<String, Collection<String>> = http.defaultHttpHeaders
    val httpProxies = mutableListOf<HttpProxy>()
    var proxyProvider: ProxyProvider? = null
    var authHandler: AuthHandler? = null
    var robotsHandler: RobotsHandler = NoopRobotsHandler
    val preHandlers = mutableListOf<PreHandler>()
    val postHandlers = mutableListOf<PostHandler>()
    val resultHandlers = mutableListOf<ResultHandler<T>>()
    val listeners = mutableListOf<Listener<T>>(StatisticListener().apply { spider = this@Spider })
    var httpClient: HttpClient? = null
    var dedupHandler: DedupHandler = HashSetDedupHandler()
    var queueParser: RequestQueue = InMemoryRequestQueue()
    var queueCrawler: RequestQueue = InMemoryRequestQueue()
    var crawler: Crawler? = null
    var status: Status = Status.IDLE
        private set
    override lateinit var name: String
    private var finished = false
    private var stoped = false
    private lateinit var coroutineContext: CoroutineContext

    constructor(crawler: Crawler, parser: Parser<T>, configure: Spider<T>.() -> Unit = {}) : this(parser, configure) {
        this.crawler = crawler
    }

    constructor(parser: Parser<T>, vararg urls: String, configure: Spider<T>.() -> Unit = {}) : this(parser, configure) {
        requests += urls.map { Request(it) }
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg urls: String, configure: Spider<T>.() -> Unit = {}) : this(parser, configure) {
        this.crawler = crawler
        requests += urls.map { Request(it) }
    }

    init {
        this.configure()
    }

    fun addUrl(url: String) {
        requests += Request(url)
    }

    fun basicAuth(username: String, password: String) {
        authHandler = BasicAuthHandler(username, password)
    }

    fun cookieAuth(name: String, value: String) {
        authHandler = CookieAuthHandler(name, value)
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

    override fun crawl(refer: String, vararg urls: String) = enqueue(queueCrawler, refer, *urls)

    override fun crawl(refer: String, vararg requests: Request) = enqueue(queueCrawler, refer, *requests)

    override fun follow(refer: String, vararg urls: String) = enqueue(queueParser, refer, *urls)

    override fun follow(refer: String, vararg requests: Request) = enqueue(queueParser, refer, *requests)

    open fun enqueue(queue: RequestQueue, refer: String, vararg urls: String): Boolean {
        return enqueue(queue, refer, *urls.map { Request(it) }.toTypedArray())
    }

    open fun enqueue(queue: RequestQueue, refer: String, vararg requests: Request): Boolean {
        var success = false
        for (request in requests) {
            val url = request.url
            if (!validateUrl(url)) {
                continue
            }
            val uri = normalizeUrl(refer, url)
            if (uri != null) {
                request.headers["Referer"] = listOf(refer)
                val req = request.copy(url = uri)
                if (!robotsHandler.handle(req)) {
                    listeners.forEach { it.onSkip(req) }
                    logger.debug("Skip {}", req.url)
                    continue
                }
                if (!dedupHandler.handle(req)) {
                    logger.debug("Ignore {}", req.url)
                    continue
                }
                queue.push(req)
                logger.debug("Enqueue {}", req.url)
                success = true
            }
        }
        return success
    }

    open fun enqueue(queue: RequestQueue) {
        requests.forEach {
            queue.push(it)
        }
    }

    open fun validateUrl(url: String): Boolean {
        if (url.isBlank() || url == "#" || url.startsWith("javascript:")) {
            return false
        }
        return true
    }

    override fun dispatch(request: Request) = httpClient!!.dispatch(request)

    override fun finish() {
        finished = true
    }

    fun stop() {
        stoped = true
        if (status == Status.STARTED || status == Status.RUNNING) {
            status = Status.CANCELLED
        }
    }

    fun run() = runBlocking {
        start()
    }

    suspend fun start() {
        validate()
        prepare()

        logger.info("Spider $name Started")
        listeners.forEach { it.onStart() }
        preHandle()

        val jobs = mutableListOf<Job>()

        if (crawler != null) {
            enqueue(queueCrawler)

            crawler!!.context = this@Spider
            val job = GlobalScope.plus(coroutineContext).launch {
                crawl(this)
            }
            jobs += job
        } else {
            enqueue(queueParser)
        }

        parser.context = this@Spider
        coroutineScope {
            repeat(concurrency) {
                val job = launch(coroutineContext) {
                    parse(this)
                }
                jobs += job
            }
        }
        jobs.forEach { it.join() }

        postHandle()
        if (status == Status.CANCELLED) {
            listeners.forEach { it.onCancel() }
        } else {
            listeners.forEach { it.onFinish() }
        }
        logger.info("Spider $name " + status.name.toLowerCase().capitalize())
    }

    open fun validate() {
        if (status == Status.STARTED || status == Status.RUNNING) {
            throw IllegalStateException("Spider is " + status.name.toLowerCase().capitalize())
        }
        if (requests.isEmpty()) {
            throw IllegalStateException("start url is required")
        }
    }

    open fun prepare() {
        finished = false
        stoped = false
        status = Status.STARTED
        if (!this::name.isInitialized) {
            name = URL(requests[0].url).host
        }
        if (concurrency == 0) {
            concurrency = if (crawler != null) {
                Runtime.getRuntime().availableProcessors()
            } else {
                1
            }
        }
        coroutineContext = newFixedThreadPoolContext(concurrency, "Spider")
        if (resultHandlers.isEmpty()) {
            resultHandlers += ConsoleLogResultHandler
        }
        robotsHandler.init(requests)
        authHandler?.let {
            preHandlers += authHandler!!
        }
        userAgentProvider = userAgentProvider ?: RoundRobinUserAgentProvider(userAgents)
        proxyProvider = proxyProvider ?: RoundRobinProxyProvider(httpProxies)
        initHttpClient()
    }

    open fun initHttpClient() {
        httpClient = httpClient ?: FuelHttpClient()
        crawler?.let {
            if (crawler!!.httpClient == null) {
                crawler!!.httpClient = httpClient
            }
            val client = crawler!!.httpClient!!
            client.userAgentProvider = userAgentProvider!!
            client.proxyProvider = proxyProvider!!
            client.charset = http.charset
        }

        if (parser.httpClient == null) {
            parser.httpClient = httpClient
        }
        val client = parser.httpClient!!
        client.userAgentProvider = userAgentProvider!!
        client.proxyProvider = proxyProvider!!
        client.charset = http.charset
    }

    open fun preHandle() {
        requests.forEach { request ->
            preHandlers.forEach {
                it.context = this@Spider
                try {
                    it.handle(request)
                } catch (e: Exception) {
                    listeners.forEach { l -> l.onError(e) }
                    logger.warn("pre handle failed", e)
                }
            }
        }
    }

    open fun postHandle() {
        requests.forEach { request ->
            postHandlers.forEach {
                it.context = this@Spider
                try {
                    it.handle(request)
                } catch (e: Exception) {
                    listeners.forEach { l -> l.onError(e) }
                    logger.warn("post handle failed", e)
                }
            }
        }
        if (status == Status.RUNNING) {
            status = Status.COMPLETED
        }
    }

    open fun setHeaders(request: Request, referer: String?) {
        if (referer != null && !request.headers.containsKey("Referer")) {
            request.headers["Referer"] = listOf(referer)
        }
        for (entry in httpHeaders) {
            if (!request.headers.containsKey(entry.key)) {
                request.headers[entry.key] = entry.value
            }
        }
    }

    private val count = AtomicInteger(0)

    private suspend fun crawl(scope: CoroutineScope) {
        var referer: String? = null
        status = Status.RUNNING
        while (scope.isActive) {
            val request = queueCrawler.poll(1000L)
            if (request != null) {
                try {
                    setHeaders(request, referer)
                    count.incrementAndGet()
                    crawler!!.httpClient!!.dispatch(request) { result ->
                        result.onSuccess { response ->
                            listeners.forEach { it.onDownloadSuccess(request, response) }
                            try {
                                crawler!!.handle(request, response)
                                listeners.forEach { it.onCrawlSuccess(request, response) }
                            } catch (e: Exception) {
                                listeners.forEach { it.onCrawlFailed(request, e) }
                                listeners.forEach { it.onError(e) }
                                logger.warn("Crawl page {} failed", request.url, e)
                            }
                            count.decrementAndGet()
                        }
                        result.onFailure { e ->
                            listeners.forEach { it.onDownloadFailed(request, e) }
                            listeners.forEach { it.onError(e) }
                            logger.warn("Download page {} failed", request.url, e)
                            count.decrementAndGet()
                        }
                    }
                    referer = request.url

                    delay(interval)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    logger.warn("Crawl page {} failed", request.url, e)
                }
            }

            if (stoped || (finished && queueCrawler.isEmpty() && count.get() == 0)) {
                break
            }
        }
    }

    private suspend fun parse(scope: CoroutineScope) {
        var referer: String? = null
        status = Status.RUNNING
        while (scope.isActive) {
            val request = queueParser.poll(1000L)
            if (request != null) {
                try {
                    setHeaders(request, referer)
                    count.incrementAndGet()
                    parser.httpClient!!.dispatch(request) { res ->
                        res.onSuccess { response ->
                            try {
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
                                        listeners.forEach { l -> l.onError(e) }
                                        logger.warn("Handle result failed", e)
                                    }
                                }
                            } catch (e: Throwable) {
                                listeners.forEach { it.onError(e) }
                                logger.warn("Parse page {} failed", request.url, e)
                            }
                            count.decrementAndGet()
                        }
                        res.onFailure { e ->
                            listeners.forEach { it.onDownloadFailed(request, e) }
                            listeners.forEach { it.onError(e) }
                            logger.warn("Download page {} failed", request.url, e)
                            count.decrementAndGet()
                        }
                    }
                    referer = request.url

                    delay(interval)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    logger.warn("Handle page {} failed", request.url, e)
                }
            }

            if (stoped || (finished && queueParser.isEmpty() && count.get() == 0)) {
                break
            }
        }
    }
}

class SimpleSpider<T>(vararg url: String, parse: (request: Request, response: Response) -> T) : Spider<T>(SimpleParser(parse), *url)

enum class Status {
    IDLE,
    STARTED,
    RUNNING,
    ABORTED,
    CANCELLED,
    COMPLETED
}
