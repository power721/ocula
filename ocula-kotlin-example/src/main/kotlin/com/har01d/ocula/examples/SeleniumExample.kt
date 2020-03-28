package com.har01d.ocula.examples

import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.parser.AbstractParser
import com.har01d.ocula.selenium.SeleniumSpider

fun main() {
    SeleniumSpider(CourseCrawler(), CourseParser(), "https://www.icourse163.org/category/historiography")
            //.phantomjs("/opt/phantomjs/bin/phantomjs")
            .run()
}

class CourseCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        response.select(".m-course-list .u-clist .cnt .first-row a").forEach {
            follow(response, it.attr("href"))
        }

        val last = response.select(".ux-pager_itm a").last()
        if (last.className() == "th-bk-main") {
            spider.finish()
        } else {
            val current = response.select(".th-bk-main").text().toInt()
            crawl(response, "?pageIndex=" + (current + 1))
        }
    }
}

class CourseParser : AbstractParser<Course>() {
    override fun parse(request: Request, response: Response): Course {
        val title = response.select(".course-title").text()
        val category = response.select(".sub-category").text()
        return Course(title, category)
    }
}

data class Course(val title: String, val category: String)
