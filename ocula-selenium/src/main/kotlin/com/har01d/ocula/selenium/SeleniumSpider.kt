package com.har01d.ocula.selenium

import com.google.common.base.Function
import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.Crawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.Parser
import com.har01d.ocula.parser.SimpleParser
import org.openqa.selenium.WebDriver

open class SeleniumSpider<T>(parser: Parser<T>) : Spider<T>(parser) {
    var driverType: DriverType = DriverType.CHROME
    var phantomjsExecPath: String? = null
    var expectedConditions: Function<WebDriver, *>? = null
    var seleniumAction: WebDriver.() -> Unit = {}
    var webDriverProvider: WebDriverProvider? = null
    var timeoutInSeconds: Int = 10

    constructor(parser: Parser<T>, vararg urls: String) : this(parser) {
        requests += urls.map { Request(it) }
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg urls: String) : this(parser) {
        this.crawler = crawler
        requests += urls.map { Request(it) }
    }

    override fun preHandle() {
        val size = if (httpProxies.size > 0) httpProxies.size else 1
        webDriverProvider = webDriverProvider
                ?: DefaultWebDriverProvider(size, proxyProvider!!, driverType, phantomjsExecPath)
        val httpClient = SeleniumHttpClient(webDriverProvider!!, timeoutInSeconds)
        httpClient.expectedConditions = expectedConditions
        httpClient.seleniumAction = seleniumAction
        this.httpClient = httpClient
        super.preHandle()
    }

    override fun postHandle() {
        webDriverProvider!!.clean()
        super.postHandle()
    }

    fun phantomjs(execPath: String) {
        driverType = DriverType.PHANTOMJS
        phantomjsExecPath = execPath
    }
}

class SimpleSeleniumSpider<T>(vararg url: String, parse: (request: Request, response: Response) -> T) : SeleniumSpider<T>(SimpleParser(parse), *url)

enum class DriverType {
    CHROME,
    FIREFOX,
    PHANTOMJS
}
