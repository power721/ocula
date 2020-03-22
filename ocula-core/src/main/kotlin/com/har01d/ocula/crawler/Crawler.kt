package com.har01d.ocula.crawler

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Crawler {
    var spider: Spider<*>
    fun handle(request: Request, response: Response): List<Request>
}
