package com.har01d.ocula

import com.har01d.ocula.http.HttpMethod
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

typealias Parameters = List<Pair<String, Any?>>

interface PreHandler {
    var spider: Spider<*>
    fun handle(request: Request)
}

abstract class AbstractPreHandler : PreHandler {
    override lateinit var spider: Spider<*>
}

abstract class AuthHandler : AbstractPreHandler()

class BasicAuthHandler(private val username: String, private val password: String) : AuthHandler() {
    override fun handle(request: Request) {
        request.headers["Authorization"] = listOf("Basic " + Base64.getEncoder().encode("$username:$password".toByteArray()))
    }
}

class TokenAuthHandler(private val token: String, private val header: String = "Authorization") : AuthHandler() {
    override fun handle(request: Request) {
        request.headers[header] = listOf(token)
    }
}

val sessionHandler = fun(request: Request, response: Response) {
    response.cookies.find { it.name == "session" }
            ?.let {
                request.cookies += it
            }
}

class FormAuthHandler(private val actionUrl: String, private val parameters: Parameters, val block: (request: Request, response: Response) -> Unit = sessionHandler) : AuthHandler() {
    override fun handle(request: Request) {
        val formRequest = Request(actionUrl, HttpMethod.POST, parameters)
        val response = spider.downloader.dispatch(formRequest)
        block(request, response)
    }
}

interface PostHandler {
    var spider: Spider<*>
    fun handle(request: Request)
}

abstract class AbstractPostHandler : PostHandler {
    override lateinit var spider: Spider<*>
}

interface ResultHandler<in T> {
    fun handle(request: Request, response: Response, result: T)
}

object LogResultHandler : ResultHandler<Any?> {
    override fun handle(request: Request, response: Response, result: Any?) {
        println(request.url)
        println(Objects.toString(result))
    }
}

class FileResultHandler(private val file: String) : ResultHandler<Any?> {
    override fun handle(request: Request, response: Response, result: Any?) {
        BufferedWriter(FileWriter(file, true)).use { out -> out.write(Objects.toString(result)) }
    }
}

class HtmlResultHandler(private val directory: String) : ResultHandler<Any?> {
    init {
        File(directory).mkdirs()
    }

    override fun handle(request: Request, response: Response, result: Any?) {
        val file = File(directory, request.url.md5() + ".html")
        BufferedWriter(FileWriter(file)).use { out ->
            out.write(response.body)
        }
    }
}
