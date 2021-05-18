package cn.har01d.ocula.http

import cn.har01d.ocula.SpiderThreadFactory
import cn.har01d.ocula.util.generateId
import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.jsonBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.HttpCookie
import java.util.concurrent.Executors

class FuelHttpClient : AbstractHttpClient() {
    companion object {
        val logger: Logger =
            LoggerFactory.getLogger(FuelHttpClient::class.java)

        init {
            FuelManager.instance.executorService =
                Executors.newCachedThreadPool(
                    SpiderThreadFactory("Fuel")
                )
        }
    }

    override fun close() {}

// TODO: auto detect html charset

    override fun dispatch(request: Request): Response {
        val start = System.currentTimeMillis()
        val (id, req) = prepare(request)
        val (_, response, result) = req.responseString(request.charset ?: charset)
        when (result) {
            is com.github.kittinunf.result.Result.Failure -> throw result.getException()
            is com.github.kittinunf.result.Result.Success -> {
                val time = System.currentTimeMillis() - start
                logger.debug("[Response][$id] status code: ${response.statusCode}  content length: ${response.contentLength}  time: $time ms")
                return Response(
                    response.url.toExternalForm(),
                    result.value,
                    response.statusCode,
                    response.responseMessage,
                    response.headers,
                    response.headers["Set-Cookie"].flatMap { HttpCookie.parse(it) },
                    response.contentLength,
                    time
                )
            }
        }
    }

    override fun dispatch(request: Request, handler: (result: Result<Response>) -> Unit) {
        val start = System.currentTimeMillis()
        val (id, req) = prepare(request)
        req.responseString(request.charset ?: charset) { _, response, result ->
            when (result) {
                is com.github.kittinunf.result.Result.Failure -> handler(
                    Result.failure(
                        result.getException()
                    )
                )
                is com.github.kittinunf.result.Result.Success -> {
                    val time = System.currentTimeMillis() - start
                    logger.debug("[Response][$id] status code: ${response.statusCode}  content length: ${response.contentLength}  time: $time ms")
                    val res = Response(
                        response.url.toExternalForm(),
                        result.value,
                        response.statusCode,
                        response.responseMessage,
                        response.headers,
                        response.headers["Set-Cookie"].flatMap { HttpCookie.parse(it) },
                        response.contentLength,
                        time
                    )
                    handler(Result.success(res))
                }
            }
        }
    }

    private fun prepare(request: Request): Pair<String, com.github.kittinunf.fuel.core.Request> {
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        if (proxyProvider.hasAny()) {
            val httpProxy = proxyProvider.select()
            FuelManager.instance.proxy = httpProxy.toProxy()  /// TODO: not work
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
                is FormRequestBody -> {
                    req.header("Content-Type", "application/x-www-form-urlencoded")
                    req.parameters = it.form.toList()
                }
            }
        }
        return Pair(id, req)
    }
}
