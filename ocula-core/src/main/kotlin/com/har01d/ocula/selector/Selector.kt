package com.har01d.ocula.selector

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.TypeRef
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import us.codecraft.xsoup.Xsoup

open class Selector(private val body: String) {
    private val document: Document by lazy {
        Jsoup.parse(body)
    }

    private val json: DocumentContext by lazy {
        JsonPath.parse(body)
    }

    fun links(cssQuery: String = "a[href]"): List<String> = select(cssQuery).map { it.attr("href") }

    fun images(cssQuery: String = "img[src]"): List<String> = select(cssQuery).map { it.attr("src") }

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

operator fun Element.get(key: String): String = attr(key)

operator fun Elements.get(key: String): String = attr(key)

fun Element.toDouble(): Double? {
    val text = text().trim()
    return if (text.isEmpty()) {
        null
    } else {
        text.toDouble()
    }
}

fun Element.toInt(): Int? {
    val text = text().trim()
    return if (text.isEmpty()) {
        null
    } else {
        text.toInt()
    }
}

fun Element.toLong(): Long? {
    val text = text().trim()
    return if (text.isEmpty()) {
        null
    } else {
        text.toLong()
    }
}

fun Element.texts(): String {
    val sb = StringBuilder()
    for (element in childNodes()) {
        if (sb.isNotEmpty()) sb.append(" ")
        if (element.nodeName() == "br") {
            sb.append("\n")
        } else {
            sb.append(if (element is Element) element.text() else if (element is TextNode) element.text() else "")
        }
    }
    return sb.toString()
}

fun Element.lines(): List<String> {
    val list = mutableListOf<String>()
    val sb = StringBuilder()
    for (element in childNodes()) {
        if (sb.isNotEmpty()) sb.append(" ")
        // TODO: handle "li", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5", "tr"
        if (element.nodeName() == "br") {
            list += sb.toString().trim()
            sb.clear()
        } else {
            sb.append(if (element is Element) element.text() else if (element is TextNode) element.text() else "")
        }
    }
    return list
}

fun Elements.string(): String {
    return this.fold(StringBuilder()) { sb, e ->
        sb.append(e.texts())
    }.toString()
}

fun Elements.lines(): List<String> {
    return this.fold(mutableListOf()) { list, e ->
        list.apply { addAll(e.lines()) }
    }
}

fun Elements.texts(): List<String> {
    return this.map { it.text() }
}
