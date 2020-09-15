package cn.har01d.ocula

import cn.har01d.ocula.handler.AuthHandler
import cn.har01d.ocula.handler.NoopRobotsHandler
import cn.har01d.ocula.handler.RobotsHandler
import cn.har01d.ocula.http.HttpProxy
import cn.har01d.ocula.http.ProxyProvider
import cn.har01d.ocula.http.UserAgentProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import java.nio.charset.Charset
import java.util.*

open class Config {
    companion object {
        init {
            Configuration.setDefaults(object : Configuration.Defaults {
                private val mapper = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

                override fun jsonProvider() = JacksonJsonProvider(mapper)
                override fun mappingProvider() = JacksonMappingProvider(mapper)
                override fun options() = EnumSet.noneOf(Option::class.java)
            })
        }
    }

    val http = HttpConfig()
    val crawler = CrawlerConfig()
    val parser = ParserConfig()
    var interval = 500L
    var completeOnIdleTime = 300
    var authHandler: AuthHandler? = null

    fun http(block: HttpConfig.() -> Unit) {
        with(http) {
            block()
        }
    }

    fun crawler(block: CrawlerConfig.() -> Unit) {
        with(crawler) {
            block()
        }
    }

    fun parser(block: ParserConfig.() -> Unit) {
        with(parser) {
            block()
        }
    }

    class HttpConfig {
        private val defaultUserAgents = listOf(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:42.0) Gecko/20100101 Firefox/42.0",
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.1b3) Gecko/20090305 Firefox/3.1b3 GTB5",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0",
            "Mozilla/5.0 (IE 11.0; Windows NT 6.3; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko",
            "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Win64; x64; Trident/6.0)",
            "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 QIHU 360SE",
            "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17 QIHU 360EE",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 QIHU 360SE",
            "Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.7 (KHTML, like Gecko) Ubuntu/11.10 Chromium/16.0.912.21 Chrome/16.0.912.21 Safari/535.7",
            "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh_CN) AppleWebKit/534.7 (KHTML, like Gecko) Chrome/7.0 baidubrowser/1.x Safari/534.7",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/34.0.1847.116 Chrome/34.0.1847.116 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.38 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:58.0) Gecko/20100101 Firefox/58.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36 Edge/14.14359",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.80 Safari/537.36 QQBrowser/9.3.6874.400",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; Xbox; Xbox One) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36 Edge/14.14359",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36"
        )

        private val mobileUserAgents = listOf(
            "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1",
            "Mozilla/5.0 (Linux; Android 8.0.0; Pixel 2 XL Build/OPD1.170816.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; U; Android 4.2; en-us; Nexus 10 Build/JVP15I) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30",
            "Mozilla/5.0 (Android 4.2.2; Tablet; rv:47.0) Gecko/47.0 Firefox/47.0",
            "Mozilla/5.0 (Linux; Android 9; LYA-AL00 Build/HUAWEILYA-AL00L) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.111 HuaweiBrowser/9.0.2.305 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; U; Android 4.1.1; zh-cn; MI 2SC Build/JRO03L) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.36 XiaoMi/MiuiBrowser/2.1.1",
            "Mozilla/5.0 (iPhone 5; CPU iPhone OS 7_0_6 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/6.0 MQQBrowser/5.0.5 Mobile/11B651 Safari/8536.25",
            "Mozilla/5.0 (Linux; U; Android 6.0; zh-CN; PE-TL20 Build/HuaweiPE-TL20) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 Quark/2.0.3.954 Mobile Safari/537.36"
        )

        private val defaultHttpHeaders = mapOf(
            "Accept" to listOf("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"),
            // "Accept-Encoding" to listOf("gzip", "deflate"),  // do not support br
            "Accept-Language" to listOf("en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7,ja;q=0.6,zh-TW;q=0.5")
        )

        fun mobile() {
            userAgents = mobileUserAgents
        }

        var base = ""
        var charset: Charset = Charsets.UTF_8
        var timeout = 15000
        var timeoutRead = 15000

        var userAgents: List<String> = defaultUserAgents
        var userAgentProvider: UserAgentProvider? = null
        var headers: Map<String, Collection<String>> = defaultHttpHeaders
        val proxies = mutableListOf<HttpProxy>()
        var proxyProvider: ProxyProvider? = null
        var robotsHandler: RobotsHandler = NoopRobotsHandler
    }

    class CrawlerConfig {
        var abortOnError = false
        var concurrency: Int = 1
    }

    class ParserConfig {
        var abortOnError = false
        var concurrency: Int = 0
    }
}
