package com.har01d.ocula

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
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext


open class Spider<T>(val crawler: Crawler? = null, val parser: Parser<T>, configure: Spider<T>.() -> Unit = {}) : Config(), Context {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Spider::class.java)
    }

    override lateinit var name: String
    val requests = mutableListOf<Request>()
    val preHandlers = mutableListOf<PreHandler>()
    val postHandlers = mutableListOf<PostHandler>()
    val resultHandlers = mutableListOf<ResultHandler<T>>()
    val listeners = mutableListOf<Listener<T>>(StatisticListener().apply { spider = this@Spider })
    lateinit var httpClient: HttpClient
    var status: Status = Status.IDLE
        private set

    private var finished = false
    private var stoped = false
    private lateinit var coroutineContext: CoroutineContext

    constructor(parser: Parser<T>, vararg urls: String, configure: Spider<T>.() -> Unit = {}) : this(null, parser, configure) {
        requests += urls.map { Request(it) }
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg urls: String, configure: Spider<T>.() -> Unit = {}) : this(crawler, parser, configure) {
        requests += urls.map { Request(it) }
    }

    init {
        this.configure()
    }

    /**
     * Add initial urls, Spider starts from these urls.
     */
    fun addUrl(vararg url: String) {
        url.forEach {
            requests += Request(it)
        }
    }

    /**
     * Add initial requests, Spider starts from these requests.
     */
    fun addRequest(vararg request: Request) {
        requests += request
    }

    /**
     * Add urls to the crawler queue.
     */
    override fun crawl(refer: String, vararg urls: String) = enqueue(crawler?.queue!!, crawler.dedupHandler!!, refer, *urls)

    /**
     * Add requests to the crawler queue.
     */
    override fun crawl(refer: String, vararg requests: Request) = enqueue(crawler?.queue!!, crawler.dedupHandler!!, refer, *requests)

    /**
     * Add urls to the parser queue.
     */
    override fun follow(refer: String, vararg urls: String) = enqueue(parser.queue!!, parser.dedupHandler!!, refer, *urls)

    /**
     * Add requests to the parser queue.
     */
    override fun follow(refer: String, vararg requests: Request) = enqueue(parser.queue!!, parser.dedupHandler!!, refer, *requests)

    /**
     * Add urls to the queue, will normalize the url by refer URL, check duplication before add them.
     */
    private fun enqueue(queue: RequestQueue, dedupHandler: DedupHandler, refer: String, vararg urls: String): Boolean {
        return enqueue(queue, dedupHandler, refer, *urls.map { Request(it) }.toTypedArray())
    }

    /**
     * Add requests to the queue.
     * Invalid urls are ignored, eg.: "", "#", "javascript:void(0);".
     * RobotsHandler checks if the urls can access by robots.txt.
     * DedupHandler checks if the requests should handle.
     */
    private fun enqueue(queue: RequestQueue, dedupHandler: DedupHandler, refer: String, vararg requests: Request): Boolean {
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
                if (!http.robotsHandler.handle(req)) {
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

    private fun validateUrl(url: String): Boolean {
        if (url.isBlank() || url == "#" || url.startsWith("javascript:")) {
            return false
        }
        return true
    }

    /**
     * Dispatch HTTP request by FuelHttpClient.
     */
    override fun dispatch(request: Request) = httpClient.dispatch(request)

    /**
     * Indicate no more new tasks, wait tasks in queue finish.
     */
    override fun finish() {
        finished = true
    }

    /**
     * Stop the Spider, ignore the tasks in queue.
     */
    override fun stop() {
        stoped = true
        // TODO: how to stop before RUNNING?
        if (status == Status.STARTED || status == Status.RUNNING) {
            status = Status.CANCELLED
        }
    }

    /**
     * Start the Spider and block the current thread.
     */
    fun run() = runBlocking {
        start()
    }

    /**
     * Start the Spider in coroutine.
     */
    suspend fun start() {
        validate()
        prepare()

        logger.info("Spider $name Started")
        listeners.forEach { it.onStart() }
        preHandle()

        val jobs = mutableListOf<Job>()

        if (crawler != null) {
            requests.forEach {
                crawler.queue!!.push(it)
            }

            crawler.context = this@Spider
            val job = GlobalScope.plus(coroutineContext).launch {
                crawl(this)
            }
            jobs += job
        } else {
            requests.forEach {
                parser.queue!!.push(it)
            }
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
        crawler?.let {
            it.queue = it.queue ?: InMemoryRequestQueue()
            it.dedupHandler = it.dedupHandler ?: HashSetDedupHandler()
        }
        parser.queue = parser.queue ?: InMemoryRequestQueue()
        parser.dedupHandler = parser.dedupHandler ?: HashSetDedupHandler()
        http.robotsHandler.init(requests)
        authHandler?.let {
            preHandlers += authHandler!!
        }
        http.userAgentProvider = http.userAgentProvider ?: RoundRobinUserAgentProvider(http.userAgents)
        http.proxyProvider = http.proxyProvider ?: RoundRobinProxyProvider(http.proxies)
        initHttpClient()
    }

    open fun initHttpClient() {
        if (!this::httpClient.isInitialized) {
            httpClient = FuelHttpClient()
        }
        crawler?.let {
            it.httpClient = it.httpClient ?: httpClient
            val client = it.httpClient!!
            client.userAgentProvider = http.userAgentProvider!!
            client.proxyProvider = http.proxyProvider!!
            client.charset = http.charset
        }

        parser.httpClient = parser.httpClient ?: httpClient
        val client = parser.httpClient!!
        client.userAgentProvider = http.userAgentProvider!!
        client.proxyProvider = http.proxyProvider!!
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
        for (entry in http.headers) {
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
            val request = crawler!!.queue!!.poll(1000L)
            if (request != null) {
                try {
                    setHeaders(request, referer)
                    count.incrementAndGet()
                    crawler.httpClient!!.dispatch(request) { result ->
                        result.onSuccess { response ->
                            listeners.forEach { it.onDownloadSuccess(request, response) }
                            try {
                                crawler.handle(request, response)
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

            if (stoped || (finished && crawler.queue!!.isEmpty() && count.get() == 0)) {
                break
            }
        }
    }

    private suspend fun parse(scope: CoroutineScope) {
        var referer: String? = null
        status = Status.RUNNING
        while (scope.isActive) {
            val request = parser.queue!!.poll(1000L)
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

            if (stoped || (finished && parser.queue!!.isEmpty() && count.get() == 0)) {
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
