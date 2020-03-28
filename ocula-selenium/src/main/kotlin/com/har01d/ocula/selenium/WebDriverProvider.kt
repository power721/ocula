package com.har01d.ocula.selenium

import com.har01d.ocula.http.EmptyProxyProvider
import com.har01d.ocula.http.Provider
import com.har01d.ocula.http.ProxyProvider
import com.har01d.ocula.http.RoundRobinProvider
import org.openqa.selenium.MutableCapabilities
import org.openqa.selenium.Proxy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.opera.OperaDriver
import org.openqa.selenium.opera.OperaOptions
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.phantomjs.PhantomJSDriverService
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.safari.SafariOptions

interface WebDriverProvider : Provider<WebDriver> {
    fun clean()
}

class DefaultWebDriverProvider(
        size: Int = 1,
        private val proxyProvider: ProxyProvider = EmptyProxyProvider,
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
        val cap: MutableCapabilities = when (driverType) {
            DriverType.CHROME -> ChromeOptions()
            DriverType.EDGE -> EdgeOptions()
            DriverType.FIREFOX -> FirefoxOptions()
            DriverType.OPERA -> OperaOptions()
            DriverType.SAFARI -> SafariOptions()
            DriverType.PHANTOMJS -> DesiredCapabilities.phantomjs()
        }

        if (proxyProvider.hasAny()) {
            val httpProxy = proxyProvider.select()
            val proxy = Proxy()
            proxy.httpProxy = httpProxy.hostname + ":" + httpProxy.port
            proxy.sslProxy = proxy.httpProxy
            cap.setCapability(CapabilityType.PROXY, proxy)
        }

        cap.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true)

        when (driverType) {
            DriverType.CHROME -> {
                val options = cap as ChromeOptions
                options.setHeadless(true)
                options.setAcceptInsecureCerts(true)
                return ChromeDriver(options)
            }
            DriverType.FIREFOX -> {
                val options = cap as FirefoxOptions
                options.setHeadless(true)
                options.setAcceptInsecureCerts(true)
                return FirefoxDriver(options)
            }
            DriverType.EDGE -> {
                val options = cap as EdgeOptions
                return EdgeDriver(options)
            }
            DriverType.OPERA -> {
                val options = cap as OperaOptions
                return OperaDriver(options)
            }
            DriverType.SAFARI -> {
                val options = cap as SafariOptions
                return SafariDriver(options)
            }
            DriverType.PHANTOMJS -> {
                if (phantomjsExecPath == null) {
                    throw IllegalStateException("phantomjsExecPath is required")
                }
                cap.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsExecPath)
                val args = listOf("--web-security=false", "--ssl-protocol=any", "--ignore-ssl-errors=true")
                cap.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args)
                cap.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, arrayOf("--logLevel=INFO"))
                return PhantomJSDriver(cap)
            }
        }
    }
}
