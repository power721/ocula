package cn.har01d.ocula.redis

import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.listener.AbstractListener
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config

class RedisErrorListener(name: String, connection: String = "redis://127.0.0.1:6379") : AbstractListener() {
    private lateinit var redisson: RedissonClient
    private val list by lazy { redisson.getList<String>(name) }

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

    override fun onDownloadFailed(request: Request, e: Throwable) {
        list.add(request.url)
    }

    override fun onCrawlFailed(request: Request, e: Throwable) {
        list.add(request.url)
    }

    override fun onParseFailed(request: Request, response: Response, e: Throwable) {
        list.add(request.url)
    }
}
