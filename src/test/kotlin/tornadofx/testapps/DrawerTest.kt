package tornadofx.testapps

import javafx.beans.property.SimpleStringProperty
import javafx.scene.paint.Color
import tornadofx.*

class DrawerTestApp : App(DrawerWorkspace::class) {
    override fun onBeforeShow(view: UIComponent) {
        workspace.root.setPrefSize(600.0, 400.0)
        workspace.dock<JustAView>()
    }
}

class JustAView : View("Just A View") {
    override val root = vbox {
        label("I'm just a view - I do nothing")
        button("Load another View").action {
            workspace.dock<TestDrawerContributor>()
        }
    }
}

class AnotherView : View("Another") {
    override val root = stackpane {
        label(title)
    }
}

class YetAnotherView : View("Yet Another") {
    override val root = stackpane {
        label(title)
    }
}

class TestDrawerContributor : View("Test View with dynamic drawer item") {
    override val root = stackpane {
        vbox {
            label("I add something to the drawer when I'm docked")
            button("Load another View").action {
                workspace.dock<AnotherView>()
            }
            button("Load yet another View").action {
                workspace.dock<YetAnotherView>()
            }
        }
    }

    override fun onDock() {
        workspace.leftDrawer.item("Temp Drawer") {
            stackpane {
                label("I'm only guest starring!")
            }
        }
    }
}

class DrawerWorkspace : Workspace("Drawer Workspace", Workspace.NavigationMode.Stack) {
    init {
        menubar {
            menu("Options") {
                checkmenuitem("Toggle Navigation Mode").action {
                    navigationMode = if (navigationMode == NavigationMode.Stack) NavigationMode.Tabs else NavigationMode.Stack
                }
            }
        }
        with(bottomDrawer) {
            item("Console") {
                style {
                    backgroundColor += Color.BLACK
                }
                label("""Connected to the target VM, address: '127.0.0.1:64653', transport: 'socket'
Disconnected from the target VM, address: '127.0.0.1:64653', transport: 'socket'

Process finished with exit code 0
""") {
                    style {
                        backgroundColor += Color.BLACK
                        textFill = Color.LIGHTGREY
                        fontFamily = "Consolas"
                    }
                }
            }

            item("Events") {

            }
        }
        with(leftDrawer) {
            item<TableViewDirtyTest>()
            item("Form item") {
                form {
                    fieldset("Customer Details") {
                        field("Name") { textfield() }
                        field("Password") { textfield() }
                    }
                }
            }
            item("SqueezeBox Item", showHeader = false) {
                squeezebox(multiselect = false, fillHeight = true) {
                    fold("Customer Editor") {
                        form {
                            fieldset("Customer Details") {
                                field("Name") { textfield() }
                                field("Password") { textfield() }
                            }
                        }
                    }
                    fold("Some other editor") {
                        stackpane {
                            label("Nothing here")
                        }
                    }
                    fold("A Table") {
                        tableview(listOf("One", "Two", "Three").observable()) {
                            column<String, String>("Value") { SimpleStringProperty(it.value) }
                        }
                    }
                }
            }
        }
    }
}