package cn.har01d.ocula.examples.json

import cn.har01d.ocula.Spider
import cn.har01d.ocula.http.OkHttp3Client

class OkHttpSpider : Spider<List<JsonQuote>>(QuotesJsonParser(), "http://quotes.toscrape.com/api/quotes") {
    init {
        httpClient = OkHttp3Client()
    }
}

fun main() {
    val spider = OkHttpSpider()
    spider.run()
}
