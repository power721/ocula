package com.har01d.ocula.parser

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Parser<out T> {
    var spider: Spider<*>
    fun parse(request: Request, response: Response): T
}
