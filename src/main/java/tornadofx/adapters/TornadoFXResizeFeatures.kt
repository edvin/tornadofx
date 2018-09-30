package tornadofx.adapters

import javafx.collections.ObservableMap
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView

typealias Properties = ObservableMap<Any?, Any?>

fun TableView.ResizeFeatures<*>.toTornadoFXFeatures(): TornadoFXResizeFeatures<TableColumn<*, *>, TableView<*>> = TornadoFxTableResizeFeatures(this)
fun TreeTableView.ResizeFeatures<*>.toTornadoFXResizeFeatures(): TornadoFXResizeFeatures<TreeTableColumn<*, *>, TreeTableView<*>> = TornadoFXTreeTableResizeFeatures(this)

interface TornadoFXResizeFeatures<COLUMN, out TABLE : Any> {
    val delta: Double
    val column: TornadoFXColumn<COLUMN>?
    val table: TornadoFXTable<COLUMN, TABLE>
}

// FIXME 'Fx' should be 'FX'
class TornadoFxTableResizeFeatures(val param: TableView.ResizeFeatures<out Any>) : TornadoFXResizeFeatures<TableColumn<*, *>, TableView<*>> {
    override val delta: Double = param.delta
    override val column: TornadoFXColumn<TableColumn<*, *>>? = param.column?.toTornadoFXColumn()
    override val table: TornadoFXTable<TableColumn<*, *>, TableView<*>> = param.table.toTornadoFXTable()
}

class TornadoFXTreeTableResizeFeatures(val param: TreeTableView.ResizeFeatures<out Any>) : TornadoFXResizeFeatures<TreeTableColumn<*, *>, TreeTableView<*>> {
    override val delta: Double get() = param.delta
    override val column: TornadoFXColumn<TreeTableColumn<*, *>>? = param.column?.toTornadoFXColumn()
    override val table: TornadoFXTable<TreeTableColumn<*, *>, TreeTableView<*>> = param.table.toTornadoFXTable()
}
