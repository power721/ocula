package com.har01d.ocula.examples.selenium

import com.har01d.ocula.examples.GoogleParser
import com.har01d.ocula.selenium.SeleniumSpider

fun main() {
    SeleniumSpider(GoogleParser, "https://www.google.com/ncr").apply {
        httpProxy("127.0.0.1", 1080)
    }.run()
}
