package com.har01d.ocula.redis

import com.har01d.ocula.Spider
import com.har01d.ocula.listener.AbstractListener
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config

fun <T> Spider<T>.enableRedis(keyPrefix: String, connection: String = "redis://127.0.0.1:6379", includeCrawler: Boolean = false) {
    val config = Config()
    config.codec = JsonJacksonCodec()
    config.useSingleServer().address = connection
    val redisson = Redisson.create(config)

    enableRedis(keyPrefix, redisson, includeCrawler)
}

fun <T> Spider<T>.enableRedis(keyPrefix: String, redisson: RedissonClient, includeCrawler: Boolean = false) {
    parser.dedupHandler = RedisDedupHandler("$keyPrefix-set", redisson)
    parser.queue = RedisRequestQueue("$keyPrefix-p-queue", redisson)
    statisticListener = RedisStatisticListener("$keyPrefix-stat", redisson)
    if (includeCrawler && crawler != null) {
        crawler!!.dedupHandler = parser.dedupHandler
        crawler!!.queue = RedisRequestQueue("$keyPrefix-c-queue", redisson)
    }
}

class RedissonListener(private val redisson: RedissonClient) : AbstractListener() {
    override fun onShutdown() {
        redisson.shutdown()
    }
}
