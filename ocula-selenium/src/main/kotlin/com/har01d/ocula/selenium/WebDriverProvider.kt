package com.har01d.ocula.selenium

import com.har01d.ocula.http.Provider
import com.har01d.ocula.http.ProxyProvider
import com.har01d.ocula.http.RoundRobinProvider
import org.openqa.selenium.Proxy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.phantomjs.PhantomJSDriverService
import org.openqa.selenium.remote.DesiredCapabilities

interface WebDriverProvider : Provider<WebDriver> {
    fun clean()
}

class DefaultWebDriverProvider(
        size: Int,
        private val proxyProvider: ProxyProvider,
        private val driverType: DriverType = DriverType.CHROME,
        private val phantomjsExecPath: String? = null,
        private val drivers: MutableList<WebDriver> = mutableListOf()
) : WebDriverProvider, RoundRobinProvider<WebDriver>(drivers) {
    init {
        for (i in 1..size) {
            drivers += driver()
        }
    }

    override fun clean() {
        drivers.forEach { it.quit() }
    }

    private fun driver(): WebDriver {
        val cap = when (driverType) {
            DriverType.CHROME -> DesiredCapabilities.chrome()
            DriverType.FIREFOX -> DesiredCapabilities.firefox()
            DriverType.PHANTOMJS -> DesiredCapabilities.phantomjs()
        }
        cap.isJavascriptEnabled = true

        if (proxyProvider.hasAny()) {
            val httpProxy = proxyProvider.select()
            val proxy = Proxy()
            proxy.httpProxy = httpProxy.hostname + ":" + httpProxy.port
            proxy.sslProxy = proxy.httpProxy
            cap.setCapability("proxy", proxy)
        }

        if (driverType == DriverType.PHANTOMJS) {
            if (phantomjsExecPath == null) {
                throw IllegalStateException("phantomjsExecPath is required")
            }
            cap.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsExecPath)
        }

        val args = listOf("--web-security=false", "--ssl-protocol=any", "--ignore-ssl-errors=true")
        cap.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args)
        cap.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, arrayOf("--logLevel=INFO"))

        return when (driverType) {
            DriverType.CHROME -> ChromeDriver(cap)
            DriverType.FIREFOX -> FirefoxDriver(cap)
            DriverType.PHANTOMJS -> PhantomJSDriver(cap)
        }
    }
}
