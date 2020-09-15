package cn.har01d.ocula.examples.redis

import cn.har01d.ocula.Spider
import cn.har01d.ocula.crawler.AbstractCrawler
import cn.har01d.ocula.examples.crawler.QQCourseParser
import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.RequestBody
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.redis.enableRedis
import cn.har01d.ocula.util.path

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
                follow(response, Request(url.path(), body = RequestBody.text("Hello!")))
            }
        }
    }
}
