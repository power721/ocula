package com.har01d.ocula.http

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

class ApacheHttpClient : AbstractHttpClient() {
    companion object {
        val logger: Logger =
            LoggerFactory.getLogger(ApacheHttpClient::class.java)
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
            val time = System.currentTimeMillis() - start
            logger.debug("[Response][$id] status code: ${response.statusLine.statusCode}  content length: $contentLength  time: $time ms")
            val headers = convertHeaders(response.allHeaders)
            val url = context.getAttribute("uri") as String? ?: request.url
            return Response(
                url,
                EntityUtils.toString(response.entity, charset),
                response.statusLine.statusCode,
                response.statusLine.reasonPhrase,
                headers,
                response.getHeaders("Set-Cookie").flatMap { HttpCookie.parse(it.value) },
                contentLength,
                System.currentTimeMillis() - start
            )
        }
    }

    override fun dispatch(request: Request, handler: (result: Result<Response>) -> Unit) {
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        val start = System.currentTimeMillis()
        val httpRequest = convertRequest(request)
        val context = HttpClientContext.create()
        asyncClient.execute(httpRequest, context, object :
            FutureCallback<HttpResponse> {
            override fun cancelled() {}

            override fun completed(response: HttpResponse) {
                val contentLength = response.entity.contentLength
                val time = System.currentTimeMillis() - start
                logger.debug("[Response][$id] status code: ${response.statusLine.statusCode}  content length: $contentLength  time: $time ms")
                val headers = convertHeaders(response.allHeaders)
                val url = context.getAttribute("uri") as String? ?: request.url
                val res = Response(
                    url,
                    EntityUtils.toString(response.entity, charset),
                    response.statusLine.statusCode,
                    response.statusLine.reasonPhrase,
                    headers,
                    response.getHeaders("Set-Cookie")
                        .flatMap { HttpCookie.parse(it.value) },
                    contentLength,
                    System.currentTimeMillis() - start
                )
                handler(Result.success(res))
            }

            override fun failed(e: Exception) {
                handler(Result.failure(e))
            }
        })
    }

    private fun convertRequest(request: Request): HttpUriRequest {
        val httpRequest = when (request.method) {
            HttpMethod.GET -> HttpGet(
                request.url
            )
            HttpMethod.POST -> HttpPost(
                request.url
            ).apply { entity = convertEntity(request) }
            HttpMethod.PUT -> HttpPut(
                request.url
            ).apply { entity = convertEntity(request) }
            HttpMethod.PATCH -> HttpPatch(
                request.url
            ).apply { entity = convertEntity(request) }
            HttpMethod.DELETE -> HttpDelete(
                request.url
            )
            HttpMethod.HEAD -> HttpHead(
                request.url
            )
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
                is TextRequestBody -> StringEntity(
                    body.text
                )
                is JsonRequestBody -> StringEntity(
                    body.json,
                    ContentType.APPLICATION_JSON
                )
                is BytesRequestBody -> ByteArrayEntity(
                    body.bytes
                )
                is FileRequestBody -> FileEntity(
                    body.file
                )
                is FormRequestBody -> UrlEncodedFormEntity(
                    body.form.map { BasicNameValuePair(it.key, it.value) })
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
