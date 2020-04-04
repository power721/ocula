package com.har01d.ocula.util

import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ThreadLocalRandom

val TOKENS = "Ok4jShBpcvKY5gTQMVRsEHfGe3nDdb81IJwrqLFP0UC6xilazo2ZWut9yNmA7X".toCharArray()

fun generateId(length: Int): String {
    val sb = StringBuilder()
    val random = ThreadLocalRandom.current()
    for (i in 1..length) {
        sb.append(TOKENS[random.nextInt(TOKENS.size)])
    }
    return sb.toString()
}

fun normalizeUrl(refer: String, url: String) =
        try {
            val base = URL(refer)
            val uri = if (url.startsWith("?")) base.path + url else url
            URL(base, uri).toExternalForm()
        } catch (e: MalformedURLException) {
            null
        }

fun String.path(): String {
    val index = indexOfAny(charArrayOf('?', '#'))
    if (index > 0) {
        return substring(0, index)
    }
    return this
}

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.toHex()
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
