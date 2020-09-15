package cn.har01d.ocula.ocr

import org.bytedeco.javacpp.Loader
import org.bytedeco.leptonica.global.lept.pixDestroy
import org.bytedeco.leptonica.global.lept.pixRead
import org.bytedeco.tesseract.TessBaseAPI
import org.bytedeco.tesseract.global.tesseract.PSM_AUTO_OSD
import java.io.File
import java.net.URL


/**
 * Download osd file and language files from [https://github.com/tesseract-ocr/tessdata_fast]
 */
class OcrUtil {
    fun detect(
        url: URL,
        dataPath: String,
        language: String = "chi_sim",
        mode: Int = PSM_AUTO_OSD,
        removeEmptyLine: Boolean = true
    ): String {
        val file: File = Loader.cacheResource(url)
        return detect(file.absolutePath, dataPath, language, mode, removeEmptyLine)
    }

    fun detect(
        filename: String,
        dataPath: String,
        language: String = "chi_sim",
        mode: Int = PSM_AUTO_OSD,
        removeEmptyLine: Boolean = true
    ): String {
        val api = TessBaseAPI()
        if (api.Init(dataPath, language) != 0) {
            throw IllegalStateException("Check if data files (osd.traineddata and $language.traineddata) exist")
        }

        val image = pixRead(filename)
        api.SetPageSegMode(mode)
        api.SetImage(image)
        val outText = api.GetUTF8Text()

        api.End()
        outText.deallocate()
        pixDestroy(image)
        return if (removeEmptyLine) {
            val lines = outText.string.split("\n").filter { it.isNotBlank() }
            lines.joinToString("\n")
        } else {
            outText.string
        }
    }
}
