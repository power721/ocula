package cn.har01d.ocula.examples.selenium

import cn.har01d.ocula.examples.QuotesParser
import cn.har01d.ocula.selenium.SeleniumSpider
import cn.har01d.ocula.selenium.WaitElementPresent

fun main() {
    // System.setProperty("webdriver.gecko.driver", "/home/harold/Downloads/firefox/geckodriver")
    SeleniumSpider(QuotesParser(), "http://quotes.toscrape.com/js/") {
        actionHandler = WaitElementPresent("div.quote")
        // driverType = DriverType.FIREFOX
        // phantomjs("/opt/phantomjs/bin/phantomjs")
    }.run()
}
