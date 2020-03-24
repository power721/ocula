package com.har01d.ocula.examples

import com.har01d.ocula.SimpleSpider
import com.har01d.ocula.util.normalizeUrl

fun main() {
    SimpleSpider("https://bing.com") { _, res ->
        normalizeUrl(res.url, res.select("#bgLink", "href"))
    }.run()

    SimpleSpider("http://quotes.toscrape.com/random") { _, res ->
        res.xpath("//div[@class=quote]/span[@class=text]/text()", String::class.java)
    }.run()
}
