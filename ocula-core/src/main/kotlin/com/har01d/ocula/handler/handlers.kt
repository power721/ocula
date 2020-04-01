package com.har01d.ocula.handler

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import java.net.URL
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

interface RobotsHandler {
    fun init(requests: List<Request>)
    fun handle(request: Request): Boolean
}

object NoopRobotsHandler : RobotsHandler {
    override fun init(requests: List<Request>) {}
    override fun handle(request: Request) = true
}

class DefaultRobotsHandler : RobotsHandler {
    private val map = mutableMapOf<String, List<String>>()

    override fun init(requests: List<Request>) {
        requests.map { URL(URL(it.url), "/robots.txt").toExternalForm() }.toSet().forEach {
            map[URL(it).host] = parseRobotsTxt(it)
        }
    }

    private fun parseRobotsTxt(url: String): List<String> {
        val list = mutableListOf<String>()
        val (_, _, res) = url.httpGet().responseString()
        when (res) {
            is Result.Success -> {
                val lines = res.value.lines()
                val index = lines.indexOf("User-agent: *")
                if (index > -1) {
                    for (i in index + 1 until lines.size) {
                        val line = lines[i]
                        if (line.isEmpty() || line.startsWith("User-agent:")) {
                            break
                        }
                        // TODO: Allow:
                        // TODO: wildcard
                        list += line.split(":")[1].trim()
                    }
                }
            }
        }
        return list
    }

    override fun handle(request: Request): Boolean {
        val url = URL(request.url)
        val list = map[url.host]
        if (list != null) {
            return !list.any { request.url.contains(it) }
        }
        return true
    }
}
