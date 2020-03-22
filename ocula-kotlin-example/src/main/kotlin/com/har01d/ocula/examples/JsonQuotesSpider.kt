package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser

fun main() {
    val spider = Spider(JsonQuotesParser(), "http://quotes.toscrape.com/api/quotes")
    spider.run()
}

class JsonQuotesParser : AbstractParser<List<Quote>>() {
    override fun parse(request: Request, response: Response): List<Quote> {
        val quotes = mutableListOf<Quote>()
        val list: List<Map<String, Any>> = response.jsonPath("$.quotes")
        for (item in list) {
            quotes += Quote((item["author"] as Map<*, *>)["name"] as String, item["text"] as String)
        }

        val hasNext: Boolean = response.jsonPath("$.has_next")
        if (hasNext) {
            val page: Int = response.jsonPath("$.page")
            val next = "http://quotes.toscrape.com/api/quotes?page=" + (page + 1)
            spider.follow(request.url, next)
        } else {
            spider.finish()
        }
        return quotes
    }
}
