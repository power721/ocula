package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import org.jsoup.Jsoup

fun main() {
    val spider = Spider(TiebaParser(), "http://tieba.baidu.com/f/index/forumpark?cn=&ci=0&pcn=%E5%B0%8F%E8%AF%B4&pci=161&ct=&st=new&pn=1")
    spider.concurrency = Runtime.getRuntime().availableProcessors()
    spider.crawler = TiebaCrawler()
    spider.run()
}

class TiebaCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        val elements = response.select("#ba_list .ba_info")
        for (element in elements) {
            spider.follow(request.url, element.select("a").first().attr("href"))
        }

        val next = response.select("a.next").attr("href")
        if (next.isNotEmpty()) {
            spider.crawl(request.url, next)
        } else {
            spider.finish()
        }
    }
}

class TiebaParser : AbstractParser<Tieba?>() {
    override fun parse(request: Request, response: Response): Tieba? {
        val doc = Jsoup.parse(response.body.replace("<!--", "").replace("-->", ""))
        val name = doc.select("a.card_title_fname").text()
        val text = doc.select("div.th_footer_l").text()
        val m = "共有主题数(\\d+)个，贴子数 (\\d+)篇 .*数(\\d+)".toRegex().find(text)
        return if (m != null) {
            Tieba(name, m.groupValues[3].toInt(), m.groupValues[1].toInt(), m.groupValues[2].toInt())
        } else {
            null
        }
    }
}

data class Tieba(val name: String, val members: Int, val subjects: Int, val posts: Int)
