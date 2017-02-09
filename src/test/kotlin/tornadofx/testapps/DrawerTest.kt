package tornadofx.testapps

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
        button("Load another View") {
            setOnAction {
                workspace.dock<TestDrawerContributor>()
            }
        }
    }
}

class TestDrawerContributor : View("Test View with dynamic drawer item") {
    override val root = stackpane {
        vbox {
            label("I add something to the drawer when I'm docked")
            button("Close") {
                setOnAction {
                    close()
                }
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

    override fun onUndock() {
        workspace.viewStack.remove(this)
    }
}

class DrawerWorkspace : Workspace("Drawer Workspace", Workspace.NavigationMode.Stack) {
    init {
        menubar {
            menu("Options") {
                checkmenuitem("Toggle Navigation") {
                    setOnAction {
                        navigationMode = if (navigationMode == NavigationMode.Stack) NavigationMode.Tabs else NavigationMode.Stack
                    }
                }
            }
        }
        with(leftDrawer) {
            item(TableViewDirtyTest::class)
            item("Second Item") {
                stackpane {
                    label("Content of Item Two")
                    style { backgroundColor += Color.RED }
                }
            }
            item("Form item") {
                form {
                    fieldset("Customer Details") {
                        field("Name") { textfield() }
                        field("Password") { textfield() }
                    }
                }
            }
            item("SqueezeBox Item") {
                squeezebox(multiselect = false) {
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
                }
            }
            item("Third Item") {
                stackpane {
                    label("Content of Item Three")
                    style { backgroundColor += Color.YELLOW }
                }
            }
        }
    }
}