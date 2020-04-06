package com.har01d.ocula.redis

import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.listener.DefaultStatisticListener
import com.har01d.ocula.listener.StatisticListener
import kotlinx.coroutines.*
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RedisStatisticListener(name: String, connection: String = "redis://127.0.0.1:6379") : StatisticListener() {
    private lateinit var redisson: RedissonClient
    private val map by lazy { redisson.getMap<String, Int>(name) }

    private val logger: Logger = LoggerFactory.getLogger(DefaultStatisticListener::class.java)
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
        job = GlobalScope.launch {
            while (isActive) {
                delay(30000)
                log()
            }
        }
        map["startTime"] = startTime.toInt()
    }

    override fun onSkip(request: Request) {
        map.addAndGet("skipped", 1)
    }

    override fun onDownloadSuccess(request: Request, response: Response) {
        map.addAndGet("downloaded", 1)
    }

    override fun onCrawlSuccess(request: Request, response: Response) {
        map.addAndGet("crawled", 1)
    }

    override fun <T> onParseSuccess(request: Request, response: Response, result: T) {
        map.addAndGet("parsed", 1)
    }

    override fun onError(e: Throwable) {
        map.addAndGet("errors", 1)
    }

    override fun onCancel() {
        job.cancel()
        log()
    }

    override fun onComplete() {
        job.cancel()
        log(true)
    }

    override fun onFinish() {
        redisson.shutdown()
    }

    private fun log(finished: Boolean = false) {
        val endTime = System.currentTimeMillis()
        map["endTime"] = endTime.toInt()
        val time = (endTime - startTime) / 1000
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
        logger.info("$name: Downloaded pages: $downloaded  Crawled pages: $crawled  Parsed pages: $parsed  " +
                "Skipped pages: $skipped $queue Errors: $errors  Time: ${time}s")
    }
}
