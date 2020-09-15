package cn.har01d.ocula.examples.charset

import cn.har01d.ocula.SimpleSpider
import cn.har01d.ocula.selector.toInt
import java.nio.charset.Charset

fun main() {
    SimpleSpider("http://top.baidu.com/buzz?b=341&fr=topbuzz_b341") { _, res ->
        val list = mutableListOf<Top>()
        res.select(".list-table tr").forEach {
            val keyword = it.select(".keyword")
            if (!keyword.isEmpty()) {
                val title = keyword.select("a").first().text()
                val url = keyword.select("a").first().attr("href")
                val search = it.select(".last").first().toInt() ?: 0
                list += Top(title, url, search)
            }
        }
        list
    }.apply { config.http.charset = Charset.forName("gb2312") }.run()
}

data class Top(val title: String, val url: String, val search: Int)
