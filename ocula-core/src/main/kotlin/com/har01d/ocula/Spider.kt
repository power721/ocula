package com.har01d.ocula

import com.har01d.ocula.crawler.Crawler
import com.har01d.ocula.handler.*
import com.har01d.ocula.http.FuelHttpClient
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.listener.Listener
import com.har01d.ocula.listener.StatisticListener
import com.har01d.ocula.parser.Parser
import com.har01d.ocula.queue.InMemoryRequestQueue
import com.har01d.ocula.queue.RequestQueue
import com.har01d.ocula.queue.enqueue
import com.har01d.ocula.util.defaultHttpHeaders
import com.har01d.ocula.util.defaultUserAgents
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class Spider<T>(private val parser: Parser<T>) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Spider::class.java)
    }

    private val requests = mutableListOf<Request>()
    var userAgents: List<String> = defaultUserAgents
    var httpHeaders: List<Pair<String, Collection<String>>> = defaultHttpHeaders
    var authHandler: AuthHandler? = null
    val preHandlers = mutableListOf<PreHandler>()
    val postHandlers = mutableListOf<PostHandler>()
    val resultHandlers = mutableListOf<ResultHandler<T>>()
    val listeners = mutableListOf<Listener<T>>(StatisticListener)
    var httpClient: HttpClient = FuelHttpClient()
    var dedupHandler: DedupHandler = HashSetDedupHandler
    var queueParser: RequestQueue = InMemoryRequestQueue()
    var queueCrawler: RequestQueue = queueParser
    var crawler: Crawler? = null
    var interval: Long = 500L
    private var finished = false

    constructor(parser: Parser<T>, vararg urls: String) : this(parser) {
        requests += urls.map { Request(it) }
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
                crawl()
            }
        } else {
            enqueue(queueParser)
        }

        parser.spider = this@Spider
        val job = GlobalScope.launch {
            parse()
        }
        job.join()

        postHandle()
        listeners.forEach { it.onFinish() }
    }

    private fun validate() {
        if (requests.isEmpty()) {
            throw IllegalStateException("start url is required")
        }
    }

    private fun prepare() {
        if (resultHandlers.isEmpty()) {
            resultHandlers.add(LogResultHandler)
        }
        authHandler?.let {
            preHandlers += authHandler!!
        }
        httpClient.userAgents = userAgents
    }

    private fun enqueue(queue: RequestQueue) {
        requests.forEach {
            queue.push(it)
        }
    }

    private fun preHandle() {
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

    private fun postHandle() {
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
        for (header in httpHeaders) {
            if (!request.headers.containsKey(header.first)) {
                request.headers += header
            }
        }
    }

    private suspend fun crawl() {
        var referer: String? = null
        while (true) {
            val request = queueCrawler.poll()
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

    private suspend fun parse() {
        var referer: String? = null
        while (true) {
            val request = queueParser.poll()
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

            if (finished && queueParser.isEmpty()) {
                break
            }
        }
    }
}
