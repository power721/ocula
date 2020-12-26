package cn.har01d.ocula.examples

import cn.har01d.ocula.handler.AuthHandler
import cn.har01d.ocula.handler.ConsoleLogResultHandler
import cn.har01d.ocula.handler.HtmlResultHandler
import cn.har01d.ocula.handler.TextFileResultHandler
import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.RequestBody
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.http.post
import cn.har01d.ocula.listener.LogListener
import cn.har01d.ocula.parser.AbstractParser
import cn.har01d.ocula.spider


fun main() = spider(QuotesParser(), "http://quotes.toscrape.com/tag/humor/") {
    config.authHandler = CsrfFormAuthHandler()
    listeners += LogListener
    resultHandlers += ConsoleLogResultHandler
    resultHandlers += TextFileResultHandler(System.getProperty("java.io.tmpdir") + "/quotes/text")
    resultHandlers += HtmlResultHandler(System.getProperty("java.io.tmpdir") + "/quotes/html")
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
        val next = response.href("li.next a")
        if (!context.follow(response.url, next)) {
            context.finish()
        }
        return quotes
    }
}

class CsrfFormAuthHandler : AuthHandler() {
    override fun handle(request: Request) {
        val url = "http://quotes.toscrape.com/login"
        val res = dispatch(Request(url))
        val token = res.select("input[name=csrf_token]").`val`()
        val formRequest = url.post(
            RequestBody.form(
                "csrf_token" to token,
                "username" to "user",
                "password" to "user"
            )
        )
        dispatch(formRequest)
    }
}

data class Quote(val author: String, val text: String)
