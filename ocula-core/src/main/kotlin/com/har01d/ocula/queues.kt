package com.har01d.ocula

import com.har01d.ocula.http.HttpMethod
import com.har01d.ocula.http.Request
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

interface RequestQueue {
    fun poll(): Request
    fun push(request: Request)
    fun isEmpty(): Boolean
}

class InMemoryRequestQueue(capacity: Int = 1000) : RequestQueue {
    private val queue: BlockingQueue<Request> =
            if (capacity <= 1000) ArrayBlockingQueue(capacity) else LinkedBlockingQueue(capacity)

    override fun poll(): Request = queue.take()
    override fun push(request: Request) = queue.put(request)
    override fun isEmpty(): Boolean = queue.isEmpty()
}

fun RequestQueue.enqueue(refer: String, vararg urls: String): Boolean {
    var success = false
    for (url in urls) {
        if (url.isBlank() || url == "#" || url.startsWith("javascript:")) {
            continue
        }
        val headers: MutableMap<String, Collection<String>> = mutableMapOf("Referer" to listOf(refer))
        val uri = normalizeUrl(refer, url)
        if (uri != null) {
            this.push(Request(uri, HttpMethod.GET, headers))
            success = true
        }
    }
    return success
}

fun RequestQueue.enqueue(refer: String, vararg requests: Request): Boolean {
    var success = false
    for (request in requests) {
        val url = request.url
        if (url.isBlank() || url == "#" || url.startsWith("javascript:")) {
            continue
        }
        val headers: MutableMap<String, Collection<String>> = mutableMapOf("Referer" to listOf(refer))
        val uri = normalizeUrl(refer, url)
        if (uri != null) {
            this.push(Request(uri, request.method, headers))
            success = true
        }
    }
    return success
}
