package com.har01d.ocula.http

import java.net.HttpCookie
import java.nio.charset.Charset

data class Request(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val parameters: List<Pair<String, Any?>>? = null,
    val body: RequestBody? = null,
    val headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    val cookies: MutableList<HttpCookie> = mutableListOf(),
    val extra: MutableMap<String, Any?> = mutableMapOf(),
    val charset: Charset? = null,
    val allowRedirects: Boolean = true
)

fun String.get(
    vararg parameters: Pair<String, Any?>,
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.GET, parameters.toList(), body, headers, cookies, extra, charset, allowRedirects)

fun String.post(
    vararg parameters: Pair<String, Any?>,
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.POST, parameters.toList(), body, headers, cookies, extra, charset, allowRedirects)

fun String.put(
    vararg parameters: Pair<String, Any?>,
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.PUT, parameters.toList(), body, headers, cookies, extra, charset, allowRedirects)

fun String.patch(
    vararg parameters: Pair<String, Any?>,
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.PATCH, parameters.toList(), body, headers, cookies, extra, charset, allowRedirects)

fun String.delete(
    vararg parameters: Pair<String, Any?>,
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.DELETE, parameters.toList(), body, headers, cookies, extra, charset, allowRedirects)

fun String.head(
    vararg parameters: Pair<String, Any?>,
    body: RequestBody? = null,
    headers: MutableMap<String, Collection<String>> = mutableMapOf(),
    cookies: MutableList<HttpCookie> = mutableListOf(),
    extra: MutableMap<String, Any?> = mutableMapOf(),
    charset: Charset? = null,
    allowRedirects: Boolean = true
) = Request(this, HttpMethod.HEAD, parameters.toList(), body, headers, cookies, extra, charset, allowRedirects)
