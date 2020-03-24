package com.har01d.ocula.handler

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpDownload
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.util.md5
import com.har01d.ocula.util.normalizeUrl
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

class ImageResultHandler(directory: String) : FileResultHandler<Any?>(directory) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(ImageResultHandler::class.java)
    }

    private val regex = ".*/(.*\\.(?:jpg|jpeg|png|webp|tiff|gif)).*".toRegex()
    var count = 0

    override fun handle(request: Request, response: Response, result: Any?) {
        when (result) {
            is String -> if (!download(normalizeUrl(response.url, result))) {
                logger.warn("ignore $result")
            }
            is Collection<*> -> {
                for (item in result) {
                    if (item is String) {
                        if (!download(normalizeUrl(response.url, item))) {
                            logger.warn("ignore $item")
                        }
                    } else {
                        logger.warn("ignore $item")
                    }
                }
            }
            else -> logger.warn("ignore $result")
        }
    }

    private fun download(url: String?): Boolean {
        if (url == null) return false
        val name = regex.find(url)?.let {
            it.groupValues[1]
        } ?: return false
        val file = File(directory, name)
        if (file.exists()) {
            logger.info("file $file exists")
            return false
        }
        url.httpDownload().fileDestination { _, _ ->
            file.also { logger.info("download $url to $file") }
        }.response()
        record(url, file)
        count++
        return true
    }
}
