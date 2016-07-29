package tornadofx.osgi

import kotlin.reflect.KClass

interface StylesheetProvider {
    val stylesheet: KClass<out StylesheetProvider>
}