package tornadofx.adapters

import javafx.collections.ObservableMap
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.util.Callback

typealias Properties = ObservableMap<Any?, Any?>
typealias TableViewResizeCallback = Callback<TableView.ResizeFeatures<out Any>, Boolean>
typealias TreeTableViewResizeCallback = Callback<TreeTableView.ResizeFeatures<out Any>, Boolean>

fun TreeTableView.ResizeFeatures<*>.toTornadoFXResizeFeatures() = TornadoFXTreeTableResizeFeatures(this)
fun TableView.ResizeFeatures<*>.toTornadoFXFeatures() = TornadoFxTableResizeFeatures(this)

interface TornadoFXResizeFeatures<COLUMN, out TABLE : Any> {
    val table: TornadoFXTable<COLUMN, TABLE>
    val delta: Double
    val column: TornadoFXColumn<COLUMN>?
}

class TornadoFXTreeTableResizeFeatures(val param: TreeTableView.ResizeFeatures<out Any>) : TornadoFXResizeFeatures<TreeTableColumn<*, *>, TreeTableView<*>> {
    override val column = param.column?.toTornadoFXColumn()
    override val table = param.table.toTornadoFXTable()
    override val delta get() = param.delta!!
}

class TornadoFxTableResizeFeatures(val param: TableView.ResizeFeatures<out Any>) : TornadoFXResizeFeatures<TableColumn<*, *>, TableView<*>> {
    override val table: TornadoFXTable<TableColumn<*, *>, TableView<*>> = TornadoFXNormalTable(param.table)
    override val delta: Double = param.delta
    override val column: TornadoFXColumn<TableColumn<*, *>>? = param.column?.let { TornadoFxNormalTableColumn(it) }
}