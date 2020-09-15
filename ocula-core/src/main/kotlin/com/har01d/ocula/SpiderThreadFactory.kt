package cn.har01d.ocula

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger


class SpiderThreadFactory(namePrefix: String) : ThreadFactory {
    companion object {
        private val map = mutableMapOf<String, AtomicInteger>()
    }

    private val number = AtomicInteger(1)
    private val name: String

    init {
        val id = map.getOrDefault(namePrefix, AtomicInteger(1))
        map[namePrefix] = id
        name = "$namePrefix-" + id.getAndIncrement() + "-"
    }

    override fun newThread(r: Runnable) = Thread(r, name + number.getAndIncrement()).apply {
        isDaemon = true
        priority = Thread.NORM_PRIORITY
    }
}
