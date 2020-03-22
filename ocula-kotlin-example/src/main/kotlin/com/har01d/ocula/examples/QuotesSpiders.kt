package com.har01d.ocula.examples

import com.har01d.ocula.*
import com.har01d.ocula.http.HttpMethod
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser


fun main() {
    val spider = Spider(QuotesParser(), "http://quotes.toscrape.com/tag/humor/").apply {
        preHandlers += CsrfFormAuthHandler()
        listeners += LogListener
        resultHandlers += LogResultHandler
        resultHandlers += FileResultHandler("/tmp/quotes.json")
        resultHandlers += HtmlResultHandler("/tmp/quotes/humor")
    }
    spider.run()
}

class QuotesParser : AbstractParser<List<Quote>>() {
    override fun parse(request: Request, response: Response): List<Quote> {
        val quotes = mutableListOf<Quote>()
        for (quote in response.select("div.quote")) {
            val author = quote.select("span small").text()
            val text = quote.select("span.text").text()
            quotes += Quote(author, text)
        }

        println(response.select("a[href=/logout]").text())
        val next = response.select("li.next a").attr("href")
        if (!spider.follow(request.url, next)) {
            spider.finish()
        }
        return quotes
    }
}

class CsrfFormAuthHandler : AuthHandler() {
    override fun handle(request: Request) {
        val url = "http://quotes.toscrape.com/login"
        val res = spider.downloader.dispatch(Request(url))
        val token = res.select("input[name=csrf_token]").`val`()
        val formRequest = Request(url, HttpMethod.POST, listOf(
                "csrf_token" to token,
                "username" to "user",
                "password" to "password"
        ), cookies = res.cookies.toMutableList())
        val response = spider.downloader.dispatch(formRequest)
        request.cookies += response.cookies
    }
}

data class Quote(val author: String, val text: String)
