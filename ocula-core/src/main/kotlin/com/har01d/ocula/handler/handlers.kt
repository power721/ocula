package com.har01d.ocula.handler

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request

typealias Parameters = List<Pair<String, Any?>>

interface PreHandler {
    var spider: Spider<*>
    fun handle(request: Request)
}

abstract class AbstractPreHandler : PreHandler {
    override lateinit var spider: Spider<*>
}

interface PostHandler {
    var spider: Spider<*>
    fun handle(request: Request)
}

abstract class AbstractPostHandler : PostHandler {
    override lateinit var spider: Spider<*>
}
