package com.har01d.ocula

import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response

interface Parser<out T> {
    var spider: Spider<*>
    fun parse(request: Request, response: Response): T
}

abstract class AbstractParser<T> : Parser<T> {
    override lateinit var spider: Spider<*>
}
