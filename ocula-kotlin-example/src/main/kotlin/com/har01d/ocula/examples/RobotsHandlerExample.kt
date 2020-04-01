package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.handler.DefaultRobotsHandler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser

fun main() {
    Spider(PageParser(), "https://cn.bing.com/").apply {
        robotsHandler = DefaultRobotsHandler()
    }.run()
}

class PageParser : AbstractParser<String>() {
    override fun parse(request: Request, response: Response): String {
        response.select("a[href]").forEach {
            val url = it.attr("href")
            if (url.startsWith("/") || url.startsWith("https://cn.bing.com/")) {
                follow(url)
            }
        }
        context.finish()
        return response.select("title").text()
    }
}
