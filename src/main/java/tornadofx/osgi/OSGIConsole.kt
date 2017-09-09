package tornadofx.osgi

import javafx.scene.control.TableView
import javafx.scene.control.TextInputDialog
import javafx.scene.input.TransferMode
import javafx.stage.FileChooser
import org.osgi.framework.Bundle
import org.osgi.framework.Bundle.ACTIVE
import org.osgi.framework.startlevel.BundleStartLevel
import tornadofx.*
import tornadofx.osgi.impl.fxBundleContext
import java.nio.file.Files

class OSGIConsole : View() {
    override val root = borderpane {
        title = "TornadoFX OSGi Console"
        prefWidth = 800.0
        prefHeight = 600.0

        center {
            tableview<Bundle> {
                columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

                column("ID", Long::class) {
                    value { it.value.bundleId }
                    fixedWidth(75.0)
                }

                column("State", String::class) {
                    value { it.value.stateDescription }
                    fixedWidth(120.0)
                }
                column("Level", Int::class) {
                    value { it.value.state }
                    fixedWidth(50.0)
                }
                column("Name", String::class) {
                    value { it.value.description }
                }

                items.setAll(fxBundleContext.bundles.toList())

                fxBundleContext.addBundleListener {
                    items.setAll(fxBundleContext.bundles.toList())
                }

                contextmenu {
                    item("Stop").action {
                        selectedItem?.stop()
                    }
                    item("Start").action {
                        selectedItem?.start()
                    }
                    item("Uninstall").action {
                        selectedItem?.uninstall()
                    }
                    item("Update").action {
                        selectedItem?.update()
                    }
                    item("Update from...").action {
                        val result = chooseFile("Select file to replace ${selectedItem!!.symbolicName}", arrayOf(FileChooser.ExtensionFilter("OSGi Bundle Jar", "jar")))
                        if (result.isNotEmpty()) selectedItem?.update(Files.newInputStream(result.first().toPath()))
                    }
                    item("Set start level...").action {
                        TextInputDialog("").showAndWait().ifPresent {
                            selectedItem!!.bundleContext.bundle.adapt(BundleStartLevel::class.java).startLevel = it.toInt()
                        }
                    }
                }

                setOnContextMenuRequested {
                    val stop = contextMenu.items.first { it.text == "Stop" }
                    val start = contextMenu.items.first { it.text == "Start" }
                    stop.isDisable = selectedItem?.state != Bundle.ACTIVE
                    start.isDisable = !stop.isDisable
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
        ACTIVE -> "Active"
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