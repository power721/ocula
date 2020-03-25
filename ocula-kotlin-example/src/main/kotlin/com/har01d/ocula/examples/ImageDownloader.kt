package com.har01d.ocula.examples

import com.har01d.ocula.SimpleSpider
import com.har01d.ocula.selector.get

fun main() {
    SimpleSpider("https://book.douban.com") { _, res ->
        res.select("img[src]").map { it["src"] }
    }.downloadImages(System.getProperty("java.io.tmpdir") + "/images")
}
