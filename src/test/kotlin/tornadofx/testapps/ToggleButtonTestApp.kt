package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import tornadofx.*

/**
 * Test case for #785
 *
 * Functions to bind both String and ObservableValue<String>
 *
 * @author carl
 */

class ToggleButtonTestView : View("Toggle Button Test") {
    override val root = vbox {
        togglebutton("String")
        togglebutton(SimpleStringProperty("Observable"))
        padding = Insets(10.0)
        spacing = 4.0
    }
}

class ToggleButtonTestApp : App(ToggleButtonTestView::class)
