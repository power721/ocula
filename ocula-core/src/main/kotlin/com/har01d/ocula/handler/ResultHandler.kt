package com.har01d.ocula.handler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpDownload
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

object LogResultHandler : ResultHandler<Any?> {
    override fun handle(request: Request, response: Response, result: Any?) {
        println(request.url)
        println(Objects.toString(result))
    }
}

class TextFileResultHandler(private val file: String) : ResultHandler<Any?> {
    override fun handle(request: Request, response: Response, result: Any?) {
        BufferedWriter(FileWriter(file, true)).use { out -> out.write(Objects.toString(result)) }
    }
}

class JsonFileResultHandler(private val file: String) : ResultHandler<Any?> {
    private val mapper = jacksonObjectMapper()
    override fun handle(request: Request, response: Response, result: Any?) {
        BufferedWriter(FileWriter(file, true)).use { out -> out.write(mapper.writeValueAsString(result)) }
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

class ImageResultHandler(private val directory: String) : ResultHandler<Any?> {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(ImageResultHandler::class.java)
    }

    private val regex = ".*/(.*\\.(?:jpg|jpeg|png|webp|tiff|gif)).*".toRegex()

    init {
        File(directory).mkdirs()
    }

    override fun handle(request: Request, response: Response, result: Any?) {
        when (result) {
            is String -> download(fixUrl(response.url, result))
            is Collection<*> -> {
                for (item in result) {
                    if (item is String) {
                        download(fixUrl(response.url, item))
                    } else {
                        logger.warn("ignore $item")
                    }
                }
            }
            else -> logger.warn("ignore $result")
        }
    }

    private fun download(url: String) {
        val name = regex.find(url)?.let {
            it.groupValues[1]
        } ?: return
        val file = File(directory, name)
        if (file.exists()) return
        url.httpDownload().fileDestination { _, _ ->
            file.also { logger.info("download $url to $file") }
        }.response()
    }

    private fun fixUrl(refer: String, url: String): String = if (url.startsWith("http")) url else if (url.startsWith("//")) "https:$url" else "$refer/$url"
}
