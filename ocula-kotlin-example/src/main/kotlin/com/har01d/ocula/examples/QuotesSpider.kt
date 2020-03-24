package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.handler.AuthHandler
import com.har01d.ocula.handler.ConsoleLogResultHandler
import com.har01d.ocula.handler.HtmlResultHandler
import com.har01d.ocula.handler.TextFileResultHandler
import com.har01d.ocula.http.HttpMethod
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.listener.LogListener
import com.har01d.ocula.parser.AbstractParser


fun main() {
    val spider = Spider(QuotesParser(), "http://quotes.toscrape.com/tag/humor/").apply {
        authHandler = CsrfFormAuthHandler()
        listeners += LogListener
        resultHandlers += ConsoleLogResultHandler
        resultHandlers += TextFileResultHandler(System.getProperty("java.io.tmpdir") + "/quotes/text")
        resultHandlers += HtmlResultHandler(System.getProperty("java.io.tmpdir") + "/quotes/html")
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
        val next = response.select("li.next a", "href")
        if (!spider.follow(response.url, next)) {
            spider.finish()
        }
        return quotes
    }
}

class CsrfFormAuthHandler : AuthHandler() {
    override fun handle(request: Request) {
        val url = "http://quotes.toscrape.com/login"
        val res = spider.dispatch(Request(url))
        val token = res.select("input[name=csrf_token]").`val`()
        val formRequest = Request(url, HttpMethod.POST, listOf(
                "csrf_token" to token,
                "username" to "user",
                "password" to "user"
        ), cookies = res.cookies.toMutableList(), allowRedirects = false)
        spider.dispatch(formRequest).apply {
            request.cookies += cookies
        }
    }
}

data class Quote(val author: String, val text: String)
