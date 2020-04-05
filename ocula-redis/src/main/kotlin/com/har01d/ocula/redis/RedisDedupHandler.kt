package com.har01d.ocula.redis

import com.har01d.ocula.handler.DedupHandler
import com.har01d.ocula.http.Request
import com.har01d.ocula.listener.AbstractListener
import org.redisson.Redisson
import org.redisson.config.Config

class RedisDedupHandler(name: String, connection: String = "redis://127.0.0.1:6379") : DedupHandler, AbstractListener() {
    private val redisson by lazy {
        val config = Config()
        config.useSingleServer().address = connection
        Redisson.create(config)
    }
    private val set = redisson.getSet<String>(name)

    override fun handle(request: Request): Boolean {
        return set.add(request.url)
    }

    override fun onFinish() {
        redisson.shutdown()
    }
}
