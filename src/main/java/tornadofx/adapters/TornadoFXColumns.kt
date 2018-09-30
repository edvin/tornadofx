package tornadofx.adapters

import javafx.beans.property.DoubleProperty
import javafx.scene.control.TableColumn
import javafx.scene.control.TreeTableColumn

fun TableColumn<*, *>.toTornadoFXColumn(): TornadoFXColumn<TableColumn<*, *>> = TornadoFxNormalTableColumn(this)
fun TreeTableColumn<*, *>.toTornadoFXColumn(): TornadoFXColumn<TreeTableColumn<*, *>> = TornadoFXTreeTableColumn(this)

interface TornadoFXColumn<COLUMN> {
    val column: COLUMN
    val properties: Properties

    val width: Double
    var prefWidth: Double

    val minWidthProperty: DoubleProperty
    var minWidth: Double

    val maxWidthProperty: DoubleProperty
    var maxWidth: Double

    fun isLegalWidth(width: Double): Boolean = width in minWidthProperty.get()..maxWidthProperty.get()
}

class TornadoFXTreeTableColumn(override val column: TreeTableColumn<*, *>) : TornadoFXColumn<TreeTableColumn<*, *>> {
    override val properties: Properties = column.properties

    override val width: Double
        get() = column.width
    override var prefWidth: Double
        get() = column.prefWidth
        set(value) {
            column.prefWidth = value
        }

    override val minWidthProperty: DoubleProperty get() = column.minWidthProperty()
    override var minWidth: Double
        get() = column.minWidth
        set(value) {
            column.minWidth = value
        }

    override val maxWidthProperty: DoubleProperty get() = column.maxWidthProperty()
    override var maxWidth: Double
        get() = column.maxWidth
        set(value) {
            column.maxWidth = value
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TornadoFXTreeTableColumn) return false

        if (column != other.column) return false

        return true
    }

    override fun hashCode(): Int {
        return column.hashCode()
    }
}

class TornadoFxNormalTableColumn(override val column: TableColumn<*, *>) : TornadoFXColumn<TableColumn<*, *>> {
    override val properties: Properties = column.properties

    override val width: Double
        get() = column.width
    override var prefWidth: Double
        get() = column.prefWidth
        set(value) {
            column.prefWidth = value
        }

    override val minWidthProperty: DoubleProperty get() = column.minWidthProperty()
    override var minWidth: Double
        get() = column.minWidth
        set(value) {
            column.minWidth = value
        }

    override val maxWidthProperty: DoubleProperty get() = column.maxWidthProperty()
    override var maxWidth: Double
        get() = column.maxWidth
        set(value) {
            column.maxWidth = value
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TornadoFxNormalTableColumn) return false

        if (column != other.column) return false

        return true
    }

    override fun hashCode(): Int {
        return column.hashCode()
    }
}
