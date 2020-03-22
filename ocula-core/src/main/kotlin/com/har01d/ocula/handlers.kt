package com.har01d.ocula

import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

interface PreHandler {
    var spider: Spider<*>
    fun handle(request: Request)
}

abstract class AbstractPreHandler : PreHandler {
    override lateinit var spider: Spider<*>
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
