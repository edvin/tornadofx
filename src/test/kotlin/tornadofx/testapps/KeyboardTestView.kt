package tornadofx.testapps

import tornadofx.*

class KeyboardTestApp : App(KeyboardTestView::class)

class KeyboardTestView : View("TornadoFX Keyboard Layout") {
    override val root = vbox(30) {
        paddingAll = 40
        // Loaded with JSON
        keyboard {
            load(resources.json("/tornadofx/tests/TornadoKeyboard.json"))
            println(toKeyboardLayoutEditorFormat())
        }
        // Generated with builders
        keyboard {
            row {
                key("", width = 1.25)
                key("")
                key("")
                key("")
                key("")
                key("")
                key("Del", height = 2)
                key("")
                key("")
                key("")
                key("")
                key("")
                key("")
                key("")
            }
            row {
                key("", width = 1.25)
                key("")
                key("Ctrl+W")
                key("Esc")
                key("")
                key("")
                spacer()
                key("")
                key("PgUp")
                key("▲")
                key("PgDn")
                key("")
                key("")
                key("")
            }
            row {
                key("", width = 1.25)
                key("")
                key("")
                key("")
                key("")
                key("")
                key("", height = 2)
                key("Home")
                key("◀")
                key("▼")
                key("▶")
                key("Ins")
                key("Del")
                key("")
            }
            row {
                key("", width = 1.25)
                key("Ctrl+Z")
                key("Ctrl+X")
                key("Ctrl+C")
                key("Ctrl+V")
                key("")
                spacer()
                key("End")
                key("")
                key("")
                key("")
                key("")
                key("", width = 2)
            }
            row {
                key("", width = 1.25)
                key("", width = 1.25)
                key("", width = 1.25)
                key("FN (Pressed)", width = 2.75)
                key("", width = 2.75)
                key("", width = 1.25)
                key("", width = 1.25)
                key("", width = 1.25)
                key("", width = 1.25)
            }
        }
    }
}