package tornadofx

import javafx.collections.ObservableList
import javafx.scene.chart.PieChart
import javafx.scene.layout.Pane

/**
 * Create a PieChart with optional title data. The op will be performed on the new instance.
 */
fun Pane.piechart(title: String? = null, data: ObservableList<PieChart.Data>? = null, op: PieChart.() -> Unit): PieChart {
    val chart = if (data != null) PieChart(data) else PieChart()
    if (title != null) chart.title = title
    return opcr(this, chart, op)
}

/**
 * Add and create a PieChart.Data entry. The op will be performed on the data instance,
 * a good place to add event handlers to the PieChart.Data.node for example.
 *
 * @return The new Data entry
 */
fun PieChart.data(name: String, value: Double, op: (PieChart.Data.() -> Unit)? = null) = PieChart.Data(name, value).apply {
    data.add(this)
    if (op != null) op(this)
}

/**
 * Add and create multiple PieChart.Data entries from the given map.
 */
fun PieChart.data(value: Map<String, Double>) = value.forEach { data(it.key, it.value) }

