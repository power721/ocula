package cn.har01d.ocula.examples.image

import cn.har01d.ocula.SimpleSpider
import cn.har01d.ocula.handler.ResultHandler
import cn.har01d.ocula.http.Request
import cn.har01d.ocula.http.Response
import cn.har01d.ocula.image.*
import cn.har01d.ocula.util.normalizeUrl

fun main() {
    SimpleSpider("https://book.douban.com") { _, res ->
        res.images()
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
