package com.har01d.ocula

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger


class SpiderThreadFactory(namePrefix: String) : ThreadFactory {
    companion object {
        private val poolNumber = AtomicInteger(1)
    }

    private val threadNumber = AtomicInteger(1)
    private val name: String = namePrefix + "-" + poolNumber.getAndIncrement() + "-"

    override fun newThread(r: Runnable) = Thread(r, name + threadNumber.getAndIncrement()).apply {
        isDaemon = true
        priority = Thread.NORM_PRIORITY
    }
}
