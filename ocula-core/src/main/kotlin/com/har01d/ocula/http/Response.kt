package com.har01d.ocula.http

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.HttpCookie

data class Response(
        val url: String,
        val body: String,
        val statusCode: Int = -1,
        val responseMessage: String = "",
        val headers: Map<String, Collection<String>> = mapOf(),
        val cookies: List<HttpCookie> = listOf(),
        val contentLength: Long = 0L
) {
    private val document: Document by lazy {
        Jsoup.parse(body)
    }

    private val jsonDocument: DocumentContext by lazy {
        JsonPath.parse(body)
    }

    fun xpath(path: String) {

    }

    fun select(cssQuery: String): Elements = document.select(cssQuery)

    fun <T> jsonPath(path: String): T = jsonDocument.read(path)

    fun regex(pattern: String): MatchResult? {
        val regex = pattern.toRegex()
        return regex.find(body)
    }
}
