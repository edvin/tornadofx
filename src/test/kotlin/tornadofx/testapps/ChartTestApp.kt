package tornadofx.testapps

import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.paint.Color
import tornadofx.*

class BarChartTestApp : App(BarChartTest::class, BarChartStyles::class)

class BarChartTest : View() {
    override val root = barchart("Stock Monitoring, 2010", CategoryAxis(), NumberAxis()) {
        series("Portfolio 1") {
            data("Jan", 23)
            data("Feb", 14)
            data("Mar", 15)
        }
        series("Portfolio 2") {
            data("Jan", 11)
            data("Feb", 19)
            data("Mar", 27)
        }
    }
}

class BarChartStyles : Stylesheet() {
    val chartBar by cssclass()

    init {
        defaultColor0 and chartBar {
            barFill = Color.VIOLET
        }
    }
}