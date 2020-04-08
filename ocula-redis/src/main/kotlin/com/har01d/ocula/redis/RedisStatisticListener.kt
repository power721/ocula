package com.har01d.ocula.redis

import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.listener.StatisticListener
import com.har01d.ocula.util.toDuration
import kotlinx.coroutines.*
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RedisStatisticListener(name: String, connection: String = "redis://127.0.0.1:6379") : StatisticListener() {
    private val logger: Logger = LoggerFactory.getLogger(RedisStatisticListener::class.java)
    private lateinit var redisson: RedissonClient
    private val map by lazy { redisson.getMap<String, Int>(name) }
    private lateinit var job: Job
    private var startTime: Long = 0

    constructor(name: String, redisson: RedissonClient) : this(name, "") {
        this.redisson = redisson
    }

    init {
        if (connection.isNotEmpty()) {
            val config = Config()
            config.codec = JsonJacksonCodec()
            config.useSingleServer().address = connection
            redisson = Redisson.create(config)
        }
    }

    override fun onStart() {
        startTime = System.currentTimeMillis()
        map.addAndGetAsync("id", 1)
        map["startTime"] = startTime.toInt()
        job = GlobalScope.launch {
            while (isActive) {
                delay(30000)
                log()
            }
        }
    }

    override fun onSkip(request: Request) {
        map.addAndGetAsync("skipped", 1)
    }

    override fun onDownloadSuccess(request: Request, response: Response) {
        map.addAndGetAsync("downloaded", 1)
    }

    override fun onCrawlSuccess(request: Request, response: Response) {
        map.addAndGetAsync("crawled", 1)
    }

    override fun <T> onParseSuccess(request: Request, response: Response, result: T) {
        map.addAndGetAsync("parsed", 1)
    }

    override fun onError(e: Throwable) {
        map.addAndGetAsync("errors", 1)
    }

    override fun onCancel() {
        log()
    }

    override fun onAbort() {
        log()
    }

    override fun onComplete() {
        log(true)
    }

    override fun onShutdown() {
        job.cancel()
        val endTime = System.currentTimeMillis()
        map["endTime"] = endTime.toInt()
        map.addAndGetAsync("elapsed", (endTime - startTime))
    }

    private fun log(finished: Boolean = false) {
        val time = (System.currentTimeMillis() - startTime).toDuration()
        val size1 = spider.crawler?.queue?.size()
        val size2 = spider.parser.queue!!.size()
        val queue = if (!finished) {
            if (size1 != null) " Queue: $size1-$size2 " else " Queue: $size2 "
        } else {
            ""
        }
        val name = spider.name
        val skipped = map.getOrDefault("skipped", 0)
        val downloaded = map.getOrDefault("downloaded", 0)
        val crawled = map.getOrDefault("crawled", 0)
        val parsed = map.getOrDefault("parsed", 0)
        val errors = map.getOrDefault("errors", 0)
        logger.info(
            "$name: Downloaded pages: $downloaded  Crawled pages: $crawled  Parsed pages: $parsed  " +
                    "Skipped pages: $skipped $queue Errors: $errors  Time: $time"
        )
    }
}
