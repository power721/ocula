package cn.har01d.ocula.examples.json

import cn.har01d.ocula.Spider
import cn.har01d.ocula.handler.JsonFileResultHandler
import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.parser.AbstractParser
import com.jayway.jsonpath.TypeRef

fun main() {
    val spider = Spider(QuotesJsonParser(), "http://quotes.toscrape.com/api/quotes")
    spider.resultHandlers += JsonFileResultHandler(System.getProperty("java.io.tmpdir") + "/quotes/json")
    spider.run()
}

class QuotesJsonParser : AbstractParser<List<JsonQuote>>() {
    override fun parse(request: Request, response: Response): List<JsonQuote> {
        val typeRef: TypeRef<List<JsonQuote>> = object : TypeRef<List<JsonQuote>>() {}
        val quotes: List<JsonQuote> = response.jsonPath("$.quotes", typeRef)

        val hasNext: Boolean = response.jsonPath("$.has_next")
        if (hasNext) {
            val page: Int = response.jsonPath("$.page", Int::class.java)
            val next = "http://quotes.toscrape.com/api/quotes?page=" + (page + 1)
            follow(response, next)
        } else {
            finish()
        }
        return quotes
    }
}

data class Author(val name: String, val slug: String)
data class JsonQuote(val author: Author, val text: String)
