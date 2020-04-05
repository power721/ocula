package com.har01d.ocula.examples

import com.har01d.ocula.SimpleSpider

fun main() {
    SimpleSpider("https://www.google.com/ncr") { _, res ->
        res.select("div.fbar div span").text()
    }.apply {
        httpProxy("127.0.0.1", 1080)
    }.run()
}
