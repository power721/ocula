package cn.har01d.ocula.image

import cn.har01d.ocula.Spider
import cn.har01d.ocula.handler.FileResultHandler
import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.util.normalizeUrl
import com.github.kittinunf.fuel.httpDownload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

fun <T> Spider<T>.downloadImages(directory: String, fileProvider: FileProvider = DefaultFileProvider) {
    val handler = ImageResultHandler(directory, fileProvider)
    resultHandlers += handler
    run()
    logger.info("downloaded ${handler.count} images")
}

interface FileProvider {
    fun getFile(directory: String, fileName: String): File
}

object DefaultFileProvider : FileProvider {
    override fun getFile(directory: String, fileName: String) = File(directory, fileName)
}

open class ImageResultHandler(directory: String, private val fileProvider: FileProvider = DefaultFileProvider) :
    FileResultHandler<Any?>(directory) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(ImageResultHandler::class.java)
    }

    open val regex = ".*/(.*\\.(?:jpg|jpeg|png|webp|tiff|gif)).*".toRegex()
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

    open fun download(url: String?): Boolean {
        if (url == null) return false
        val name = regex.find(url)?.let {
            it.groupValues[1]
        } ?: return false
        val file = fileProvider.getFile(directory, name)
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
