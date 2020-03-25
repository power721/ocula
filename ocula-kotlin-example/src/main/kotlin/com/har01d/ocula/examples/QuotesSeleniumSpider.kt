package com.har01d.ocula.examples

import com.har01d.ocula.selenium.SeleniumSpider
import com.har01d.ocula.selenium.WaitElementPresent

fun main() {
    SeleniumSpider(QuotesParser(), "http://quotes.toscrape.com/js/").apply {
        actionHandler = WaitElementPresent("div.quote")
        // phantomjs("/opt/phantomjs/bin/phantomjs")
    }.run()
}
