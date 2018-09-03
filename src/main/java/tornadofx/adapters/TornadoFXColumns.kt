package tornadofx.adapters

import javafx.beans.property.DoubleProperty
import javafx.scene.control.TableColumn
import javafx.scene.control.TreeTableColumn

fun TreeTableColumn<*,*>.toTornadoFXColumn() = TornadoFXTreeTableColumn(this)
fun TableColumn<*,*>.toTornadoFXColumn() = TornadoFxNormalTableColumn(this)

interface TornadoFXColumn<COLUMN> {
    val column: COLUMN
    val properties: Properties
    var prefWidth: Double
    var maxWidth: Double
    var minWidth: Double
    val width: Double
    val minWidthProperty: DoubleProperty
    val maxWidthProperty: DoubleProperty
    fun isLegalWidth(width: Double) = width in minWidthProperty.get() .. maxWidthProperty.get()
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
    override val width: Double
        get() = column.width

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TornadoFXTreeTableColumn

        if (column != other.column) return false

        return true
    }

    override fun hashCode(): Int {
        return column.hashCode()
    }

}

class TornadoFxNormalTableColumn(override val column: TableColumn<*, *>) : TornadoFXColumn<TableColumn<*, *>> {
    override var minWidth: Double
        get() = column.minWidth
        set(value) {
            column.minWidth = value
        }
    override var maxWidth: Double
        get() = column.maxWidth
        set(value) {
            column.maxWidth = value
        }
    override var prefWidth: Double
        get() = column.prefWidth
        set(value) {
            column.prefWidth = value
        }
    override val properties = column.properties
    override val width: Double
        get() = column.width
    override val minWidthProperty get() = column.minWidthProperty()
    override val maxWidthProperty get() = column.maxWidthProperty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TornadoFxNormalTableColumn

        if (column != other.column) return false

        return true
    }

    override fun hashCode(): Int {
        return column.hashCode()
    }
}
