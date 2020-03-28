package com.har01d.ocula.examples

import com.har01d.ocula.selenium.SeleniumSpider
import com.har01d.ocula.selenium.WaitElementPresent

fun main() {
    // System.setProperty("webdriver.gecko.driver", "/home/harold/Downloads/firefox/geckodriver")
    SeleniumSpider(QuotesParser(), "http://quotes.toscrape.com/js/").apply {
        actionHandler = WaitElementPresent("div.quote")
        // driverType = DriverType.FIREFOX
        // phantomjs("/opt/phantomjs/bin/phantomjs")
    }.run()
}
