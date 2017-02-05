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
                    workspace.dock<JustAView>()
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
}

class DrawerWorkspace : Workspace("Drawer Workspace") {
    init {
        menubar {
            menu("Options") {
                checkmenuitem("Floating dock") {
                    selectedProperty().bindBidirectional(leftDrawer.floatingContentProperty)
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
            item("Third Item") {
                stackpane {
                    label("Content of Item Three")
                    style { backgroundColor += Color.YELLOW }
                }
            }
        }
    }
}