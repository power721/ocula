package com.har01d.ocula.handler

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.util.md5
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

interface ResultHandler<in T> {
    fun handle(request: Request, response: Response, result: T)
}

object ConsoleLogResultHandler : ResultHandler<Any?> {
    override fun handle(request: Request, response: Response, result: Any?) {
        println(request.url)
        println(Objects.toString(result))
    }
}

abstract class FileResultHandler<T>(val directory: String) : ResultHandler<T> {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FileResultHandler::class.java)
    }

    private val logFile = File(directory, "files.log")

    init {
        File(directory).mkdirs()
    }

    fun record(url: String, file: File) {
        BufferedWriter(FileWriter(logFile, true)).use { out ->
            out.write("$url    $file\n")
        }
    }
}

class TextFileResultHandler(directory: String) : FileResultHandler<Any?>(directory) {
    override fun handle(request: Request, response: Response, result: Any?) {
        val file = File(directory, request.url.md5() + ".txt")
        BufferedWriter(FileWriter(file)).use { out ->
            out.write(Objects.toString(result))
            record(response.url, file)
            logger.info("write result to file $file")
        }
    }
}

class JsonFileResultHandler(directory: String) : FileResultHandler<Any?>(directory) {
    private val mapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    override fun handle(request: Request, response: Response, result: Any?) {
        val file = File(directory, request.url.md5() + ".json")
        BufferedWriter(FileWriter(file)).use { out ->
            out.write(mapper.writeValueAsString(result))
            record(response.url, file)
            logger.info("write result to file $file")
        }
    }
}

class HtmlResultHandler(directory: String) : FileResultHandler<Any?>(directory) {
    override fun handle(request: Request, response: Response, result: Any?) {
        val file = File(directory, request.url.md5() + ".html")
        BufferedWriter(FileWriter(file)).use { out ->
            out.write(response.body)
            record(response.url, file)
            logger.info("write response to file $file")
        }
    }
}
