package cn.har01d.ocula.http

import java.nio.charset.Charset

data class Request(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val body: RequestBody? = null,
    val headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    val cookies: MutableList<HttpCookie> = mutableListOf(),
    val extra: MutableMap<String, Any?> = mutableMapOf(),
    val charset: Charset? = null,
    val allowRedirects: Boolean = true
)

fun header(name: String, vararg value: String) = name to listOf(*value)
fun headers(vararg headers: Pair<String, Collection<String>>) = mutableMapOf(*headers)

fun accept(accept: String) = header("Accept", accept)
fun acceptCharset(charset: String) = header("Accept-Charset", charset)
fun acceptEncoding(encoding: String) = header("Accept-Encoding", encoding)
fun acceptLanguage(language: String) = header("Accept-Language", language)
fun acceptRanges(ranges: String) = header("Accept-Ranges", ranges)
fun contentType(type: String) = header("Content-Type", type)
fun referer(referer: String) = header("Referer", referer)
fun userAgent(userAgent: String) = header("User-Agent", userAgent)

fun String.jsonRequest(
    method: HttpMethod = HttpMethod.GET,
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(
    this,
    method,
    body,
    headers.apply { putIfAbsent("Accept", listOf("application/json")) },
    cookies,
    extra,
    charset,
    allowRedirects
)

fun String.get(
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.GET, body, headers, cookies, extra, charset, allowRedirects)

fun String.post(
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.POST, body, headers, cookies, extra, charset, allowRedirects)

fun String.put(
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.PUT, body, headers, cookies, extra, charset, allowRedirects)

fun String.patch(
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.PATCH, body, headers, cookies, extra, charset, allowRedirects)

fun String.delete(
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.DELETE, body, headers, cookies, extra, charset, allowRedirects)

fun String.head(
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.HEAD, body, headers, cookies, extra, charset, allowRedirects)
