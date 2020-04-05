package com.har01d.ocula.handler

import com.har01d.ocula.Context
import com.har01d.ocula.http.Request
import java.util.*
import java.util.concurrent.ConcurrentHashMap

typealias Parameters = List<Pair<String, Any?>>

interface PreHandler {
    var context: Context
    fun handle(request: Request)
}

abstract class AbstractPreHandler : PreHandler {
    override lateinit var context: Context
}

interface PostHandler {
    var context: Context
    fun handle(request: Request)
}

abstract class AbstractPostHandler : PostHandler {
    override lateinit var context: Context
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

interface RobotsHandler {
    fun init(requests: List<Request>)
    fun handle(request: Request): Boolean
}

object NoopRobotsHandler : RobotsHandler {
    override fun init(requests: List<Request>) {}
    override fun handle(request: Request) = true
}
