package com.har01d.ocula.http

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import com.har01d.ocula.util.generateId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.HttpCookie

interface HttpClient {
    var userAgents: List<String>
    var httpProxies: List<HttpProxy>
    fun dispatch(request: Request): Response
}

class FuelHttpClient : HttpClient {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FuelHttpClient::class.java)
    }

    override var userAgents = listOf<String>()
    override var httpProxies = listOf<HttpProxy>()

    override fun dispatch(request: Request): Response {
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        if (httpProxies.isNotEmpty()) {
            val httpProxy = httpProxies.random()
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
        if (userAgents.isNotEmpty()) {
            req.header("User-Agent", userAgents.random())
        }
        val (_, response, result) = req.header(request.headers.toMap()).responseString()
        when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> {
                logger.debug("[Response][$id] status code: ${response.statusCode}  content length: ${response.contentLength}")
                return Response(
                        request.url,
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
