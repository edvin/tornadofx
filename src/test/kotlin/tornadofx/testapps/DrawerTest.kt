package tornadofx.testapps

import javafx.scene.paint.Color
import tornadofx.*

class DrawerTestApp : App(DrawerWorkspace::class) {
    override fun onBeforeShow(view: UIComponent) {
        workspace.dock<TestDrawerContributor>()
    }
}

class JustAView : View() {
    override val root = label("I'm just a view - I do nothing")
}

class TestDrawerContributor : View() {
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
        with(leftDrawer) {
            add(TableViewDirtyTest::class)
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