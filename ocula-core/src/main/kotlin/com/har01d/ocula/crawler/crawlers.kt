package com.har01d.ocula.crawler

import com.har01d.ocula.Context
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Crawler {
    var context: Context
    var httpClient: HttpClient?
    val candidates: List<Request>
    fun handle(request: Request, response: Response)
}

abstract class AbstractCrawler : Crawler {
    override lateinit var context: Context
    override var httpClient: HttpClient? = null
    override val candidates: MutableList<Request> = mutableListOf()

    fun follow(response: Response, href: String) {
        context.follow(response.url, href)
    }

    fun follow(response: Response, request: Request) {
        context.follow(response.url, request)
    }

    fun crawl(next: String) {
        candidates += Request(next)
    }

    fun crawl(next: Request) {
        candidates += next
    }

    fun finish() {
        context.finish()
    }
}
