package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.lines
import com.har01d.ocula.parser.AbstractParser
import com.har01d.ocula.toDouble

fun main() {
    Spider(DoubanBookCrawler(), DoubanBookParser(), "https://book.douban.com/tag/%E7%A5%9E%E7%BB%8F%E7%BD%91%E7%BB%9C").run()
}

class DoubanBookCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        response.select("#subject_list .subject-item .info h2 a").forEach {
            spider.follow(response.url, it.attr("href"))
        }
        val next = response.select(".next a", "href")
        if (next.isNotEmpty()) {
            spider.crawl(response.url, next)
        } else {
            spider.finish()
        }
    }
}

class DoubanBookParser : AbstractParser<Book>() {
    override fun parse(request: Request, response: Response): Book {
        val name = response.select("h1 span").text()
        val score = response.select(".rating_num").first().toDouble()
        val info = response.select("#info").lines()
        var author = ""
        var publisher = ""
        var date = ""
        var isbn = ""
        var pages = 0
        for (line in info) {
            val pair = line.split(":")
            val tag = pair[0].trim()
            val value = pair[1].trim()
            when (tag) {
                "作者" -> author = value
                "出版年" -> date = value
                "出版社" -> publisher = value
                "ISBN" -> isbn = value
                "页数" -> pages = value.trim().toInt()
            }
        }
        return Book(name, author, publisher, date, isbn, pages, score)
    }
}

data class Book(var name: String, val author: String, val publisher: String, val date: String, val ISBN: String, val pages: Int, val score: Double?)
