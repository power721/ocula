package cn.har01d.ocula.handler

import cn.har01d.ocula.Context
import cn.har01d.ocula.http.Request
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

/**
 * If this is persisted, the request queue have to persist too.
 */
interface DedupHandler {
    fun shouldVisit(request: Request): Boolean
    fun reset()
}

object DefaultDedupHandler : DedupHandler {
    override fun shouldVisit(request: Request) = true
    override fun reset() {}
}

class HashSetDedupHandler : DedupHandler {
    val set: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    override fun shouldVisit(request: Request) = set.add(request.url)
    override fun reset() = set.clear()
}

interface RobotsHandler {
    fun init(requests: List<Request>)
    fun handle(request: Request): Boolean
}

object NoopRobotsHandler : RobotsHandler {
    override fun init(requests: List<Request>) {}
    override fun handle(request: Request) = true
}
