package com.har01d.ocula.examples.redis

import com.har01d.ocula.Spider
import com.har01d.ocula.crawler.AbstractCrawler
import com.har01d.ocula.examples.crawler.QQCourseParser
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.redis.enableRedis
import com.har01d.ocula.util.path

fun main() {
    Spider(QQCourseCrawler(), QQCourseParser(), "https://ke.qq.com/course/list") {
        enableRedis("qq")
    }.run()
}

class QQCourseCrawler : AbstractCrawler() {
    override fun handle(request: Request, response: Response) {
        response.links().forEach { url ->
            if (url.matches("/course/list\\?mt=\\d+".toRegex())) {
                crawl(response, url)
            } else if (url.matches("(https:)?//ke.qq.com/course/\\d+.*".toRegex())) {
                follow(response, url.path())
            }
        }
    }
}
