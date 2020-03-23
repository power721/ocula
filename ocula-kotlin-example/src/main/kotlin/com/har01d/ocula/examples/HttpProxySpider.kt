package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import com.har01d.ocula.selenium.SeleniumSpider

fun main() {
    Spider(GoogleParser, "https://www.google.com/ncr").apply {
        httpProxy("127.0.0.1", 1080)
    }.run()

    SeleniumSpider(GoogleParser, "https://www.google.com/ncr").apply {
        httpProxy("127.0.0.1", 1080)
    }.run()
}

object GoogleParser : AbstractParser<String>() {
    override fun parse(request: Request, response: Response): String {
        val text = response.select("div.fbar div span").first().text()
        spider.finish()
        return text
    }
}