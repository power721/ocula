package com.har01d.ocula.http

import java.net.HttpCookie

data class Request(
        val url: String,
        val method: HttpMethod = HttpMethod.GET,
        val parameters: List<Pair<String, Any?>>? = null,
        val headers: MutableMap<String, Collection<String>> = mutableMapOf(),
        val cookies: MutableList<HttpCookie> = mutableListOf()
)
