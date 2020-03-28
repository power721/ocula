package com.har01d.ocula.selenium

import com.har01d.ocula.http.*
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset

class SeleniumHttpClient(private val webDriverProvider: WebDriverProvider) : HttpClient {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(SeleniumHttpClient::class.java)
    }

    override lateinit var userAgentProvider: UserAgentProvider
    override lateinit var proxyProvider: ProxyProvider
    override lateinit var charset: Charset

    var actionHandler: SeleniumActionHandler? = null

    override fun dispatch(request: Request): Response {
        val webDriver = webDriverProvider.take()
        logger.debug("[Request] handle ${request.url}")
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
                    contentLength = content.length.toLong()
            )
        } finally {
            webDriverProvider.release(webDriver)
        }
    }
}
