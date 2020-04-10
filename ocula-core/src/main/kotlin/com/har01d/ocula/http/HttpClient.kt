package com.har01d.ocula.http

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.har01d.ocula.SpiderThreadFactory
import com.har01d.ocula.util.generateId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.HttpCookie
import java.nio.charset.Charset
import java.util.concurrent.Executors
import kotlin.Result as KResult

interface HttpClient : AutoCloseable {
    var userAgentProvider: UserAgentProvider
    var proxyProvider: ProxyProvider
    var charset: Charset
    var timeout: Int
    var timeoutRead: Int
    fun dispatch(request: Request): Response
    fun dispatch(request: Request, handler: (result: KResult<Response>) -> Unit)
    override fun close()
}

abstract class AbstractHttpClient : HttpClient {
    override lateinit var userAgentProvider: UserAgentProvider
    override lateinit var proxyProvider: ProxyProvider
    override var charset: Charset = Charsets.UTF_8
    override var timeout: Int = 15000
    override var timeoutRead: Int = 15000
}

class FuelHttpClient : AbstractHttpClient() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FuelHttpClient::class.java)

        init {
            FuelManager.instance.executorService = Executors.newCachedThreadPool(SpiderThreadFactory("Fuel"))
        }
    }

    override fun close() {}

// TODO: auto detect html charset

    override fun dispatch(request: Request): Response {
        val start = System.currentTimeMillis()
        val (id, req) = prepare(request)
        val (_, response, result) = req.responseString(request.charset ?: charset)
        when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> {
                logger.debug("[Response][$id] status code: ${response.statusCode}  content length: ${response.contentLength}")
                return Response(
                    response.url.toExternalForm(),
                    result.value,
                    response.statusCode,
                    response.responseMessage,
                    response.headers,
                    response.headers["Set-Cookie"].flatMap { HttpCookie.parse(it) },
                    response.contentLength,
                    System.currentTimeMillis() - start
                )
            }
        }
    }

    override fun dispatch(request: Request, handler: (result: KResult<Response>) -> Unit) {
        val start = System.currentTimeMillis()
        val (id, req) = prepare(request)
        req.responseString(request.charset ?: charset) { _, response, result ->
            when (result) {
                is Result.Failure -> handler(KResult.failure(result.getException()))
                is Result.Success -> {
                    logger.debug("[Response][$id] status code: ${response.statusCode}  content length: ${response.contentLength}")
                    val res = Response(
                        response.url.toExternalForm(),
                        result.value,
                        response.statusCode,
                        response.responseMessage,
                        response.headers,
                        response.headers["Set-Cookie"].flatMap { HttpCookie.parse(it) },
                        response.contentLength,
                        System.currentTimeMillis() - start
                    )
                    handler(KResult.success(res))
                }
            }
        }
    }

    private fun prepare(request: Request): Pair<String, com.github.kittinunf.fuel.core.Request> {
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        if (proxyProvider.hasAny()) {
            val httpProxy = proxyProvider.select()
            FuelManager.instance.proxy = httpProxy.toProxy()
            logger.debug("[Proxy][$id] use $httpProxy")
        } else {
            FuelManager.instance.proxy = null
        }
        val req = when (request.method) {
            HttpMethod.GET -> request.url.httpGet()
            HttpMethod.POST -> request.url.httpPost()
            HttpMethod.PUT -> request.url.httpPut()
            HttpMethod.PATCH -> request.url.httpPatch()
            HttpMethod.DELETE -> request.url.httpDelete()
            HttpMethod.HEAD -> request.url.httpHead()
        }
        req.allowRedirects(request.allowRedirects)
        if (request.cookies.isNotEmpty()) {
            req.header("Cookie", request.cookies.joinToString("&"))
        }
        if (userAgentProvider.hasAny()) {
            req.header("User-Agent", userAgentProvider.select())
        }
        req.header(request.headers.toMap())
        req.timeout(timeout)
        req.timeoutRead(timeoutRead)
        request.body?.let {
            when (it) {
                is TextRequestBody -> req.body(it.text)
                is JsonRequestBody -> req.jsonBody(it.json)
                is BytesRequestBody -> req.body(it.bytes)
                is FileRequestBody -> req.body(it.file)
                is FormRequestBody -> req.parameters = it.form.toList()
            }
        }
        return Pair(id, req)
    }
}
