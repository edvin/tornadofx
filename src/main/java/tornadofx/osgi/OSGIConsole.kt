package tornadofx.osgi

import javafx.beans.property.SimpleObjectProperty
import org.osgi.framework.Bundle
import tornadofx.*
import tornadofx.osgi.impl.fxBundleContext

class OSGIConsole : View() {
    override val root = borderpane {
        center {
            tableview<Bundle>() {
                column<Bundle, Long>("ID", { SimpleObjectProperty(it.value.bundleId) })
                column<Bundle, String>("State", { SimpleObjectProperty(it.value.getStateName()) })
                column<Bundle, Int>("Level", { SimpleObjectProperty(it.value.state) })
                column<Bundle, String>("Name", { SimpleObjectProperty(it.value.symbolicName) })
                items.setAll(fxBundleContext.bundles.toList())
                fxBundleContext.addBundleListener {
                    items.setAll(fxBundleContext.bundles.toList())
                }
            }
        }
    }

    fun Bundle.getStateName() = when (state) {
        Bundle.ACTIVE -> "Active"
        Bundle.INSTALLED -> "Installed"
        Bundle.RESOLVED -> "Resolved"
        Bundle.STARTING -> "Starting"
        Bundle.STOPPING -> "Stopping"
        Bundle.UNINSTALLED -> "Uninstalled"
        else -> "Unknown"
    }
}