package com.har01d.ocula.extension

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpUpload
import com.github.kittinunf.result.Result
import com.jayway.jsonpath.JsonPath
import com.qiniu.storage.Configuration
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth
import java.io.File


interface ImageUploader {
    fun upload(imageUrl: String): String
}

abstract class AbstractImageUploader : ImageUploader {
    fun download(url: String): File {
        val file = File.createTempFile("image", ".jpg")
        url.httpDownload().fileDestination { _, _ ->
            file
        }.response()
        return file
    }
}

object TouTiaoImageUploader : AbstractImageUploader() {
    private const val api = "https://mp.toutiao.com/upload_photo/?type=json"
    override fun upload(imageUrl: String): String {
        val imageFile = download(imageUrl)
        val (_, _, result) = api.httpUpload()
                .add { FileDataPart(imageFile, name = "photo") }
                .responseString()
        when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> return JsonPath.parse(result.get()).read("$.web_url")
        }
    }
}

object JueJinImageUploader : AbstractImageUploader() {
    private const val api = "https://cdn-ms.juejin.im/v1/upload?bucket=gold-user-assets"
    override fun upload(imageUrl: String): String {
        val imageFile = download(imageUrl)
        val (_, _, result) = api.httpUpload()
                .add { FileDataPart(imageFile, name = "file") }
                .responseString()
        when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> {
                return JsonPath.parse(result.get()).read("$.d.url.https")
            }
        }
    }
}

object SouHuImageUploader : AbstractImageUploader() {
    private const val api = "http://changyan.sohu.com/api/2/comment/attachment"
    override fun upload(imageUrl: String): String {
        val imageFile = download(imageUrl)
        val (_, _, result) = api.httpUpload()
                .add { FileDataPart(imageFile, name = "file") }
                .responseString()
        when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> return JsonPath.parse(result.get()).read("$.url")
        }
    }
}

object NeteaseImageUploader : AbstractImageUploader() {
    private const val api = "http://you.163.com/xhr/file/upload.json"
    override fun upload(imageUrl: String): String {
        val imageFile = download(imageUrl)
        val (_, _, result) = api.httpUpload()
                .add { FileDataPart(imageFile, name = "file") }
                .responseString()
        when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> return JsonPath.parse(result.get()).read("$.data[0]")
        }
    }
}

class QiniuImageUploader(private val accessKey: String, private val secretKey: String, private val bucket: String, private val server: String) : AbstractImageUploader() {
    private val uploadManager = UploadManager(Configuration())

    override fun upload(imageUrl: String): String {
        val imageFile = download(imageUrl)
        return upload(imageFile)
    }

    private fun upload(file: File): String {
        val auth = Auth.create(accessKey, secretKey)
        val token = auth.uploadToken(bucket)
        val response = uploadManager.put(file, file.name, token)
        return server + "/" + JsonPath.parse(response.bodyString()).read("$.key")
    }
}
