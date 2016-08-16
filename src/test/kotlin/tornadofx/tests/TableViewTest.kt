package tornadofx.tests

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.junit.Before
import org.junit.Test
import org.testfx.api.FxRobot
import org.testfx.api.FxToolkit
import tornadofx.column
import tornadofx.makeIndexColumn
import tornadofx.tableview
import java.nio.file.Paths

class TableViewTest {
    class TestObject(i: Int) {
        val A = SimpleIntegerProperty(5 * i)
        val B = SimpleDoubleProperty(3.14159 * i)
        val C = SimpleStringProperty("Test string $i")
    }

    val TestList = FXCollections.observableArrayList(Array(5, { TestObject(it) }).asList())

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun columnTest() {
        FxToolkit.setupFixture {
            val root = StackPane().apply {
                tableview(TestList) {
                    makeIndexColumn()
                    column("A Column", TestObject::A)
                    column("B Column", TestObject::B)
                    column("C Column", TestObject::C)
                }
                setPrefSize(400.0, 160.0)
            }
            primaryStage.scene = Scene(root)
            primaryStage.show()
        }

        val robot = FxRobot()
        robot.robotContext().captureSupport.saveImage(robot.capture(primaryStage.scene.root), Paths.get("example-table.png"))
    }
}
