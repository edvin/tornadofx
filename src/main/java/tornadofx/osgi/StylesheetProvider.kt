package tornadofx.osgi

import org.osgi.framework.BundleContext
import tornadofx.Stylesheet
import java.util.*
import kotlin.reflect.KClass

interface StylesheetProvider {
    val stylesheet: KClass<out Stylesheet>
}

fun BundleContext.registerStylesheet(stylesheet: KClass<out Stylesheet>) {
    val provider = object : StylesheetProvider {
        override val stylesheet = stylesheet
    }
    registerService(StylesheetProvider::class.java, provider, Hashtable<String, String>())
}