package tornadofx

import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.chart.*

/**
 * Create a PieChart with optional title data and add to the parent pane. The optional op will be performed on the new instance.
 */
fun EventTarget.piechart(title: String? = null, data: ObservableList<PieChart.Data>? = null, op: PieChart.() -> Unit = {}): PieChart {
    val chart = if (data != null) PieChart(data) else PieChart()
    chart.title = title
    return opcr(this, chart, op)
}

/**
 * Add and create a PieChart.Data entry. The optional op will be performed on the data instance,
 * a good place to add event handlers to the PieChart.Data.node for example.
 *
 * @return The new Data entry
 */
fun PieChart.data(name: String, value: Double, op: PieChart.Data.() -> Unit = {}) = PieChart.Data(name, value).apply {
    data.add(this)
    op(this)
}

/**
 * Add and create multiple PieChart.Data entries from the given map.
 */
fun PieChart.data(value: Map<String, Double>) = value.forEach { data(it.key, it.value) }


/**
 * Create a LineChart with optional title, axis and add to the parent pane. The optional op will be performed on the new instance.
 */
fun <X, Y> EventTarget.linechart(title: String? = null, x: Axis<X>, y: Axis<Y>, op: LineChart<X, Y>.() -> Unit = {}): LineChart<X, Y> {
    val chart = LineChart(x, y)
    chart.title = title
    return opcr(this, chart, op)
}

/**
 * Create an AreaChart with optional title, axis and add to the parent pane. The optional op will be performed on the new instance.
 */
fun <X, Y> EventTarget.areachart(title: String? = null, x: Axis<X>, y: Axis<Y>, op: AreaChart<X, Y>.() -> Unit = {}): AreaChart<X, Y> {
    val chart = AreaChart(x, y)
    chart.title = title
    return opcr(this, chart, op)
}

/**
 * Create a BubbleChart with optional title, axis and add to the parent pane. The optional op will be performed on the new instance.
 */
fun <X, Y> EventTarget.bubblechart(title: String? = null, x: Axis<X>, y: Axis<Y>, op: BubbleChart<X, Y>.() -> Unit = {}): BubbleChart<X, Y> {
    val chart = BubbleChart(x, y)
    chart.title = title
    return opcr(this, chart, op)
}

/**
 * Create a ScatterChart with optional title, axis and add to the parent pane. The optional op will be performed on the new instance.
 */
fun <X, Y> EventTarget.scatterchart(title: String? = null, x: Axis<X>, y: Axis<Y>, op: ScatterChart<X, Y>.() -> Unit = {}): ScatterChart<X, Y> {
    val chart = ScatterChart(x, y)
    chart.title = title
    return opcr(this, chart, op)
}

/**
 * Create a BarChart with optional title, axis and add to the parent pane. The optional op will be performed on the new instance.
 */
fun <X, Y> EventTarget.barchart(title: String? = null, x: Axis<X>, y: Axis<Y>, op: BarChart<X, Y>.() -> Unit = {}): BarChart<X, Y> {
    val chart = BarChart(x, y)
    chart.title = title
    return opcr(this, chart, op)
}

/**
 * Create a BarChart with optional title, axis and add to the parent pane. The optional op will be performed on the new instance.
 */
fun <X, Y> EventTarget.stackedbarchart(title: String? = null, x: Axis<X>, y: Axis<Y>, op: StackedBarChart<X, Y>.() -> Unit = {}): StackedBarChart<X, Y> {
    val chart = StackedBarChart(x, y)
    chart.title = title
    return opcr(this, chart, op)
}

/**
 * Add a new XYChart.Series with the given name to the given Chart. Optionally specify a list data for the new series or
 * add data with the optional op that will be performed on the created series object.
 */
fun <X, Y, ChartType : XYChart<X, Y>> ChartType.series(name: String, elements: ObservableList<XYChart.Data<X, Y>>? = null, op: (XYChart.Series<X, Y>).() -> Unit = {}): XYChart.Series<X, Y> {
    val series = XYChart.Series<X, Y>()
    series.name = name
    if (elements != null) series.data = elements
    op(series)
    data.add(series)
    return series
}

/**
 * Add and create a XYChart.Data entry with x, y and optional extra value. The optional op will be performed on the data instance,
 * a good place to add event handlers to the Data.node for example.
 *
 * @return The new Data entry
 */
fun <X, Y> XYChart.Series<X, Y>.data(x: X, y: Y, extra: Any? = null, op: (XYChart.Data<X, Y>).() -> Unit = {}) = XYChart.Data<X, Y>(x, y).apply {
    if (extra != null) extraValue = extra
    data.add(this)
    op(this)
}

/**
 * Helper class for the multiseries support
 */
class MultiSeries<X, Y>(val series: List<XYChart.Series<X, Y>>, val chart: XYChart<X, Y>) {
    fun data(x: X, vararg y: Y) {
        for ((index, value) in y.withIndex())
            series[index].data(x, value)
    }
}

/**
 * Add multiple series XYChart.Series with data in one go. Specify a list of names for the series
 * and then add values in the op. Example:
 * <pre>
 * multiseries("Portfolio 1", "Portfolio 2") {
 *   data(1, 23, 10)
 *   data(2, 14, 5)
 *   data(3, 15, 8)
 *   ...
 * }
 * </pre>
 */
fun <X, Y, ChartType : XYChart<X, Y>> ChartType.multiseries(vararg names: String, op: (MultiSeries<X, Y>).() -> Unit = {}): MultiSeries<X, Y> {
    val series = names.map { XYChart.Series<X, Y>().apply { name = it } }
    val multiseries = MultiSeries(series, this)
    op(multiseries)
    data.addAll(series)
    return multiseries
}