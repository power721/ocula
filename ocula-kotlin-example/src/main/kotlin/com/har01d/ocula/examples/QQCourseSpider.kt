package com.har01d.ocula.examples

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import com.har01d.ocula.util.path

fun main() {
    Spider(QQCourseCrawler(), QQCourseParser(), "https://ke.qq.com/course/list").run()
}

class QQCourseCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        println("handle " + response.url)
        response.select("a[href]").forEach {
            val url = it.attr("href")
            if (url.matches("(https:)?//ke.qq.com/course/list.*".toRegex()) || url.startsWith("/course/list")) {
                crawl(url)
            } else if (url.matches("(https:)?//ke.qq.com/course/\\d+.*".toRegex())) {
                follow(response, url.path())
            }
        }
    }
}

class QQCourseParser : AbstractParser<QQCourse>() {
    override fun parse(request: Request, response: Response): QQCourse {
        val name = response.select(".item--tt").text()
        val school = response.select(".js-agency-name").text()
        val free = "免费" == response.select(".title-free").text()
        return QQCourse(name, response.url, school, free)
    }
}

data class QQCourse(val name: String, val url: String, val school: String, val free: Boolean)
