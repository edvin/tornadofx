package tornadofx.di.frameworks.guice

import com.google.inject.AbstractModule
import com.google.inject.Guice
import tornadofx.*
import kotlin.reflect.KClass

/**
 * If I end up overriding the class, will the other constructors need to be overridden as well?
  */

class TornadoFXGuice(override val primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class,
                     vararg stylesheet: KClass<out Stylesheet>,
                     private val module: AbstractModule) : App() {

    init {
        Stylesheet.importServiceLoadedStylesheets()
        stylesheet.forEach { importStylesheet(it) }

        val guice = Guice.createInjector(module)

        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T =
                    guice.getInstance(type.java)

            // override fun <T : Any> getInstance(type: KClass<T>, name: String): T =
               //     guice.getInstance(name, type.java)
        }
    }
}