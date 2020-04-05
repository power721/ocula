package com.har01d.ocula.selenium

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.Crawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.Parser
import com.har01d.ocula.parser.SimpleParser

open class SeleniumSpider<T>(crawler: Crawler? = null, parser: Parser<T>, configure: SeleniumSpider<T>.() -> Unit = {}) : Spider<T>(crawler, parser) {
    var driverType: DriverType = DriverType.CHROME
    var headless: Boolean = true
    var phantomjsExecPath: String? = null
    var webDriverProvider: WebDriverProvider? = null
    var actionHandler: SeleniumActionHandler? = null

    constructor(parser: Parser<T>, vararg urls: String, configure: SeleniumSpider<T>.() -> Unit = {}) : this(null, parser, configure) {
        addUrl(*urls)
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg urls: String, configure: SeleniumSpider<T>.() -> Unit = {}) : this(crawler, parser, configure) {
        addUrl(*urls)
    }

    init {
        this.configure()
    }

    override fun initHttpClient() {
        if (concurrency == 0) {
            concurrency = if (http.proxies.size > 0) http.proxies.size else 1
        }
        webDriverProvider = webDriverProvider
                ?: DefaultWebDriverProvider(concurrency, http.proxyProvider!!, driverType, headless, phantomjsExecPath)
        val httpClient = SeleniumHttpClient(webDriverProvider!!)
        httpClient.actionHandler = actionHandler
        this.httpClient = httpClient
        super.initHttpClient()
    }

    override fun postHandle() {
        webDriverProvider!!.clean()
        super.postHandle()
    }

    fun phantomjs(execPath: String): SeleniumSpider<T> {
        driverType = DriverType.PHANTOMJS
        phantomjsExecPath = execPath
        return this
    }
}

class SimpleSeleniumSpider<T>(vararg url: String, parse: (request: Request, response: Response) -> T) : SeleniumSpider<T>(SimpleParser(parse), *url)

enum class DriverType {
    CHROME,
    EDGE,
    FIREFOX,
    OPERA,
    SAFARI,
    PHANTOMJS
}
