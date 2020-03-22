package com.har01d.ocula

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.HttpCookie

interface Downloader {
    fun download(request: Request): Response
}

class FuelDownloader : Downloader {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FuelDownloader::class.java)
    }

    override fun download(request: Request): Response {
        logger.debug("[Request] ${request.method} ${request.url}")
        val (_, response, result) = request.url.httpGet().header(request.headers.toMap()).responseString()
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
