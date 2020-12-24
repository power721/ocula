package cn.har01d.ocula.http

import cn.har01d.ocula.util.generateId
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpCookie
import java.time.Duration

// Fixed thread pool is slower than cached thread pool
// follow redirects is global
class OkHttp3Client : AbstractHttpClient() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(OkHttp3Client::class.java)
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofMillis(timeout.toLong()))
            .connectTimeout(Duration.ofMillis(timeoutRead.toLong()))
            .followRedirects(true)
            .build()
    }

    override fun close() {
        client.dispatcher.executorService.shutdown()
    }

    override fun dispatch(request: Request): Response {
        val start = System.currentTimeMillis()
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        val builder = okhttp3.Request.Builder().url(request.url)
        request.headers.forEach { header ->
            header.value.forEach {
                builder.addHeader(header.key, it)
            }
        }
        if (request.method == HttpMethod.POST || request.method == HttpMethod.PUT || request.method == HttpMethod.PATCH) {
            buildRequestBody(request, builder)
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val contentLength = response.body!!.contentLength()
            logger.debug("[Response][$id] status code: ${response.code}  content length: $contentLength")
            return Response(
                request.url,
                response.body!!.string(),
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.headers("Set-Cookie").flatMap { HttpCookie.parse(it) },
                contentLength,
                System.currentTimeMillis() - start
            )
        }
    }

    override fun dispatch(request: Request, handler: (result: Result<Response>) -> Unit) {
        val start = System.currentTimeMillis()
        val id = generateId(6)
        logger.debug("[Request][$id] ${request.method} ${request.url}")
        val builder = okhttp3.Request.Builder().url(request.url)
        request.headers.forEach { header ->
            header.value.forEach {
                builder.addHeader(header.key, it)
            }
        }
        if (request.method == HttpMethod.POST || request.method == HttpMethod.PUT || request.method == HttpMethod.PATCH) {
            buildRequestBody(request, builder)
        }

        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler(Result.failure(e))
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                val contentLength = response.body!!.contentLength()
                logger.debug("[Response][$id] status code: ${response.code}  content length: $contentLength")
                val res = Response(
                    request.url,
                    response.body!!.string(),
                    response.code,
                    response.message,
                    response.headers.toMultimap(),
                    response.headers("Set-Cookie").flatMap { HttpCookie.parse(it) },
                    contentLength,
                    System.currentTimeMillis() - start
                )
                handler(Result.success(res))
            }
        })
    }

    private fun buildRequestBody(request: Request, builder: okhttp3.Request.Builder) {
        when (request.body) {
            is FormRequestBody -> {
                val form = FormBody.Builder()
                request.body.form.forEach {
                    form.add(it.key, it.value)
                }
                builder.method(request.method.name, form.build())
            }
            is FileRequestBody -> builder.method(request.method.name, request.body.file.asRequestBody())
            is TextRequestBody -> builder.method(request.method.name, request.body.text.toRequestBody())
            is JsonRequestBody -> builder.method(
                request.method.name,
                request.body.json.toRequestBody("application/json".toMediaType())
            )
            is BytesRequestBody -> builder.method(request.method.name, request.body.bytes.toRequestBody())
        }
    }
}
