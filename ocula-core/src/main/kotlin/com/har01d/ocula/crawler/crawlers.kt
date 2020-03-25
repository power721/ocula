package com.har01d.ocula.crawler

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Crawler {
    var spider: Spider<*>
    fun handle(request: Request, response: Response)
}

abstract class AbstractCrawler : Crawler {
    override lateinit var spider: Spider<*>

    fun follow(response: Response, href: String) {
        spider.follow(response.url, href)
    }

    fun follow(response: Response, request: Request) {
        spider.follow(response.url, request)
    }

    fun crawl(response: Response, next: String) {
        if (!spider.crawl(response.url, next)) {
            spider.finish()
        }
    }

    fun crawl(response: Response, next: Request) {
        if (!spider.crawl(response.url, next)) {
            spider.finish()
        }
    }
}
