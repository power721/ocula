package cn.har01d.ocula.robots

import cn.har01d.ocula.handler.RobotsHandler
import cn.har01d.ocula.http.Request
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import crawlercommons.robots.SimpleRobotRules
import crawlercommons.robots.SimpleRobotRulesParser
import java.net.URL


class SimpleRobotsHandler : RobotsHandler {
    private val rules = mutableMapOf<String, SimpleRobotRules?>()

    override fun init(requests: List<Request>) {
        requests.map { URL(it.url).relative("/robots.txt") }.toSet().forEach {
            rules[URL(it).relative("/")] = parseRobotsTxt(it)
        }
    }

    private fun parseRobotsTxt(url: String): SimpleRobotRules? {
        val (_, response, result) = url.httpGet().response()
        when (result) {
            is Result.Success -> {
                val contentType = response["content-type"].firstOrNull() ?: "text/plain"
                return SimpleRobotRulesParser().parseContent(url, result.value, contentType, "")
            }
        }
        return null
    }

    override fun handle(request: Request): Boolean {
        val url = URL(request.url)
        val rule = rules[url.relative("/")]
        if (rule != null) {
            return rule.isAllowed(request.url)
        }
        return true
    }

    private fun URL.relative(path: String) = URL(this, path).toString()
}
