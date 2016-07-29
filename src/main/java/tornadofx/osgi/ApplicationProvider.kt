package tornadofx.osgi

import tornadofx.App
import kotlin.reflect.KClass

interface ApplicationProvider {
    val application: KClass<out App>
}