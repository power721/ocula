package com.har01d.ocula

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.TypeRef
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import us.codecraft.xsoup.Xsoup

open class Selector(private val body: String) {
    private val document: Document by lazy {
        Jsoup.parse(body)
    }

    private val json: DocumentContext by lazy {
        JsonPath.parse(body)
    }

    fun xpath(expression: String): List<String> = Xsoup.compile(expression).evaluate(document).list()

    fun <T> xpath(expression: String, clazz: Class<T>): T {
        return when (clazz) {
            String::class.java -> Xsoup.compile(expression).evaluate(document).get() as T
            Short::class.java -> Xsoup.compile(expression).evaluate(document).get().toShort() as T
            Int::class.java -> Xsoup.compile(expression).evaluate(document).get().toInt() as T
            Long::class.java -> Xsoup.compile(expression).evaluate(document).get().toLong() as T
            Float::class.java -> Xsoup.compile(expression).evaluate(document).get().toFloat() as T
            Double::class.java -> Xsoup.compile(expression).evaluate(document).get().toDouble() as T
            Boolean::class.java -> Xsoup.compile(expression).evaluate(document).get().toBoolean() as T
            List::class.java -> Xsoup.compile(expression).evaluate(document).list() as T
            else -> throw IllegalArgumentException("Unsupported type $clazz")
        }
    }

    fun select(cssQuery: String): Elements = document.select(cssQuery)

    fun select(cssQuery: String, attr: String): String = document.select(cssQuery).attr(attr)

    fun <T> jsonPath(path: String, clazz: Class<T>): T = json.read(path, clazz)

    fun <T> jsonPath(path: String, type: TypeRef<T>): T = json.read(path, type)

    fun <T> jsonPath(path: String): T = json.read(path)

    fun regex(pattern: String): MatchResult? {
        val regex = pattern.toRegex()
        return regex.find(body)
    }

    fun regex(pattern: String, group: Int): String? {
        val regex = pattern.toRegex()
        return regex.find(body)?.groupValues?.get(group)
    }
}
