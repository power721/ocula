package com.har01d.ocula.selenium

import com.google.common.base.Function
import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.Crawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.parser.Parser
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.phantomjs.PhantomJSDriverService
import org.openqa.selenium.remote.DesiredCapabilities

class SeleniumSpider<T>(parser: Parser<T>) : Spider<T>(parser) {
    var driverType: DriverType = DriverType.CHROME
    var phantomjsExecPath: String? = null
    var expectedConditions: Function<WebDriver, *>? = null
    var timeoutInSeconds: Int = 10
    private val webDriver: WebDriver by lazy {
        driver()
    }

    constructor(parser: Parser<T>, vararg urls: String) : this(parser) {
        requests += urls.map { Request(it) }
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg urls: String) : this(parser) {
        this.crawler = crawler
        requests += urls.map { Request(it) }
    }

    override fun preHandle() {
        val httpClient = SeleniumHttpClient(webDriver, timeoutInSeconds)
        httpClient.expectedConditions = expectedConditions
        this.httpClient = httpClient
        super.preHandle()
    }

    override fun postHandle() {
        webDriver.quit()
        super.postHandle()
    }

    fun phantomjs(execPath: String) {
        driverType = DriverType.PHANTOMJS
        phantomjsExecPath = execPath
    }

    private fun driver(): WebDriver {
        val cap = when (driverType) {
            DriverType.CHROME -> DesiredCapabilities.chrome()
            DriverType.FIREFOX -> DesiredCapabilities.firefox()
            DriverType.PHANTOMJS -> DesiredCapabilities.phantomjs()
        }
        cap.isJavascriptEnabled = true

        if (driverType == DriverType.PHANTOMJS) {
            if (phantomjsExecPath == null) {
                throw IllegalStateException("phantomjsExecPath is required")
            }
            cap.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsExecPath)
        }

        val args = listOf("--web-security=false", "--ssl-protocol=any", "--ignore-ssl-errors=true")
        cap.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args)
        cap.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, arrayOf("--logLevel=INFO"))

        return when (driverType) {
            DriverType.CHROME -> ChromeDriver(cap)
            DriverType.FIREFOX -> FirefoxDriver(cap)
            DriverType.PHANTOMJS -> PhantomJSDriver(cap)
        }
    }
}

enum class DriverType {
    CHROME,
    FIREFOX,
    PHANTOMJS
}
