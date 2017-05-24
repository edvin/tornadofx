import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.paint.Color
import tornadofx.*

class ListMenuTestApp : App(ListMenuTest::class)

class ListMenuTest : View("ListMenu Test") {
    val listmenu = listmenu(theme = "blue") {
        addMenuItems()
        activeItem = items.first()
        maxHeight = Double.MAX_VALUE
    }

    override val root = borderpane {
        setPrefSize(650.0, 500.0)
        top {
            vbox(10) {
                label(title).style { fontSize = 3.em }
                hbox(10) {
                    alignment = Pos.CENTER
                    label("Orientation")
                    combobox(listmenu.orientationProperty, Orientation.values().toList())
                    label("Icon Position")
                    combobox(listmenu.iconPositionProperty, values = Side.values().toList())
                    label("Theme")
                    combobox(listmenu.themeProperty, values = listOf("none", "blue"))
                    checkbox("Icons Only") {
                        selectedProperty().onChange {
                            with(listmenu) {
                                items.clear()
                                if (it) {
                                    iconOnlyMenuItems()
                                } else {
                                    addMenuItems()
                                }
                            }
                        }
                    }
                }
                label(stringBinding(listmenu.activeItemProperty) { "Currently selected: ${value?.text}" }) {
                    style { textFill = Color.RED }
                }
                style {
                    alignment = Pos.CENTER
                }
            }
        }
        center = listmenu
        style {
            backgroundColor += Color.WHITE
        }
        paddingAll = 20
    }

    private fun ListMenu.iconOnlyMenuItems() {
        item(graphic = icon(USER))
        item(graphic = icon(SUITCASE))
        item(graphic = icon(COG))
    }

    private fun ListMenu.addMenuItems() {
        item("Contacts", icon(USER))
        item("Projects", icon(SUITCASE))
        item("Settings", icon(COG))
    }

    private fun icon(icon: FontAwesomeIcon) = FontAwesomeIconView(icon).apply { glyphSize = 20 }
}