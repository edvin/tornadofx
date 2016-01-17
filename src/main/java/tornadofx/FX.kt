package tornadofx

import javafx.stage.Stage
import java.util.*
import kotlin.reflect.KClass

class FX {
    companion object {
        lateinit var primaryStage: Stage
        lateinit var application: App
        val stylesheets = ArrayList<String>()
        val components = HashMap<KClass<out Component>, Component>()
        val lock = Any()
    }
}

fun importStylesheet(stylesheet: String) {
    val css = FX::class.java.getResource(stylesheet)
    FX.stylesheets.add(css.toExternalForm())
}

inline fun <reified T : Component> find(): T = find(T::class)

@Suppress("UNCHECKED_CAST")
fun <T : Component> find(type: KClass<T>): T {
    if (Fragment::class.java.isAssignableFrom(type.java))
        return type.java.newInstance()

    if (!FX.components.containsKey(type)) {
        synchronized(FX.lock) {
            if (!FX.components.containsKey(type))
                FX.components[type] = type.java.newInstance()
        }
    }

    return FX.components[type] as T
}