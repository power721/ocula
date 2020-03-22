package com.har01d.ocula.examples

import com.github.kittinunf.fuel.httpPost
import com.har01d.ocula.*
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import java.net.HttpCookie


fun main() {
    val spider = Spider(QuotesParser(), "http://quotes.toscrape.com/tag/humor/").apply {
        preHandlers += AuthHandler()
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

class AuthHandler : AbstractPreHandler() {
    override fun handle(request: Request) {
        val res = spider.downloader.download(Request("http://quotes.toscrape.com/login"))
        val token = res.select("input[name=csrf_token]").`val`()
        val (req, response, result) = "http://quotes.toscrape.com/login"
                .httpPost(
                        listOf(
                                "csrf_token" to token,
                                "username" to "user",
                                "password" to "password"
                        )
                )
                .header("Cookie", res.cookies.joinToString("&"))
                .header("Referer", "http://quotes.toscrape.com/login")
                .responseString()
        response.headers["Set-Cookie"]
                ?.flatMap { HttpCookie.parse(it) }
                .also { println(it) }
                .find { it.name == "session" }
                ?.let {
                    request.headers["Cookie"] = listOf("session=" + it.value)
                    println("login")
                }
    }
}

data class Quote(val author: String, val text: String)
