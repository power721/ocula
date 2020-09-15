package cn.har01d.ocula.examples.simple

import cn.har01d.ocula.SimpleSpider
import cn.har01d.ocula.spider
import cn.har01d.ocula.util.normalizeUrl

fun main() {
    SimpleSpider("https://bing.com") { _, res ->
        normalizeUrl(res.url, res.select("#bgLink", "href"))
    }.run()

    quotes()
}

fun quotes() = spider("http://quotes.toscrape.com/random") { _, res ->
    res.xpath("//div[@class=quote]/span[@class=text]/text()", String::class.java)
}
