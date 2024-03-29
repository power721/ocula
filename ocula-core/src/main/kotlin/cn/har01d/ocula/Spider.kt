package cn.har01d.ocula

import cn.har01d.ocula.crawler.Crawler
import cn.har01d.ocula.handler.*
import cn.har01d.ocula.http.*
import cn.har01d.ocula.listener.DefaultStatisticListener
import cn.har01d.ocula.listener.Listener
import cn.har01d.ocula.listener.StatisticListener
import cn.har01d.ocula.parser.Parser
import cn.har01d.ocula.parser.SimpleParser
import cn.har01d.ocula.queue.InMemoryRequestQueue
import cn.har01d.ocula.queue.RequestQueue
import cn.har01d.ocula.util.normalizeUrl
import cn.har01d.ocula.util.path
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


typealias Configure<T> = Spider<T>.() -> Unit

open class Spider<T>(val crawler: Crawler? = null, val parser: Parser<T>, configure: Configure<T> = {}) : Context {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val executor: ExecutorService = Executors.newFixedThreadPool(2, SpiderThreadFactory("Spider"))
    }

    override lateinit var name: String
    override val config = Config()
    val preHandlers = mutableListOf<PreHandler>()
    val postHandlers = mutableListOf<PostHandler>()
    val resultHandlers = mutableListOf<ResultHandler<T>>()
    val listeners = mutableListOf<Listener>()
    var statisticListener: StatisticListener? = null
    var httpClient: HttpClient = ApacheHttpClient()
    private val requests = mutableListOf<Request>()

    var future: Future<*>? = null
    var status: Status = Status.IDLE
        private set
    private var finished = false
    private var cancelled = false
    private var aborted = false
    private var stoped = false
    private val count = AtomicInteger()
    private val activeTime = AtomicLong()

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
        logger.debug("spider configure")
        with(config) {
            block()
        }
    }

    fun mobile() {
        logger.debug("spider uses mobile agents")
        config.http.mobile()
    }

    fun basicAuth(username: String, password: String) {
        logger.debug("basic auth, username: {}", username)
        config.authHandler = BasicAuthHandler(username, password)
    }

    fun cookieAuth(name: String, value: String) {
        logger.debug("cookie auth, name: {}", name)
        config.authHandler = CookieAuthHandler(name, value)
    }

    fun tokenAuth(token: String, header: String = "Authorization") {
        logger.debug("token auth, header: {}", header)
        config.authHandler = TokenAuthHandler(token, header)
    }

    fun formAuth(actionUrl: String, body: FormRequestBody, block: AuthConfigure = { _, _ -> }) {
        logger.debug("form auth, actionUrl: {}", actionUrl)
        config.authHandler = FormAuthHandler(actionUrl, body, block)
    }

    fun httpProxy(hostname: String, port: Int) {
        logger.debug("add http proxy {}:{}", hostname, port)
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
        return enqueue(queue, dedupHandler, refer, *urls.map { Request(it, config.http.httpMethod) }.toTypedArray())
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
                logger.debug("skip invalid url {}", url)
                continue
            }
            val uri = normalizeUrl(refer, url)
            if (uri != null) {
                request.headers["Referer"] = listOf(refer.path())
                val req = request.copy(url = uri)
                if (!config.http.robotsHandler.handle(req)) {
                    listeners.forEach { it.onSkip(req) }
                    logger.trace("[RobotsHandler] Skip {}", req.url)
                    continue
                }
                // BUG: 如果是list页面没有采集完成被中断，重启后不能再采集list页面剩余的内容
                if (!dedupHandler.shouldVisit(req)) {
                    logger.trace("[DedupHandler] Ignore {}", req.url)
                    continue
                }
                queue.push(req)
                logger.debug("Enqueue {}", req.url)
                success = true
            } else {
                logger.debug("skip malformed url {}", url)
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
     * Dispatch HTTP request by HttpClient.
     */
    override fun dispatch(request: Request) = httpClient.dispatch(request)

    override fun dispatch(url: String) = httpClient.dispatch(Request(url))

    override fun reset() {
        logger.debug("[DedupHandler] reset")
        crawler?.dedupHandler?.reset()
    }

    /**
     * Indicate no more new tasks, wait tasks in queue finish.
     */
    override fun finish() {
        logger.trace("spider finish")
        finished = true
    }

    /**
     * Abort the Spider because of error, wait tasks in queue finish.
     */
    override fun abort(stop: Boolean) {
        logger.debug("spider abort {}", stop)
        stoped = stop
        aborted = true
        finished = true
    }

    /**
     * Stop the Spider, ignore the tasks in queue.
     */
    override fun stop(): Boolean {
        stoped = true
        future?.cancel(true)
        if (status == Status.STARTED || status == Status.RUNNING) {
            cancelled = true
            logger.debug("stop spider {}", name)
            return true
        }
        logger.warn("stop spider {} failed, status: {}", name, status)
        return false
    }

    /**
     * Start the Spider in new thread.
     */
    open fun start(): Boolean {
        if (status != Status.STARTED && status != Status.RUNNING) {
            future = executor.submit {
                run()
            }
            logger.debug("start spider {}", name)
            return true
        }
        logger.warn("start spider {} failed, status: {}", name, status)
        return false
    }

    fun run() {
        logger.debug("spider run")
        validate()
        prepare()

        logger.info("Spider $name Started")
        listeners.forEach { it.onStart() }
        preHandle()

        val futures = mutableListOf<Future<*>>()

        var crawlerExecutor: ExecutorService? = null
        if (crawler != null) {
            logger.debug("start crawler")
            crawl(requests[0].url.path(), *requests.toTypedArray())

            crawler.context = this@Spider
            crawlerExecutor = Executors.newFixedThreadPool(config.crawler.concurrency, SpiderThreadFactory("Crawler"))
            repeat(config.crawler.concurrency) {
                futures += crawlerExecutor.submit {
                    try {
                        crawl()
                    } catch (e: Exception) {
                        abort(true)
                        logger.warn("crawl failed", e)
                    }
                }
            }
        } else {
            follow(requests[0].url.path(), *requests.toTypedArray())
        }

        parser.context = this@Spider
        val parserExecutor = Executors.newFixedThreadPool(config.parser.concurrency, SpiderThreadFactory("Parser"))
        repeat(config.parser.concurrency) {
            futures += parserExecutor.submit {
                try {
                    parse()
                } catch (e: Exception) {
                    abort(true)
                    logger.warn("parse failed", e)
                }
            }
        }

        futures.forEach {
            try {
                it.get()
            } catch (e: InterruptedException) {
                stoped = true
            } catch (e: Exception) {
                abort(true)
                logger.warn("execute failed", e)
            }
        }

        parserExecutor.shutdown()
        crawlerExecutor?.shutdown()
        postHandle()

        listeners.forEach { it.onShutdown() }
        logger.info("Spider $name is $status")
    }

    open fun validate() {
        logger.debug("spider validate")
        if (status == Status.STARTED || status == Status.RUNNING) {
            throw IllegalStateException("Spider $name is " + status.name.toLowerCase().capitalize())
        }
        if (requests.isEmpty()) {
            throw IllegalStateException("start url is required")
        }
    }

    open fun prepare() {
        logger.debug("spider prepare")
        resetStatus()
        initName()
        smartConcurrency()
        configCrawler()
        configParser()
        configRobots()
        configAuth()
        configHttp()
        initHttpClient()
        initStatisticListener()
        listeners.sortBy { it.order }
        if (resultHandlers.isEmpty()) resultHandlers += ConsoleLogResultHandler
        activeTime.set(System.currentTimeMillis())
        logger.debug("spider prepare done")
    }

    private fun resetStatus() {
        logger.debug("spider reset status")
        stoped = false
        aborted = false
        finished = false
        cancelled = false
        status = Status.STARTED
    }

    private fun initName() {
        if (!this::name.isInitialized) {
            name = URL(requests[0].url).host
            logger.debug("init name {}", name)
        }
    }

    private fun smartConcurrency() {
        if (config.parser.concurrency == 0) {
            config.parser.concurrency = if (crawler != null) {
                Runtime.getRuntime().availableProcessors()
            } else {
                1
            }
            logger.debug("smart concurrency: {}", config.parser.concurrency)
        }
    }

    private fun configCrawler() {
        crawler?.let {
            logger.debug("config crawler")
            it.queue = it.queue ?: InMemoryRequestQueue()
            it.dedupHandler = it.dedupHandler ?: HashSetDedupHandler()
            if (it.queue is Listener) addListener(it.queue as Listener)
            if (it.dedupHandler is Listener) addListener(it.dedupHandler as Listener)
        }
    }

    private fun configParser() {
        with(parser) {
            logger.debug("config parser")
            queue = queue ?: InMemoryRequestQueue()
            dedupHandler = dedupHandler ?: HashSetDedupHandler()
            if (queue is Listener) addListener(queue as Listener)
            if (dedupHandler is Listener) addListener(dedupHandler as Listener)
        }
    }

    private fun configRobots() {
        with(config.http.robotsHandler) {
            logger.debug("config Robots")
            if (this is Listener) addListener(this as Listener)
            init(requests)
        }
    }

    private fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) {
            logger.debug("add listener {}", listener)
            listeners += listener
        } else {
            logger.debug("listener {} exists", listener)
        }
    }

    private fun configAuth() {
        config.authHandler?.let {
            logger.debug("config auth")
            preHandlers += config.authHandler!!
            if (config.authHandler is Listener) addListener(config.authHandler as Listener)
        }
    }

    private fun configHttp() {
        with(config.http) {
            logger.debug("config HTTP")
            userAgentProvider = userAgentProvider ?: RoundRobinUserAgentProvider(userAgents)
            proxyProvider = proxyProvider ?: RoundRobinProxyProvider(proxies)
        }
    }

    open fun initHttpClient() {
        logger.debug("init HTTP client")
        crawler?.let {
            it.httpClient = it.httpClient ?: httpClient
            configHttpClient(it.httpClient!!)
        }

        parser.httpClient = parser.httpClient ?: httpClient
        configHttpClient(parser.httpClient!!)
    }

    private fun configHttpClient(client: HttpClient) {
        logger.debug("config HTTP client")
        if (client.userAgentProvider is EmptyUserAgentProvider)
            client.userAgentProvider = config.http.userAgentProvider!!
        if (client.proxyProvider is EmptyProxyProvider)
            client.proxyProvider = config.http.proxyProvider!!
        client.charset = config.http.charset
        client.timeout = config.http.timeout
        client.timeoutRead = config.http.timeoutRead
    }

    private fun initStatisticListener() {
        logger.debug("init statistic listener")
        if (statisticListener == null) {
            statisticListener = DefaultStatisticListener()
        }
        statisticListener?.spider = this@Spider
        addListener(statisticListener!!)
    }

    open fun preHandle() {
        logger.debug("spider pre handle")
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
        logger.debug("spider pre handle done")
    }

    open fun postHandle() {
        logger.debug("spider post handle")
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
        future = null
        crawler?.httpClient?.close()
        parser.httpClient?.close()
        if (status == Status.RUNNING) {
            status = if (cancelled) Status.CANCELLED else if (aborted) Status.ABORTED else Status.COMPLETED
        }
        when (status) {
            Status.ABORTED -> listeners.forEach { it.onAbort() }
            Status.CANCELLED -> listeners.forEach { it.onCancel() }
            Status.COMPLETED -> listeners.forEach { it.onComplete() }
        }
        logger.debug("spider post handle done")
    }

    open fun setHeaders(request: Request, referer: String?) {
        if (referer != null && !request.headers.containsKey("Referer")) {
            logger.trace("set HTTP header Referer={}", referer)
            request.headers["Referer"] = listOf(referer)
        }
        for (entry in config.http.headers) {
            if (!request.headers.containsKey(entry.key)) {
                logger.trace("set HTTP header {}={}", entry.key, entry.value)
                request.headers[entry.key] = entry.value
            }
        }
    }

    private fun crawl() {
        var referer: String? = null
        status = Status.RUNNING
        while (true) {
            val request = crawler!!.queue!!.poll(1000L)
            if (request != null) {
                logger.trace("crawler handle request {}", request)
                try {
                    activeTime.set(System.currentTimeMillis())
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

                    Thread.sleep(config.interval)
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

    private fun parse() {
        var referer: String? = null
        status = Status.RUNNING
        while (true) {
            val request = parser.queue!!.poll(1000L)
            if (request != null) {
                logger.trace("parser handle request {}", request)
                try {
                    activeTime.set(System.currentTimeMillis())
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

                    Thread.sleep(config.interval)
                } catch (e: Exception) {
                    listeners.forEach { it.onError(e) }
                    if (config.parser.abortOnError) abort()
                    logger.warn("Handle page {} failed", request.url, e)
                }
            }

            if (config.completeOnIdleTime > 0 && (System.currentTimeMillis() - activeTime.get()) >= config.completeOnIdleTime * 1000) {
                if (!finished) logger.info("No work for ${config.completeOnIdleTime} seconds, complete Spider $name.")
                finished = true
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

fun <T> spider(crawler: Crawler, parser: Parser<T>, vararg url: String, configure: Configure<T> = {}) {
    Spider(crawler = crawler, parser = parser, url = *url, configure = configure).run()
}

fun <T> spider(parser: Parser<T>, vararg url: String, configure: Configure<T> = {}) {
    Spider(parser = parser, url = *url, configure = configure).run()
}

fun <T> spider(vararg url: String, parse: (request: Request, response: Response) -> T) {
    SimpleSpider(url = *url, parse = parse).run()
}
