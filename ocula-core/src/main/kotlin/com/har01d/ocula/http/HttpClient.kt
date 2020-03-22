package com.har01d.ocula.http

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.HttpCookie

interface HttpClient {
    fun dispatch(request: Request): Response
}

class FuelHttpClient : HttpClient {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FuelHttpClient::class.java)
    }

    override fun dispatch(request: Request): Response {
        logger.debug("[Request] ${request.method} ${request.url}")
        val req = when (request.method) {
            HttpMethod.GET -> request.url.httpGet(request.parameters)
            HttpMethod.POST -> request.url.httpPost(request.parameters)
            HttpMethod.PUT -> request.url.httpPut(request.parameters)
        }
        if (request.cookies.isNotEmpty()) {
            req.header("Cookie", request.cookies.joinToString("&"))
        }
        req.allowRedirects(request.allowRedirects)
        val (_, response, result) = req.header(request.headers.toMap()).responseString()
        when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> {
                logger.debug("[Response] status code: ${response.statusCode}  content length: ${response.contentLength}")
                return Response(
                        request.url,
                        result.get(),
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
