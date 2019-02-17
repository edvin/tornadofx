package tornadofx.di.frameworks.spring

import tornadofx.*
import kotlin.reflect.KClass

class TornadoFXSpring (override val primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class,
                     vararg stylesheet: KClass<out Stylesheet>) : App() {

    private val spring: SpringController by inject()

    init {
        Stylesheet.importServiceLoadedStylesheets()
        stylesheet.forEach { importStylesheet(it) }

        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>): T =
                    spring.appContext.getBean(type.java)

            override fun <T : Any> getInstance(type: KClass<T>, name: String): T =
                    spring.appContext.getBean(name, type.java)
        }
    }
}