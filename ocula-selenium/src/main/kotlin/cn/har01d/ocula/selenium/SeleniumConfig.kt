package cn.har01d.ocula.selenium

import cn.har01d.ocula.Config

class SeleniumConfig : Config() {
    var driverType: DriverType = DriverType.CHROME
    var headless: Boolean = true
    var phantomjsExecPath: String? = null
}
