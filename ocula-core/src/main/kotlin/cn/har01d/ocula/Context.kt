package cn.har01d.ocula

import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.Response

interface Context {
    var name: String
    val config: Config
    fun crawl(refer: String, vararg urls: String): Boolean
    fun crawl(refer: String, vararg requests: Request): Boolean
    fun follow(refer: String, vararg urls: String): Boolean
    fun follow(refer: String, vararg requests: Request): Boolean
    fun dispatch(request: Request): Response
    fun dispatch(url: String): Response
    fun stop(): Boolean
    fun abort(stop: Boolean = false)
    fun finish()
    fun reset()
}
