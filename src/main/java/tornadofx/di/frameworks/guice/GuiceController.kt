package tornadofx.di.frameworks.guice

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import tornadofx.*

class GuiceController(vararg modules: Module): Controller() {
    val injector: Injector = Guice.createInjector(*modules)
}
