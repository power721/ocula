package com.har01d.ocula.selenium

import com.google.common.base.Function
import com.har01d.ocula.http.HttpClient
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SeleniumHttpClient(private val webDriver: WebDriver, timeoutInSeconds: Long) : HttpClient {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(SeleniumHttpClient::class.java)
    }

    var expectedConditions: Function<WebDriver, *>? = null
    override var userAgents = listOf<String>()
    private val wait = WebDriverWait(webDriver, timeoutInSeconds)

    override fun dispatch(request: Request): Response {
        // TODO: lock webDriver
        logger.debug("[Request] handle ${request.url}")
        webDriver[request.url]
        val options = webDriver.manage()
        for (entry in request.cookies) {
            val cookie = Cookie(entry.name, entry.value)
            options.addCookie(cookie)
        }

        if (expectedConditions != null) {
            wait.until(expectedConditions)
        }

        val webElement = webDriver.findElement(By.xpath("/html"))
        val content = webElement.getAttribute("outerHTML")
        return Response(
                request.url,
                content,
                200,
                contentLength = content.length.toLong()
        )
    }
}
