package cn.har01d.ocula.listener

import cn.har01d.ocula.Spider
import cn.har01d.ocula.SpiderThreadFactory
import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.util.toDuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

interface Listener {
    val order: Int
    fun onStart()
    fun onSkip(request: Request)
    fun onDownloadSuccess(request: Request, response: Response)
    fun onDownloadFailed(request: Request, e: Throwable)
    fun onCrawlSuccess(request: Request, response: Response)
    fun onCrawlFailed(request: Request, e: Throwable)
    fun <T> onParseSuccess(request: Request, response: Response, result: T)
    fun onParseFailed(request: Request, response: Response, e: Throwable)
    fun onError(e: Throwable)
    fun onCancel()
    fun onAbort()
    fun onComplete()
    fun onShutdown()
}

abstract class AbstractListener : Listener {
    override val order: Int = 100
    override fun onStart() {}
    override fun onSkip(request: Request) {}
    override fun onDownloadSuccess(request: Request, response: Response) {}
    override fun onDownloadFailed(request: Request, e: Throwable) {}
    override fun onCrawlSuccess(request: Request, response: Response) {}
    override fun onCrawlFailed(request: Request, e: Throwable) {}
    override fun <T> onParseSuccess(request: Request, response: Response, result: T) {}
    override fun onParseFailed(request: Request, response: Response, e: Throwable) {}
    override fun onError(e: Throwable) {}
    override fun onCancel() {}
    override fun onAbort() {}
    override fun onComplete() {}
    override fun onShutdown() {}
}

abstract class StatisticListener : AbstractListener() {
    override val order: Int = 1000
    lateinit var spider: Spider<*>
}

class DefaultStatisticListener : StatisticListener() {
    private val logger: Logger = LoggerFactory.getLogger(DefaultStatisticListener::class.java)
    private lateinit var executor: ScheduledExecutorService
    private var skipped = 0
    private var downloaded = 0
    private var crawled = 0
    private var parsed = 0
    private var errors = 0
    private var startTime: Long = 0

    override fun onStart() {
        startTime = System.currentTimeMillis()
        executor = Executors.newSingleThreadScheduledExecutor(SpiderThreadFactory("Statistic"))
        executor.scheduleWithFixedDelay({ log() }, 30, 30, TimeUnit.SECONDS)
    }

    override fun onSkip(request: Request) {
        skipped++
    }

    override fun onDownloadSuccess(request: Request, response: Response) {
        downloaded++
    }

    override fun onCrawlSuccess(request: Request, response: Response) {
        crawled++
    }

    override fun <T> onParseSuccess(request: Request, response: Response, result: T) {
        parsed++
    }

    override fun onError(e: Throwable) {
        errors++
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
        executor.shutdown()
    }

    private fun log(finished: Boolean = false) {
        val time = (System.currentTimeMillis() - startTime).toDuration()
        val status = spider.status
        val size1 = spider.crawler?.queue?.size()
        val size2 = spider.parser.queue!!.size()
        val queue = if (!finished) {
            if (size1 != null) " Queue: $size1-$size2 " else " Queue: $size2 "
        } else {
            ""
        }
        val name = spider.name
        logger.info(
            "$name($status): Downloaded pages: $downloaded  Crawled pages: $crawled  Parsed pages: $parsed  " +
                    "Skipped pages: $skipped $queue Errors: $errors  Time: $time"
        )
    }
}

object LogListener : AbstractListener() {
    private val logger: Logger = LoggerFactory.getLogger(LogListener::class.java)

    override fun onSkip(request: Request) {
        logger.info("Skip disallowed url {}", request.url)
    }

    override fun onDownloadSuccess(request: Request, response: Response) {
        logger.info("Download ${request.url} success, ${response.time} ms")
    }

    override fun onDownloadFailed(request: Request, e: Throwable) {
        logger.warn("Download ${request.url} failed")
    }

    override fun onCrawlSuccess(request: Request, response: Response) {
        logger.info("Crawl ${request.url} success")
    }

    override fun onCrawlFailed(request: Request, e: Throwable) {
        logger.warn("Crawl ${request.url} failed")
    }

    override fun <T> onParseSuccess(request: Request, response: Response, result: T) {
        logger.info("Parse ${request.url} success")
    }

    override fun onParseFailed(request: Request, response: Response, e: Throwable) {
        logger.warn("Parse ${request.url} failed")
    }

    override fun onError(e: Throwable) {
        logger.warn("Error", e)
    }
}
