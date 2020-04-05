package com.har01d.ocula.examples.crawler

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import com.har01d.ocula.selector.Selector

fun main() {
    val url = "http://tieba.baidu.com/f/index/forumpark?cn=&ci=0&pcn=%E5%B0%8F%E8%AF%B4&pci=161&ct=&st=new&pn=1"
    val spider = Spider(TiebaCrawler(), TiebaParser(), url)
    spider.run()
}

class TiebaCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        response.select("#ba_list .ba_info").forEach {
            val name = it.select(".ba_name").text()
            val url = it.select("a").attr("href")
            follow(response, Request(url, extra = mutableMapOf("name" to name)))
        }

        crawl(response, response.select("a.next", "href"))
    }
}

class TiebaParser : AbstractParser<Tieba>() {
    override fun parse(request: Request, response: Response): Tieba {
        val doc = Selector(response.body.replace("<!--", "").replace("-->", ""))
        val name = request.extra["name"] as String
        val elements = doc.select("div.th_footer_l .red_text").text().split(" ")
        return Tieba(name, elements[2].toInt(), elements[0].toInt(), elements[1].toInt())
    }
}

data class Tieba(val name: String, val members: Int, val subjects: Int, val posts: Int)
