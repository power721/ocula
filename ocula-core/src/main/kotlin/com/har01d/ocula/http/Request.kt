package com.har01d.ocula.http

data class Request(
        val url: String,
        val method: HttpMethod = HttpMethod.GET,
        val headers: MutableMap<String, Collection<String>> = mutableMapOf()
)
