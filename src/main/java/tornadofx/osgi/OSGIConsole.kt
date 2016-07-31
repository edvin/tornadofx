package tornadofx.osgi

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.TableView
import javafx.scene.input.TransferMode
import javafx.stage.FileChooser
import org.osgi.framework.Bundle
import tornadofx.*
import tornadofx.osgi.impl.fxBundleContext
import java.nio.file.Files

class OSGIConsole : View() {
    override val root = borderpane {
        title = "TornadoFX OSGi Console"
        prefWidth = 800.0
        prefHeight = 600.0

        center {
            tableview<Bundle>() {
                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

                column<Bundle, Long>("ID", { SimpleObjectProperty(it.value.bundleId) }).fixedWidth(75.0)
                column<Bundle, String>("State", { SimpleObjectProperty(it.value.stateDescription) }).fixedWidth(120.0)
                column<Bundle, Int>("Level", { SimpleObjectProperty(it.value.state) }).fixedWidth(50.0)
                column<Bundle, String>("Name", { SimpleObjectProperty(it.value.description) })

                items.setAll(fxBundleContext.bundles.toList())

                fxBundleContext.addBundleListener {
                    items.setAll(fxBundleContext.bundles.toList())
                }

                contextmenu {
                    menuitem("Stop") {
                        selectedItem?.stop()
                    }
                    menuitem("Start") {
                        selectedItem?.start()
                    }
                    menuitem("Uninstall") {
                        selectedItem?.uninstall()
                    }
                    menuitem("Update") {
                        selectedItem?.update()
                    }
                    menuitem("Update from...") {
                        val result = chooseFile("Select file to replace ${selectedItem!!.symbolicName}", arrayOf(FileChooser.ExtensionFilter("OSGi Bundle Jar", "jar")))
                        if (result.isNotEmpty()) selectedItem?.update(Files.newInputStream(result.first().toPath()))
                    }
                }

                setOnDragOver { event ->
                    if (event.dragboard.hasFiles()) event.acceptTransferModes(TransferMode.COPY)
                    event.consume()
                }

                setOnDragDropped { event ->
                    if (event.dragboard.hasFiles() && event.dragboard.files.first().name.toLowerCase().endsWith(".jar")) {
                        event.dragboard.files.forEach {
                            fxBundleContext.installBundle("file:${it.absolutePath}")
                        }
                        event.isDropCompleted = true
                    } else {
                        event.isDropCompleted = false
                    }
                    event.consume()
                }
            }
        }

    }

    val Bundle.stateDescription : String get() = when (state) {
        Bundle.ACTIVE -> "Active"
        Bundle.INSTALLED -> "Installed"
        Bundle.RESOLVED -> "Resolved"
        Bundle.STARTING -> "Starting"
        Bundle.STOPPING -> "Stopping"
        Bundle.UNINSTALLED -> "Uninstalled"
        else -> "Unknown"
    }

    val Bundle.description : String get() {
        val name = headers["Bundle-Name"] ?: symbolicName ?: location ?: "?"
        return "$name | $version"
    }
}