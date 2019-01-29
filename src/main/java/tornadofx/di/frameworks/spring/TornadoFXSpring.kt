package tornadofx.di.frameworks.spring

import com.google.inject.AbstractModule
import com.google.inject.Guice
import tornadofx.*
import kotlin.reflect.KClass

/**
 * If I end up overriding the class, will the other constructors need to be overridden as well?
 */

class TornadoFXSpring (override val primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class,
                     vararg stylesheet: KClass<out Stylesheet>,
                     private val classContext: ClassXmlApplicationContext) : App() {

    init {
        Stylesheet.importServiceLoadedStylesheets()
        stylesheet.forEach { importStylesheet(it) }

        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T =
                    classContext.getBean(type.java)

            // override fun <T : Any> getInstance(type: KClass<T>, name: String): T =
            //     guice.getInstance(name, type.java)
        }
    }
}