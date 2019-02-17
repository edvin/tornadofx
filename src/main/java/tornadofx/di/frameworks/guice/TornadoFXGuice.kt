package tornadofx.di.frameworks.guice

import com.google.inject.Key
import com.google.inject.name.Names
import tornadofx.*
import kotlin.reflect.KClass

/**
 * If I end up overriding the class, will the other constructors need to be overridden as well?
  */

class TornadoFXGuice(override val primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class,
                     vararg stylesheet: KClass<out Stylesheet>) : App() {

    private val guice: GuiceController by inject()

    init {
        Stylesheet.importServiceLoadedStylesheets()
        stylesheet.forEach { importStylesheet(it) }

        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T =
                    guice.injector.getInstance(type.java)

            override fun <T : Any> getInstance(type: KClass<T>, name: String): T =
                    guice.injector.getInstance(Key.get(type.java, Names.named(name)))
        }
    }
}