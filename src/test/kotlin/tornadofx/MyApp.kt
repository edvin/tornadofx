package tornadofx

import javafx.scene.layout.VBox

class MyApp: App() {
    override val primaryView = MenuView::class
}


class MenuView: View() {

    override val root = VBox()

    init {
        with(root) {
            menubar {
                menu("File") {
                    menu("Switch Account") {
                        menuitem("Facebook", "Shortcut+F") { println("Switching to Facebook") }
                        menuitem("Twitter", "Shortcut+T") { println("Switching to Twitter") }
                    }
                    separator()
                    menuitem("Save","Shortcut+S") { println("Saving") }
                    menuitem("Exit","Shortcut+Q") { System.exit(0) }
                }
                menu("Edit") {
                    menuitem("Copy") { println("Copying") }
                    menuitem("Paste") { println("Pasting") }
                    separator()
                    menu("Options") {
                        menuitem("Account") { println("Launching Account Options") }
                        menuitem("Security") { println("Launching Security Options") }
                        menuitem("Appearance") { println("Launching Appearance Options") }
                    }
                }
            }
        }
    }
}