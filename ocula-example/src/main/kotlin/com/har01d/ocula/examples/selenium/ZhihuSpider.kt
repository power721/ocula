package cn.har01d.ocula.examples.selenium

import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.parser.AbstractParser
import cn.har01d.ocula.selenium.LoadAll
import cn.har01d.ocula.selenium.SeleniumSpider
import cn.har01d.ocula.util.normalizeUrl

fun main() {
    SeleniumSpider(ZhihuParser(), "https://www.zhihu.com/search?type=content&q=Java") {
        actionHandler = LoadAll()
    }.run()
}

class ZhihuParser : AbstractParser<List<Question>>() {
    override fun parse(request: Request, response: Response): List<Question> {
        return response.select(".SearchResult-Card .List-item").map {
            val title = it.select(".ContentItem-title a").text()
            val url = normalizeUrl(response.url, it.select(".ContentItem-title a").attr("href"))
            val text = it.select(".VoteButton").text()
            val m = "(\\d+)".toRegex().find(text)
            if (m != null) {
                val vote = m.groupValues[1].toInt()
                Question(title, url, vote)
            } else {
                Question(title, url, 0)
            }
        }.also {
            finish()
            println(it.size)
        }
    }
}

data class Question(val title: String, val url: String?, val vote: Int)
