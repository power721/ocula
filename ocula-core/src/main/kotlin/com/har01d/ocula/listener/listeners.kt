package com.har01d.ocula.listener

import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Listener<in T> {
    fun onStart()
    fun onDownloadSuccess(request: Request, response: Response)
    fun onDownloadFailed(request: Request, e: Exception)
    fun onCrawlSuccess(request: Request, response: Response)
    fun onCrawlFailed(request: Request, e: Exception)
    fun onParseSuccess(request: Request, response: Response, result: T)
    fun onParseFailed(request: Request, response: Response, e: Exception)
    fun onError(e: Exception)
    fun onFinish()
}

abstract class AbstractListener<T> : Listener<T> {
    override fun onStart() {}

    override fun onDownloadSuccess(request: Request, response: Response) {}

    override fun onDownloadFailed(request: Request, e: Exception) {}

    override fun onCrawlSuccess(request: Request, response: Response) {}

    override fun onCrawlFailed(request: Request, e: Exception) {}

    override fun onParseSuccess(request: Request, response: Response, result: T) {}

    override fun onParseFailed(request: Request, response: Response, e: Exception) {}

    override fun onError(e: Exception) {}

    override fun onFinish() {}
}

object StatisticListener : AbstractListener<Any?>() {
    private val logger: Logger = LoggerFactory.getLogger(StatisticListener::class.java)
    private var downloaded = 0
    private var crawled = 0
    private var parsed = 0
    private var errors = 0
    private var startTime: Long = 0

    override fun onStart() {
        startTime = System.currentTimeMillis()
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

    override fun onError(e: Exception) {
        errors++
    }

    override fun onFinish() {
        val time = (System.currentTimeMillis() - startTime) / 1000
        logger.info("Downloaded pages: $downloaded  Crawled pages: $crawled  Parsed pages: $parsed  Errors: $errors  Time: ${time}s")
    }
}

object LogListener : Listener<Any?> {
    private val logger: Logger = LoggerFactory.getLogger(LogListener::class.java)

    override fun onStart() {
        logger.info("Spider start")
    }

    override fun onDownloadSuccess(request: Request, response: Response) {
        logger.info("Download ${request.url} success")
    }

    override fun onDownloadFailed(request: Request, e: Exception) {
        logger.warn("Download ${request.url} failed")
    }

    override fun onCrawlSuccess(request: Request, response: Response) {
        logger.info("Crawl ${request.url} success")
    }

    override fun onCrawlFailed(request: Request, e: Exception) {
        logger.warn("Crawl ${request.url} failed")
    }

    override fun onParseSuccess(request: Request, response: Response, result: Any?) {
        logger.info("Parse ${request.url} success")
    }

    override fun onParseFailed(request: Request, response: Response, e: Exception) {
        logger.warn("Parse ${request.url} failed")
    }

    override fun onError(e: Exception) {
        logger.warn("Error", e)
    }

    override fun onFinish() {
        logger.info("Spider finished")
    }
}
