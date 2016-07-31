package tornadofx.osgi

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.TableView
import javafx.scene.input.TransferMode
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import org.osgi.framework.Bundle
import org.osgi.framework.Bundle.ACTIVE
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

                val selectedIsActive = booleanBinding(selectionModel.selectedItemProperty()) {
                    get()?.state == ACTIVE
                }

                contextmenu {
                    menuitem("Stop") {
                        selectedItem?.stop()
                    }.disableProperty().bind(selectedIsActive.not())
                    menuitem("Start") {
                        selectedItem?.start()
                    }.disableProperty().bind(selectedIsActive)
                    menuitem("Uninstall", graphic = iconCross) {
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

    val iconCross = icon("M7.48 8l3.75 3.75-1.48 1.48L6 9.48l-3.75 3.75-1.48-1.48L4.52 8 .77 4.25l1.48-1.48L6 6.52l3.75-3.75 1.48 1.48z")

    fun icon(svg: String) = Pane().apply {
        style {
            prefWidth = 16.px
            prefHeight = prefWidth
            fill = Color.GRAY
            shape = svg
        }
    }
}