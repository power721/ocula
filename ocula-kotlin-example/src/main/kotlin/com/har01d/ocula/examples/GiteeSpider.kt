package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import com.har01d.ocula.selector.get

fun main() {
    Spider(GiteeCrawler(), GiteeParser(), "https://gitee.com/explore/spider?lang=Java").run()
    Spider(GiteeSearchCrawler(), GiteeParser(), "https://search.gitee.com/?type=repository&q=%E7%88%AC%E8%99%AB%E6%A1%86%E6%9E%B6&lang=java").run()
}

class GiteeCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        response.select(".items .item a.title").forEach {
            follow(response, it["href"])
        }

        crawl(response.select("a[rel=next]", "href"))
    }
}

class GiteeSearchCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        response.select("#hits-list .item .header .title a").forEach {
            follow(response, it["href"])
        }

        crawl(response.select(".next a", "href"))
    }
}

class GiteeParser : AbstractParser<Repo>() {
    override fun parse(request: Request, response: Response): Repo {
        val name = response.select(".project-title a.repository").text()
        val author = response.select(".project-title a.author").text()
        val language = response.select(".project-badges a.proj-language").text()
        val license = response.select(".project-badges a.license").text()
        val watched = response.select(".watch-container a.action-social-count", "title").toInt()
        val stared = response.select(".star-container a.action-social-count", "title").toInt()
        val forked = response.select(".fork-container a.action-social-count", "title").toInt()
        return Repo(name, response.url, author, language, license, watched, stared, forked)
    }
}

data class Repo(val name: String, val url: String, val author: String, val language: String, val license: String, val watched: Int, val stared: Int, val forked: Int)
