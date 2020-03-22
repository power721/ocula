package com.har01d.ocula

import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Crawler {
    var spider: Spider<*>
    fun handle(request: Request, response: Response): List<Request>
}

abstract class AbstractCrawler : Crawler {
    override lateinit var spider: Spider<*>
}
