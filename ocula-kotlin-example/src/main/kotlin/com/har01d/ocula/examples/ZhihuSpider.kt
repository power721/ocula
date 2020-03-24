package com.har01d.ocula.examples

import com.har01d.ocula.selenium.SimpleSeleniumSpider
import com.har01d.ocula.util.normalizeUrl
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver

fun main() {
    val loadAll: WebDriver.() -> Unit = {
        val executor = this as JavascriptExecutor
        var height = executor.executeScript("return document.body.scrollHeight")
        while (true) {
            executor.executeScript("window.scrollTo(0, document.body.scrollHeight)")
            Thread.sleep(1000L)
            val newHeight = executor.executeScript("return document.body.scrollHeight")
            if (newHeight == height) {
                break
            }
            height = newHeight
        }
    }

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
        seleniumAction = loadAll
    }.run()
}

data class Question(val title: String, val url: String?, val vote: Int)
