package com.har01d.ocula.examples

import com.har01d.ocula.SimpleSpider
import com.har01d.ocula.extension.JueJinImageUploader
import com.har01d.ocula.extension.NeteaseImageUploader
import com.har01d.ocula.extension.SouHuImageUploader
import com.har01d.ocula.extension.TouTiaoImageUploader
import com.har01d.ocula.handler.ResultHandler
import com.har01d.ocula.http.Request
import com.har01d.ocula.http.Response
import com.har01d.ocula.selector.get
import com.har01d.ocula.util.normalizeUrl

fun main() {
    SimpleSpider("https://book.douban.com") { _, res ->
        res.select("img[src]").map { it["src"] }
    }.downloadImages(System.getProperty("java.io.tmpdir") + "/images")

    SimpleSpider("https://cn.bing.com") { _, res ->
        normalizeUrl(res.url, res.select("#bgLink", "href")) ?: ""
    }.apply {
        resultHandlers += ImageHandler()
    }.run()
}

class ImageHandler : ResultHandler<String> {
    override fun handle(request: Request, response: Response, result: String) {
        println(TouTiaoImageUploader.upload(result))
        println(JueJinImageUploader.upload(result))
        println(SouHuImageUploader.upload(result))
        println(NeteaseImageUploader.upload(result))
    }
}
