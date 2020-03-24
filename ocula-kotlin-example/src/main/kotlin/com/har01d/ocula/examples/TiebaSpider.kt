package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import org.jsoup.Jsoup

fun main() {
    val url = "http://tieba.baidu.com/f/index/forumpark?cn=&ci=0&pcn=%E5%B0%8F%E8%AF%B4&pci=161&ct=&st=new&pn=1"
    val spider = Spider(TiebaCrawler(), TiebaParser(), url)
    spider.run()
}

class TiebaCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        val elements = response.select("#ba_list .ba_info")
        for (element in elements) {
            val name = element.select(".ba_name").text()
            val url = element.select("a").first().attr("href")
            spider.follow(response.url, Request(url, extra = mutableMapOf("name" to name)))
        }

        val next = response.select("a.next", "href")
        if (next.isNotEmpty()) {
            spider.crawl(response.url, next)
        } else {
            spider.finish()
        }
    }
}

class TiebaParser : AbstractParser<Tieba>() {
    override fun parse(request: Request, response: Response): Tieba {
        val doc = Jsoup.parse(response.body.replace("<!--", "").replace("-->", ""))
        val name = request.extra["name"] as String
        // val name = doc.select("a.card_title_fname").text()
        val elements = doc.select("div.th_footer_l .red_text").text().split(" ")
        return Tieba(name, elements[2].toInt(), elements[0].toInt(), elements[1].toInt())
    }
}

data class Tieba(val name: String, val members: Int, val subjects: Int, val posts: Int)
