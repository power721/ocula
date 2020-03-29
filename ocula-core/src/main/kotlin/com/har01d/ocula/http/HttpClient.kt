package com.har01d.ocula.http

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import com.har01d.ocula.util.generateId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.HttpCookie
import java.nio.charset.Charset

interface HttpClient {
    var userAgentProvider: UserAgentProvider
    var proxyProvider: ProxyProvider
    var charset: Charset
    fun dispatch(request: Request): Response
}

abstract class AbstractHttpClient : HttpClient {
    override lateinit var userAgentProvider: UserAgentProvider
    override lateinit var proxyProvider: ProxyProvider
    override lateinit var charset: Charset
}

class FuelHttpClient : AbstractHttpClient() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FuelHttpClient::class.java)
    }

    // TODO: auto detect html charset

    override fun dispatch(request: Request): Response {
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
            HttpMethod.GET -> request.url.httpGet(request.parameters)
            HttpMethod.POST -> request.url.httpPost(request.parameters)
            HttpMethod.PUT -> request.url.httpPut(request.parameters)
            HttpMethod.PATCH -> request.url.httpPatch(request.parameters)
            HttpMethod.DELETE -> request.url.httpDelete(request.parameters)
            HttpMethod.HEAD -> request.url.httpHead(request.parameters)
        }
        req.allowRedirects(request.allowRedirects)
        if (request.cookies.isNotEmpty()) {
            req.header("Cookie", request.cookies.joinToString("&"))
        }
        if (userAgentProvider.hasAny()) {
            req.header("User-Agent", userAgentProvider.select())
        }
        val (_, response, result) = req.header(request.headers.toMap()).responseString(request.charset ?: charset)
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
                        response.contentLength
                )
            }
        }
    }
}
