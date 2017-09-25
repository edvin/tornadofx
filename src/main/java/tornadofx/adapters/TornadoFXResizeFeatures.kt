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

fun <T> TreeTableView.ResizeFeatures<T>.toTornadoFXResizeFeatures() = TornadoFXTreeTableResizeFeatures(this)
fun <T> TableView.ResizeFeatures<T>.toTornadoFXFeatures() = TornadoFxTableResizeFeatures(this)

interface TornadoFXResizeFeatures<COLUMN, out TABLE : Any> {
    val table: TornadoFXTable<COLUMN, TABLE>
    val delta: Double
    val column: TornadoFXColumn<out COLUMN>?
}

class TornadoFXTreeTableResizeFeatures<T>(val param: TreeTableView.ResizeFeatures<T>) : TornadoFXResizeFeatures<TreeTableColumn<T, *>, TreeTableView<*>> {
    override val column  = param.column?.toTornadoFXColumn()
    override val table = param.table.toTornadoFXTable()
    override val delta get() = param.delta!!
}

class TornadoFxTableResizeFeatures<T>(val param: TableView.ResizeFeatures<T>) : TornadoFXResizeFeatures<TableColumn<T, *>, TableView<T>> {
    override val table = param.table.toTornadoFXTable()
    override val delta = param.delta
    override val column = param.column?.toTornadoFXColumn()
}