package com.har01d.ocula.crawler

import com.har01d.ocula.Spider
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Crawler {
    var spider: Spider<*>
    var httpClient: HttpClient?
    val candidates: List<Request>
    fun handle(request: Request, response: Response)
}

abstract class AbstractCrawler : Crawler {
    override lateinit var spider: Spider<*>
    override var httpClient: HttpClient? = null
    override val candidates: MutableList<Request> = mutableListOf()

    fun follow(response: Response, href: String) {
        spider.follow(response.url, href)
    }

    fun follow(response: Response, request: Request) {
        spider.follow(response.url, request)
    }

    fun crawl(next: String) {
        candidates += Request(next)
    }

    fun crawl(next: Request) {
        candidates += next
    }
}
