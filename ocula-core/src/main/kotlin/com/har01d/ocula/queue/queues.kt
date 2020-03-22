package com.har01d.ocula.queue

import com.har01d.ocula.http.Request
import com.har01d.ocula.util.normalizeUrl
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

interface RequestQueue {
    fun take(): Request
    fun poll(milliseconds: Long): Request?
    fun push(request: Request)
    fun isEmpty(): Boolean
}

class InMemoryRequestQueue(capacity: Int = 1000) : RequestQueue {
    private val queue: BlockingQueue<Request> =
            if (capacity <= 1000) ArrayBlockingQueue(capacity) else LinkedBlockingQueue(capacity)

    override fun take(): Request = queue.take()
    override fun poll(milliseconds: Long): Request? = queue.poll(milliseconds, TimeUnit.MILLISECONDS)
    override fun push(request: Request) = queue.put(request)
    override fun isEmpty(): Boolean = queue.isEmpty()
}

fun RequestQueue.enqueue(refer: String, vararg urls: String): Boolean {
    var success = false
    for (url in urls) {
        if (url.isBlank() || url == "#" || url.startsWith("javascript:")) {
            continue
        }
        val uri = normalizeUrl(refer, url)
        if (uri != null) {
            val headers: MutableMap<String, Collection<String>> = mutableMapOf("Referer" to listOf(refer))
            this.push(Request(uri, headers = headers))
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
        val uri = normalizeUrl(refer, url)
        if (uri != null) {
            request.headers["Referer"] = listOf(refer)
            this.push(request.copy(url = uri))
            success = true
        }
    }
    return success
}
