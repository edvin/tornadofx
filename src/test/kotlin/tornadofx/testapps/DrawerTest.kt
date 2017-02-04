package tornadofx.testapps

import javafx.scene.paint.Color
import tornadofx.*

class DrawerTestApp : App(Workspace::class) {
    override fun onBeforeShow(view: UIComponent) {
        workspace.root.left {
            drawer(multiselect = true) {
                item("Contacts", expanded = true) {
                    add(find(TableViewDirtyTest::class).root)
                }
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
}