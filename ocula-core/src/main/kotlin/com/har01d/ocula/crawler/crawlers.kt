package com.har01d.ocula.crawler

import com.har01d.ocula.Context
import com.har01d.ocula.handler.DedupHandler
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.queue.RequestQueue

interface Crawler {
    var context: Context
    var httpClient: HttpClient?
    var queue: RequestQueue?
    var dedupHandler: DedupHandler?
    fun handle(request: Request, response: Response)
}

abstract class AbstractCrawler : Crawler {
    override lateinit var context: Context
    override var httpClient: HttpClient? = null
    override var queue: RequestQueue? = null
    override var dedupHandler: DedupHandler? = null

    fun crawl(response: Response, vararg urls: String): Boolean {
        val result = context.crawl(response.url, *urls)
        if (!result) {
            context.finish()
        }
        return result
    }

    fun crawl(response: Response, vararg requests: Request): Boolean {
        val result = context.crawl(response.url, *requests)
        if (!result) {
            context.finish()
        }
        return result
    }

    fun follow(response: Response, links: List<String>) = context.follow(response.url, *links.toTypedArray())
    fun follow(response: Response, vararg href: String) = context.follow(response.url, *href)
    fun follow(response: Response, vararg request: Request) = context.follow(response.url, *request)
    fun finish() = context.finish()
}
