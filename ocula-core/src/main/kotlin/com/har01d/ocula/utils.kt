package com.har01d.ocula

import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest

fun normalizeUrl(refer: String, url: String) =
        try {
            val base = URL(refer)
            val uri = if (url.startsWith("?")) base.path + url else url
            val abs = URL(base, uri)
            abs.toExternalForm()
        } catch (e: MalformedURLException) {
            null
        }

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.toHex()
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
