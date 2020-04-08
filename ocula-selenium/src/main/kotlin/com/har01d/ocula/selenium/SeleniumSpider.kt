package com.har01d.ocula.selenium

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.Crawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.Parser
import com.har01d.ocula.parser.SimpleParser

typealias Configure<T> = SeleniumSpider<T>.() -> Unit

open class SeleniumSpider<T>(crawler: Crawler? = null, parser: Parser<T>, configure: Configure<T> = {}) :
    Spider<T>(crawler, parser) {
    var driverType: DriverType = DriverType.CHROME
    var headless: Boolean = true
    var phantomjsExecPath: String? = null
    var webDriverProvider: WebDriverProvider? = null
    var actionHandler: SeleniumActionHandler? = null

    constructor(parser: Parser<T>, vararg url: String, configure: Configure<T> = {})
            : this(null, parser, configure) {
        addUrl(*url)
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg url: String, configure: Configure<T> = {})
            : this(crawler, parser, configure) {
        addUrl(*url)
    }

    constructor(parser: Parser<T>, vararg request: Request, configure: Configure<T> = {})
            : this(null, parser, configure) {
        addRequest(*request)
    }

    constructor(crawler: Crawler, parser: Parser<T>, vararg request: Request, configure: Configure<T> = {})
            : this(crawler, parser, configure) {
        addRequest(*request)
    }

    init {
        this.configure()
    }

    override fun initHttpClient() {
        if (config.parser.concurrency == 0) {
            config.parser.concurrency = if (config.http.proxies.size > 0) config.http.proxies.size else 1
        }
        webDriverProvider = webDriverProvider
            ?: DefaultWebDriverProvider(
                config.parser.concurrency,
                config.http.proxyProvider!!,
                driverType,
                headless,
                phantomjsExecPath
            )
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

class SimpleSeleniumSpider<T>(vararg url: String, parse: (request: Request, response: Response) -> T) :
    SeleniumSpider<T>(SimpleParser(parse), *url)

enum class DriverType {
    CHROME,
    EDGE,
    FIREFOX,
    OPERA,
    SAFARI,
    PHANTOMJS
}
