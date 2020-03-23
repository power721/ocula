package com.har01d.ocula.handler

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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

interface DedupHandler {
    fun handle(request: Request): Boolean
}

object DefaultDedupHandler : DedupHandler {
    override fun handle(request: Request) = true
}

class HashSetDedupHandler : DedupHandler {
    val set: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    override fun handle(request: Request) = set.add(request.url)
}
