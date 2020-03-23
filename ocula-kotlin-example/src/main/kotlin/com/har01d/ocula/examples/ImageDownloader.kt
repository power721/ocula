package com.har01d.ocula.examples

import com.har01d.ocula.SimpleSpider

fun main() {
    SimpleSpider("https://book.douban.com") { _, res ->
        res.select("img[src]").map { it.attr("src") }
    }.downloadImages("/tmp/images")
}
