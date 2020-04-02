package com.har01d.ocula.listener

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface Listener<in T> {
    fun onStart()
    fun onSkip(request: Request)
    fun onDownloadSuccess(request: Request, response: Response)
    fun onDownloadFailed(request: Request, e: Throwable)
    fun onCrawlSuccess(request: Request, response: Response)
    fun onCrawlFailed(request: Request, e: Throwable)
    fun onParseSuccess(request: Request, response: Response, result: T)
    fun onParseFailed(request: Request, response: Response, e: Throwable)
    fun onError(e: Throwable)
    fun onFinish()
}

abstract class AbstractListener<T> : Listener<T> {
    override fun onStart() {}
    override fun onSkip(request: Request) {}
    override fun onDownloadSuccess(request: Request, response: Response) {}
    override fun onDownloadFailed(request: Request, e: Throwable) {}
    override fun onCrawlSuccess(request: Request, response: Response) {}
    override fun onCrawlFailed(request: Request, e: Throwable) {}
    override fun onParseSuccess(request: Request, response: Response, result: T) {}
    override fun onParseFailed(request: Request, response: Response, e: Throwable) {}
    override fun onError(e: Throwable) {}
    override fun onFinish() {}
}

class StatisticListener : AbstractListener<Any?>() {
    lateinit var spider: Spider<*>
    private val logger: Logger = LoggerFactory.getLogger(StatisticListener::class.java)
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var skipped = 0
    private var downloaded = 0
    private var crawled = 0
    private var parsed = 0
    private var errors = 0
    private var startTime: Long = 0

    override fun onStart() {
        startTime = System.currentTimeMillis()
        executor.scheduleAtFixedRate({ log() }, 30, 30, TimeUnit.SECONDS)
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

    override fun onParseSuccess(request: Request, response: Response, result: Any?) {
        parsed++
    }

    override fun onError(e: Throwable) {
        errors++
    }

    override fun onFinish() {
        executor.shutdown()
        log(true)
    }

    private fun log(finished: Boolean = false) {
        val time = (System.currentTimeMillis() - startTime) / 1000
        val size1 = spider.queueCrawler.size()
        val size2 = spider.queueParser.size()
        val queue = if (!finished) {
            if (spider.crawler != null) " Queue: $size1-$size2 " else " Queue: $size2 "
        } else {
            ""
        }
        val name = spider.getName()
        logger.info("$name: Downloaded pages: $downloaded  Crawled pages: $crawled  Parsed pages: $parsed  " +
                "Skipped pages: $skipped $queue Errors: $errors  Time: ${time}s")
    }
}

object LogListener : AbstractListener<Any?>() {
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

    override fun onParseSuccess(request: Request, response: Response, result: Any?) {
        logger.info("Parse ${request.url} success")
    }

    override fun onParseFailed(request: Request, response: Response, e: Throwable) {
        logger.warn("Parse ${request.url} failed")
    }

    override fun onError(e: Throwable) {
        logger.warn("Error", e)
    }
}
