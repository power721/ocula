package com.har01d.ocula.examples.extension

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import com.har01d.ocula.robots.SimpleRobotsHandler

fun main() {
    Spider(PageParser(), "https://cn.bing.com/") {
        http.robotsHandler = SimpleRobotsHandler()
    }.run()
}

class PageParser : AbstractParser<String>() {
    override fun parse(request: Request, response: Response): String {
        response.select("a[href]").forEach {
            val url = it.attr("href")
            if (!url.contains("/search")) {
                if (url.startsWith("/") || url.startsWith("https://cn.bing.com/")) {
                    follow(response, url)
                }
            }
        }
        finish()
        return response.select("title").text()
    }
}
