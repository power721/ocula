package cn.har01d.ocula.selenium

import cn.har01d.ocula.http.Request
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

interface SeleniumActionHandler {
    fun handle(request: Request, webDriver: WebDriver)
}

open class LoadAll(private val sleep: Long = 1000L) : SeleniumActionHandler {
    override fun handle(request: Request, webDriver: WebDriver) {
        val executor = webDriver as JavascriptExecutor
        var height = executor.executeScript("return document.body.scrollHeight")
        while (true) {
            executor.executeScript("window.scrollTo(0, document.body.scrollHeight)")
            Thread.sleep(sleep)
            val newHeight = executor.executeScript("return document.body.scrollHeight")
            if (newHeight == height) {
                break
            }
            height = newHeight
        }
    }
}

class WaitElementPresent(private val selector: String, private val timeout: Long = 10L) : SeleniumActionHandler {
    override fun handle(request: Request, webDriver: WebDriver) {
        val wait = WebDriverWait(webDriver, timeout)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)))
    }
}

class WaitElementVisible(private val selector: String, private val timeout: Long = 10L) : SeleniumActionHandler {
    override fun handle(request: Request, webDriver: WebDriver) {
        val wait = WebDriverWait(webDriver, timeout)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)))
    }
}

class WaitElementClickable(private val selector: String, private val timeout: Long = 10L) : SeleniumActionHandler {
    override fun handle(request: Request, webDriver: WebDriver) {
        val wait = WebDriverWait(webDriver, timeout)
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)))
    }
}
