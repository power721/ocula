package com.har01d.ocula.util

import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.min

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

fun Long.toDuration(): String {
    val seconds = this / 1000
    val ms = this % 1000
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d.%03d", h, m, s, ms)
}

fun String.host(): String {
    return URL(this).host
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

fun String.truncate(len: Int) = substring(0, min(len, this.length))

fun String?.number(): Int? {
    if (this == null) return null
    if (this.isEmpty()) return null
    return "\\d+".toRegex().find(this)?.value?.toInt()
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}
