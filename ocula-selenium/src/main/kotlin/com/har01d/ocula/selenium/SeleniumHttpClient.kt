package com.har01d.ocula.selenium

import com.google.common.base.Function
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.ProxyProvider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SeleniumHttpClient(private val webDriver: WebDriver, timeoutInSeconds: Int) : HttpClient {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(SeleniumHttpClient::class.java)
    }

    override lateinit var userAgents: List<String>
    override lateinit var proxyProvider: ProxyProvider

    var expectedConditions: Function<WebDriver, *>? = null
    private val wait = WebDriverWait(webDriver, timeoutInSeconds.toLong())

    override fun dispatch(request: Request): Response {
        // TODO: lock webDriver
        logger.debug("[Request] handle ${request.url}")
        val options = webDriver.manage()
        for (entry in request.cookies) {
            val cookie = Cookie(entry.name, entry.value)
            options.addCookie(cookie)
        }
        webDriver[request.url]

        if (expectedConditions != null) {
            wait.until(expectedConditions)
        }

        val webElement = webDriver.findElement(By.xpath("/html"))
        val content = webElement.getAttribute("outerHTML")
        options.deleteAllCookies()
        return Response(
                request.url,
                content,
                200,
                contentLength = content.length.toLong()
        )
    }
}
