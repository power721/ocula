package com.har01d.ocula.queue

import com.har01d.ocula.http.Request
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
