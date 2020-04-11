package com.har01d.ocula.http

import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.har01d.ocula.SpiderThreadFactory
import com.har01d.ocula.util.generateId
import com.har01d.ocula.util.host
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.concurrent.FutureCallback
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.FileEntity
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.cookie.BasicClientCookie
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
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
    override var userAgentProvider: UserAgentProvider = EmptyUserAgentProvider
    override var proxyProvider: ProxyProvider = EmptyProxyProvider
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


class ApacheHttpClient : AbstractHttpClient() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(ApacheHttpClient::class.java)
    }

    private val cookieStore = BasicCookieStore()

    private val client by lazy {
        HttpClients.custom()
            .setMaxConnPerRoute(10)
            .setDefaultCookieStore(cookieStore)
            .setRedirectStrategy(SpiderRedirectStrategy)
            .build()
    }

    private val asyncClient = HttpAsyncClients.custom()
        .setMaxConnPerRoute(10)
        .setDefaultCookieStore(cookieStore)
        .setRedirectStrategy(SpiderRedirectStrategy)
        .setThreadFactory(SpiderThreadFactory("HTTP"))
        .build()

    init {
        asyncClient.start()
    }

    override fun dispatch(request: Request): Response {
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        val start = System.currentTimeMillis()
        val httpRequest = convertRequest(request)
        val context = HttpClientContext.create()
        client.execute(httpRequest, context).use { response ->
            val contentLength = response.entity.contentLength
            logger.debug("[Response][$id] status code: ${response.statusLine.statusCode}  content length: $contentLength")
            val headers = convertHeaders(response.allHeaders)
            val url = context.getAttribute("uri") as String? ?: request.url
            return Response(
                url,
                EntityUtils.toString(response.entity),
                response.statusLine.statusCode,
                response.statusLine.reasonPhrase,
                headers,
                response.getHeaders("Set-Cookie").flatMap { HttpCookie.parse(it.value) },
                contentLength,
                System.currentTimeMillis() - start
            )
        }
    }

    override fun dispatch(request: Request, handler: (result: kotlin.Result<Response>) -> Unit) {
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        val start = System.currentTimeMillis()
        val httpRequest = convertRequest(request)
        val context = HttpClientContext.create()
        asyncClient.execute(httpRequest, context, object : FutureCallback<HttpResponse> {
            override fun cancelled() {}

            override fun completed(response: HttpResponse) {
                val contentLength = response.entity.contentLength
                logger.debug("[Response][$id] status code: ${response.statusLine.statusCode}  content length: $contentLength")
                val headers = convertHeaders(response.allHeaders)
                val url = context.getAttribute("uri") as String? ?: request.url
                val res = Response(
                    url,
                    EntityUtils.toString(response.entity),
                    response.statusLine.statusCode,
                    response.statusLine.reasonPhrase,
                    headers,
                    response.getHeaders("Set-Cookie").flatMap { HttpCookie.parse(it.value) },
                    contentLength,
                    System.currentTimeMillis() - start
                )
                handler(kotlin.Result.success(res))
            }

            override fun failed(e: Exception) {
                handler(kotlin.Result.failure(e))
            }
        })
    }

    private fun convertRequest(request: Request): HttpUriRequest {
        val httpRequest = when (request.method) {
            HttpMethod.GET -> HttpGet(request.url)
            HttpMethod.POST -> HttpPost(request.url).apply { entity = convertEntity(request) }
            HttpMethod.PUT -> HttpPut(request.url).apply { entity = convertEntity(request) }
            HttpMethod.PATCH -> HttpPatch(request.url).apply { entity = convertEntity(request) }
            HttpMethod.DELETE -> HttpDelete(request.url)
            HttpMethod.HEAD -> HttpHead(request.url)
        }
        request.headers.forEach { (name, list) ->
            list.forEach {
                httpRequest.addHeader(name, it)
            }
        }
        if (request.cookies.isNotEmpty()) {
            request.cookies.forEach {
                val cookie = BasicClientCookie(it.name, it.value)
                cookie.domain = request.url.host()
                cookie.path = "/"
                cookieStore.addCookie(cookie)
            }
        }
        if (userAgentProvider.hasAny()) {
            httpRequest.setHeader("User-Agent", userAgentProvider.select())
        }
        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeoutRead)
            .setRedirectsEnabled(request.allowRedirects)
        if (proxyProvider.hasAny()) {
            val httpProxy = proxyProvider.select()
            requestConfig.setProxy(HttpHost(httpProxy.hostname, httpProxy.port))
        }
        httpRequest.config = requestConfig.build()
        return httpRequest
    }

    private fun convertEntity(request: Request): HttpEntity? {
        return request.body?.let { body ->
            when (body) {
                is TextRequestBody -> StringEntity(body.text)
                is JsonRequestBody -> StringEntity(body.json, ContentType.APPLICATION_JSON)
                is BytesRequestBody -> ByteArrayEntity(body.bytes)
                is FileRequestBody -> FileEntity(body.file)
                is FormRequestBody -> UrlEncodedFormEntity(body.form.map { BasicNameValuePair(it.key, it.value) })
            }
        }
    }

    private fun convertHeaders(array: Array<Header>): Map<String, Collection<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        array.forEach {
            val list = map.getOrDefault(it.name, mutableListOf())
            list += it.value
            map[it.name] = list
        }
        return map
    }

    override fun close() {
        client.close()
        asyncClient.close()
    }
}
