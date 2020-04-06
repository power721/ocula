package com.har01d.ocula.listener

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Listener {
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
    fun onComplete()
    fun onFinish()
}

abstract class AbstractListener : Listener {
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
    override fun onComplete() {}
    override fun onFinish() {}
}

abstract class StatisticListener : AbstractListener() {
    lateinit var spider: Spider<*>
}

class DefaultStatisticListener : StatisticListener() {
    private val logger: Logger = LoggerFactory.getLogger(DefaultStatisticListener::class.java)
    private lateinit var job: Job
    private var skipped = 0
    private var downloaded = 0
    private var crawled = 0
    private var parsed = 0
    private var errors = 0
    private var startTime: Long = 0

    override fun onStart() {
        startTime = System.currentTimeMillis()
        job = GlobalScope.launch {
            while (isActive) {
                delay(30000)
                log()
            }
        }
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
        job.cancel()
        log()
    }

    override fun onComplete() {
        job.cancel()
        log(true)
    }

    private fun log(finished: Boolean = false) {
        val time = (System.currentTimeMillis() - startTime) / 1000
        val size1 = spider.crawler?.queue?.size()
        val size2 = spider.parser.queue!!.size()
        val queue = if (!finished) {
            if (size1 != null) " Queue: $size1-$size2 " else " Queue: $size2 "
        } else {
            ""
        }
        val name = spider.name
        logger.info("$name: Downloaded pages: $downloaded  Crawled pages: $crawled  Parsed pages: $parsed  " +
                "Skipped pages: $skipped $queue Errors: $errors  Time: ${time}s")
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
