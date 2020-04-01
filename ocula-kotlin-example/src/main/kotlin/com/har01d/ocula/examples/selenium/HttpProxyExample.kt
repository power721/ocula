package com.har01d.ocula.examples.selenium

import com.har01d.ocula.selenium.SimpleSeleniumSpider

fun main() {
    SimpleSeleniumSpider("https://www.google.com/ncr") { _, res ->
        res.select("div.fbar div span").text()
    }.apply {
        httpProxy("127.0.0.1", 1080)
    }.run()
}
