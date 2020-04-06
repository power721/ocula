package com.har01d.ocula.redis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.har01d.ocula.http.Request
import com.har01d.ocula.listener.AbstractListener
import com.har01d.ocula.queue.RequestQueue
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import java.util.concurrent.TimeUnit

class RedisRequestQueue(name: String, connection: String = "redis://127.0.0.1:6379") : RequestQueue, AbstractListener() {
    private lateinit var redisson: RedissonClient
    private val queue by lazy { redisson.getBlockingQueue<String>(name) }
    private val mapper = jacksonObjectMapper()

    constructor(name: String, redisson: RedissonClient) : this(name, "") {
        this.redisson = redisson
    }

    init {
        if (connection.isNotEmpty()) {
            val config = Config()
            config.codec = JsonJacksonCodec()
            config.useSingleServer().address = connection
            redisson = Redisson.create(config)
        }
    }

    override fun take(): Request {
        return mapper.readValue(queue.take(), Request::class.java)
    }

    override fun poll(milliseconds: Long): Request? {
        val json = queue.poll(milliseconds, TimeUnit.MILLISECONDS) ?: return null
        return mapper.readValue(json, Request::class.java)
    }

    override fun push(request: Request) {
        queue.put(mapper.writeValueAsString(request))
    }

    override fun size(): Int {
        return queue.size
    }

    override fun isEmpty(): Boolean {
        return size() == 0
    }

    override fun onFinish() {
        redisson.shutdown()
    }
}
