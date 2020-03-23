package com.har01d.ocula.examples

import com.har01d.ocula.SimpleSpider

fun main() {
    SimpleSpider("https://bing.com") { _, res ->
        res.url + res.select("#bgLink").attr("href")
    }.run()
}
