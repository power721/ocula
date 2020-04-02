package com.har01d.ocula.selenium

import com.har01d.ocula.http.AbstractHttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SeleniumHttpClient(private val webDriverProvider: WebDriverProvider) : AbstractHttpClient() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(SeleniumHttpClient::class.java)
    }

    var actionHandler: SeleniumActionHandler? = null

    override fun dispatch(request: Request): Response {
        val start = System.currentTimeMillis()
        logger.debug("[Request] handle ${request.url}")
        val webDriver = webDriverProvider.take()
        try {
            val options = webDriver.manage()
            for (entry in request.cookies) {
                val cookie = Cookie(entry.name, entry.value)
                options.addCookie(cookie)
            }
            webDriver[request.url]

            actionHandler?.handle(request, webDriver)

            val webElement = webDriver.findElement(By.xpath("/html"))
            val content = webElement.getAttribute("outerHTML")
            options.deleteAllCookies()
            return Response(
                    request.url,
                    content,
                    200,
                    contentLength = content.length.toLong(),
                    time = System.currentTimeMillis() - start
            )
        } finally {
            webDriverProvider.release(webDriver)
        }
    }

    override fun dispatch(request: Request, handler: (result: Result<Response>) -> Unit) {
        val start = System.currentTimeMillis()
        logger.debug("[Request] handle ${request.url}")
        val webDriver = webDriverProvider.take()
        try {
            val options = webDriver.manage()
            for (entry in request.cookies) {
                val cookie = Cookie(entry.name, entry.value)
                options.addCookie(cookie)
            }
            webDriver[request.url]

            actionHandler?.handle(request, webDriver)

            val webElement = webDriver.findElement(By.xpath("/html"))
            val content = webElement.getAttribute("outerHTML")
            options.deleteAllCookies()
            val response = Response(
                    request.url,
                    content,
                    200,
                    contentLength = content.length.toLong(),
                    time = System.currentTimeMillis() - start
            )
            handler(Result.success(response))
        } catch (e: Exception) {
            handler(Result.failure(e))
        } finally {
            webDriverProvider.release(webDriver)
        }
    }
}
