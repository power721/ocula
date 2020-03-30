package com.har01d.ocula.examples.selenium

import com.har01d.ocula.selenium.LoadAll
import com.har01d.ocula.selenium.SimpleSeleniumSpider
import com.har01d.ocula.util.normalizeUrl

fun main() {
    SimpleSeleniumSpider("https://www.zhihu.com/search?type=content&q=Java") { _, res ->
        res.select(".SearchResult-Card .List-item").map {
            val title = it.select(".ContentItem-title a").text()
            val url = normalizeUrl(res.url, it.select(".ContentItem-title a").attr("href"))
            val text = it.select(".VoteButton").text()
            val m = "(\\d+)".toRegex().find(text)
            if (m != null) {
                val vote = m.groupValues[1].toInt()
                Question(title, url, vote)
            } else {
                Question(title, url, 0)
            }
        }.also {
            println(it.size)
        }
    }.apply {
        actionHandler = LoadAll()
    }.run()
}

data class Question(val title: String, val url: String?, val vote: Int)
