package com.har01d.ocula.examples

import com.har01d.ocula.selenium.SeleniumSpider
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions

fun main() {
    val spider = SeleniumSpider(QuotesParser(), "http://quotes.toscrape.com/js/").apply {
        expectedConditions = ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.quote"))
        // phantomjs("/opt/phantomjs/bin/phantomjs")
    }
    spider.run()
}
