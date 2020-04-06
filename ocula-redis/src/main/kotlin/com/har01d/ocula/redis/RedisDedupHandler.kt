package com.har01d.ocula.redis

import com.har01d.ocula.handler.DedupHandler
import com.har01d.ocula.http.Request
import com.har01d.ocula.listener.AbstractListener
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config

class RedisDedupHandler(name: String, connection: String = "redis://127.0.0.1:6379") : DedupHandler, AbstractListener() {
    private lateinit var redisson: RedissonClient
    private val set by lazy { redisson.getSet<String>(name) }

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

    override fun handle(request: Request): Boolean {
        return set.add(request.url)
    }

    override fun onFinish() {
        redisson.shutdown()
    }
}
