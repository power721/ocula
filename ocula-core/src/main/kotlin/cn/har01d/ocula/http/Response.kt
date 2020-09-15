package cn.har01d.ocula.http

import cn.har01d.ocula.selector.Selector
import java.net.HttpCookie

data class Response(
        val url: String,
        val body: String,
        val statusCode: Int = -1,
        val responseMessage: String = "",
        val headers: Map<String, Collection<String>> = mapOf(),
        val cookies: List<HttpCookie> = listOf(),
        val contentLength: Long = 0L,
        val time: Long = 0L
) : Selector(body)
