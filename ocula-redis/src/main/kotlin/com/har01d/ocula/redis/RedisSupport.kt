package com.har01d.ocula.redis

import com.har01d.ocula.Spider
import com.har01d.ocula.listener.AbstractListener
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config

fun <T> Spider<T>.enableRedis(
    keyPrefix: String,
    connection: String = "redis://127.0.0.1:6379",
    includeCrawler: Boolean = false,
    shutdownRedisson: Boolean = true
): RedissonClient {
    val config = Config()
    config.codec = JsonJacksonCodec()
    config.useSingleServer().address = connection
    val redisson = Redisson.create(config)

    enableRedis(keyPrefix, redisson, includeCrawler, shutdownRedisson)
    return redisson
}

fun <T> Spider<T>.enableRedis(
    keyPrefix: String,
    redisson: RedissonClient,
    includeCrawler: Boolean = false,
    shutdownRedisson: Boolean = false
) {
    parser.queue = RedisRequestQueue("$keyPrefix-p-queue", redisson)
    parser.dedupHandler = RedisDedupHandler("$keyPrefix-set", redisson)
    statisticListener = RedisStatisticListener("$keyPrefix-stat", redisson)
    listeners += RedisErrorListener("$keyPrefix-failed", redisson)
    if (shutdownRedisson) listeners += RedissonShutdownListener(redisson)
    if (includeCrawler && crawler != null) {
        crawler!!.dedupHandler = parser.dedupHandler
        crawler!!.queue = RedisRequestQueue("$keyPrefix-c-queue", redisson)
    }
}

class RedissonShutdownListener(private val redisson: RedissonClient) : AbstractListener() {
    override val order: Int = Int.MAX_VALUE
    override fun onShutdown() {
        redisson.shutdown()
    }
}
