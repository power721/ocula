package com.har01d.ocula

import com.har01d.ocula.crawler.Crawler
import com.har01d.ocula.handler.*
import com.har01d.ocula.http.*
import com.har01d.ocula.listener.DefaultStatisticListener
import com.har01d.ocula.listener.Listener
import com.har01d.ocula.listener.StatisticListener
import com.har01d.ocula.parser.Parser
import com.har01d.ocula.parser.SimpleParser
import com.har01d.ocula.queue.InMemoryRequestQueue
import com.har01d.ocula.queue.RequestQueue
import com.har01d.ocula.util.normalizeUrl
import com.har01d.ocula.util.path
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


typealias Configure<T> = Spider<T>.() -> Unit

open class Spider<T>(val crawler: Crawler? = null, val parser: Parser<T>, configure: Configure<T> = {}) : Context {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Spider::class.java)
        val id = AtomicInteger(1)
    }

    override lateinit var name: String
    open val config = Config()
    val preHandlers = mutableListOf<PreHandler>()
    val postHandlers = mutableListOf<PostHandler>()
    val resultHandlers = mutableListOf<ResultHandler<T>>()
    val listeners = mutableListOf<Listener>()
    var statisticListener: StatisticListener = DefaultStatisticListener()
    var httpClient: HttpClient = FuelHttpClient()
    private val requests = mutableListOf<Request>()

    var status: Status = Status.IDLE
        private set
    private var finished = false
    private var aborted = false
    private var stoped = false
    private var coroutineContext: CoroutineContext = EmptyCoroutineContext

    constructor(parser: Parser<T>, vararg url: String, configure: Configure<T> = {})
            : this(null, parser, configure) {
        requests += url.map { Request(it) }
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg url: String, configure: Configure<T> = {})
            : this(crawler, parser, configure) {
        requests += url.map { Request(it) }
    }

    constructor(parser: Parser<T>, vararg request: Request, configure: Configure<T> = {})
            : this(null, parser, configure) {
        requests += request
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg request: Request, configure: Configure<T> = {})
            : this(crawler, parser, configure) {
        requests += request
    }

    init {
        this.configure()
    }

    /**
     * Add initial urls, Spider starts from these urls.
     */
    fun addUrl(vararg url: String) {
        if (status == Status.STARTED || status == Status.RUNNING) {
            logger.warn("Spider $name is $status")
        }
        url.forEach {
            requests += Request(it)
        }
    }

    /**
     * Add initial requests, Spider starts from these requests.
     */
    fun addRequest(vararg request: Request) {
        if (status == Status.STARTED || status == Status.RUNNING) {
            logger.warn("Spider $name is $status")
        }
        requests += request
    }

    fun configure(block: Config.() -> Unit) {
        with(config) {
            block()
        }
    }

    fun basicAuth(username: String, password: String) {
        config.authHandler = BasicAuthHandler(username, password)
    }

    fun cookieAuth(name: String, value: String) {
        config.authHandler = CookieAuthHandler(name, value)
    }

    fun tokenAuth(token: String, header: String = "Authorization") {
        config.authHandler = TokenAuthHandler(token, header)
    }

    fun formAuth(actionUrl: String, parameters: Parameters, block: AuthConfigure = sessionHandler) {
        config.authHandler = FormAuthHandler(actionUrl, parameters, block)
    }

    fun httpProxy(hostname: String, port: Int) {
        config.http.proxies += HttpProxy(hostname, port)
    }

    /**
     * Add urls to the crawler queue.
     */
    override fun crawl(refer: String, vararg urls: String) =
        enqueue(crawler?.queue!!, crawler.dedupHandler!!, refer, *urls)

    /**
     * Add requests to the crawler queue.
     */
    override fun crawl(refer: String, vararg requests: Request) =
        enqueue(crawler?.queue!!, crawler.dedupHandler!!, refer, *requests)

    /**
     * Add urls to the parser queue.
     */
    override fun follow(refer: String, vararg urls: String) =
        enqueue(parser.queue!!, parser.dedupHandler!!, refer, *urls)

    /**
     * Add requests to the parser queue.
     */
    override fun follow(refer: String, vararg requests: Request) =
        enqueue(parser.queue!!, parser.dedupHandler!!, refer, *requests)

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
    private fun enqueue(
        queue: RequestQueue,
        dedupHandler: DedupHandler,
        refer: String,
        vararg requests: Request
    ): Boolean {
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
                if (!config.http.robotsHandler.handle(req)) {
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
     * Abort the Spider because of error, wait tasks in queue finish.
     */
    override fun abort() {
        aborted = true
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
            crawl(requests[0].url.path(), *requests.toTypedArray())

            crawler.context = this@Spider
            repeat(config.crawler.concurrency) {
                val job = GlobalScope.plus(coroutineContext).launch {
                    crawl(this)
                }
                jobs += job
            }
        } else {
            follow(requests[0].url.path(), *requests.toTypedArray())
        }

        parser.context = this@Spider
        coroutineScope {
            repeat(config.parser.concurrency) {
                val job = launch(coroutineContext) {
                    parse(this)
                }
                jobs += job
            }
        }
        jobs.forEach { it.join() }

        postHandle()
        when (status) {
            Status.CANCELLED -> listeners.forEach { it.onCancel() }
            Status.ABORTED -> listeners.forEach { it.onCancel() }
            else -> listeners.forEach { it.onComplete() }
        }
        listeners.forEach { it.onShutdown() }
        logger.info("Spider $name is $status")
    }

    open fun validate() {
        if (status == Status.STARTED || status == Status.RUNNING) {
            throw IllegalStateException("Spider $name is " + status.name.toLowerCase().capitalize())
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
        if (config.parser.concurrency == 0) {
            config.parser.concurrency = if (crawler != null) {
                Runtime.getRuntime().availableProcessors()
            } else {
                1
            }
        }
        coroutineContext = newFixedThreadPoolContext(config.parser.concurrency, "Spider-" + id.getAndIncrement())
        if (resultHandlers.isEmpty()) {
            resultHandlers += ConsoleLogResultHandler
        }
        statisticListener.spider = this@Spider
        listeners += statisticListener
        crawler?.let {
            it.queue = it.queue ?: InMemoryRequestQueue()
            it.dedupHandler = it.dedupHandler ?: HashSetDedupHandler()
            if (it.queue is Listener) listeners += it.queue as Listener
            if (it.dedupHandler is Listener) listeners += it.dedupHandler as Listener
        }
        with(parser) {
            queue = queue ?: InMemoryRequestQueue()
            dedupHandler = dedupHandler ?: HashSetDedupHandler()
            if (queue is Listener) listeners += queue as Listener
            if (dedupHandler is Listener) listeners += dedupHandler as Listener
        }
        with(config.http.robotsHandler) {
            if (this is Listener) listeners += this as Listener
            init(requests)
        }
        config.authHandler?.let {
            preHandlers += config.authHandler!!
            if (config.authHandler is Listener) listeners += config.authHandler as Listener
        }
        with(config.http) {
            userAgentProvider = userAgentProvider ?: RoundRobinUserAgentProvider(userAgents)
            proxyProvider = proxyProvider ?: RoundRobinProxyProvider(proxies)
        }
        listeners.sortBy { it.order }
        initHttpClient()
    }

    open fun initHttpClient() {
        crawler?.let {
            it.httpClient = it.httpClient ?: httpClient
            val client = it.httpClient!!
            client.userAgentProvider = config.http.userAgentProvider!!
            client.proxyProvider = config.http.proxyProvider!!
            client.charset = config.http.charset
        }

        parser.httpClient = parser.httpClient ?: httpClient
        val client = parser.httpClient!!
        client.userAgentProvider = config.http.userAgentProvider!!
        client.proxyProvider = config.http.proxyProvider!!
        client.charset = config.http.charset
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
            status = if (aborted) Status.ABORTED else Status.COMPLETED
        }
    }

    open fun setHeaders(request: Request, referer: String?) {
        if (referer != null && !request.headers.containsKey("Referer")) {
            request.headers["Referer"] = listOf(referer)
        }
        for (entry in config.http.headers) {
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
                    count.incrementAndGet()
                    setHeaders(request, referer)
                    crawler.httpClient!!.dispatch(request) { result ->
                        result.onSuccess { response ->
                            listeners.forEach { it.onDownloadSuccess(request, response) }
                            try {
                                crawler.handle(request, response)
                                listeners.forEach { it.onCrawlSuccess(request, response) }
                            } catch (e: Exception) {
                                listeners.forEach { it.onCrawlFailed(request, e) }
                                listeners.forEach { it.onError(e) }
                                if (config.crawler.abortOnError) abort()
                                logger.warn("Crawl page {} failed", request.url, e)
                            }
                            count.decrementAndGet()
                        }
                        result.onFailure { e ->
                            listeners.forEach { it.onDownloadFailed(request, e) }
                            listeners.forEach { it.onError(e) }
                            if (config.crawler.abortOnError) abort()
                            logger.warn("Download page {} failed", request.url, e)
                            count.decrementAndGet()
                        }
                    }
                    referer = request.url

                    delay(config.interval)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    if (config.crawler.abortOnError) abort()
                    logger.warn("Crawl page {} failed", request.url, e)
                }
            }

            if (stoped || (finished && crawler.queue!!.isEmpty() && parser.queue!!.isEmpty() && count.get() == 0)) {
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
                    count.incrementAndGet()
                    setHeaders(request, referer)
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
                                        if (config.parser.abortOnError) abort()
                                        logger.warn("Handle result failed", e)
                                    }
                                }
                            } catch (e: Throwable) {
                                listeners.forEach { it.onError(e) }
                                if (config.parser.abortOnError) abort()
                                logger.warn("Parse page {} failed", request.url, e)
                            }
                            count.decrementAndGet()
                        }
                        res.onFailure { e ->
                            listeners.forEach { it.onDownloadFailed(request, e) }
                            listeners.forEach { it.onError(e) }
                            if (config.parser.abortOnError) abort()
                            logger.warn("Download page {} failed", request.url, e)
                            count.decrementAndGet()
                        }
                    }
                    referer = request.url

                    delay(config.interval)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    if (config.parser.abortOnError) abort()
                    logger.warn("Handle page {} failed", request.url, e)
                }
            }

            if (stoped || (finished && (crawler?.queue?.isEmpty() != false) && parser.queue!!.isEmpty() && count.get() == 0)) {
                break
            }
        }
    }
}

class SimpleSpider<T>(vararg url: String, parse: (request: Request, response: Response) -> T) :
    Spider<T>(SimpleParser(parse), *url)

enum class Status {
    IDLE,
    STARTED,
    RUNNING,
    ABORTED,
    CANCELLED,
    COMPLETED;

    override fun toString(): String {
        return name.toLowerCase().capitalize()
    }
}
