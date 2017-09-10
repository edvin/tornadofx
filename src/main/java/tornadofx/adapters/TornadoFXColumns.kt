package tornadofx.adapters

import javafx.beans.property.DoubleProperty
import javafx.scene.control.TableColumn
import javafx.scene.control.TreeTableColumn

fun TreeTableColumn<*,*>.toTornadoFXColumn() = TornadoFXTreeTableColumn(this)
fun TableColumn<*,*>.toTornadoFXColumn() = TornadoFxNormalTableColumn(this)

interface TornadoFXColumn<COLUMN> {
    val column: Any
    val properties: Properties
    var prefWidth: Double
    var maxWidth: Double
    var minWidth: Double
    var width: Double
    val minWidthProperty: DoubleProperty
    val maxWidthProperty: DoubleProperty
}

class TornadoFXTreeTableColumn(override val column: TreeTableColumn<*, *>) : TornadoFXColumn<TreeTableColumn<*, *>> {
    override val minWidthProperty get() = column.minWidthProperty()
    override val maxWidthProperty get() = column.maxWidthProperty()
    override var minWidth: Double
        get() = column.minWidth
        set(value) {
            column.minWidth = value
        }
    override val properties = column.properties
    override var prefWidth: Double
        get() = column.prefWidth
        set(value) {
            column.prefWidth = value
        }
    override var maxWidth: Double
        get() = column.maxWidth
        set(value) {
            column.maxWidth = value
        }
    override var width: Double
        get() = column.width
        set(value) {
            column.maxWidth = value
        }
}

class TornadoFxNormalTableColumn(override val column: TableColumn<*, *>) : TornadoFXColumn<TableColumn<*, *>> {
    override var minWidth: Double
        get() = column.minWidth;
        set(value) {
            column.minWidth = value
        }
    override var maxWidth: Double
        get() = column.maxWidth;
        set(value) {
            column.maxWidth = value
        }
    override var prefWidth: Double
        get() = column.prefWidth;
        set(value) {
            column.prefWidth = value
        }
    override val properties = column.properties
    override var width: Double
        get() = column.width;
        set(value) {
            column.prefWidth = value
        }
    override val minWidthProperty get() = column.minWidthProperty()
    override val maxWidthProperty get() = column.maxWidthProperty()
}
