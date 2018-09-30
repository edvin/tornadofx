package tornadofx.adapters

import javafx.beans.property.ObjectProperty
import javafx.scene.control.*
import javafx.util.Callback
import tornadofx.mapEach

fun TableView<*>.toTornadoFXTable(): TornadoFXTable<TableColumn<*, *>, TableView<*>> = TornadoFXNormalTable(this)
fun TreeTableView<*>.toTornadoFXTable(): TornadoFXTable<TreeTableColumn<*, *>, TreeTableView<*>> = TornadoFXTreeTable(this)

interface TornadoFXTable<COLUMN, out TABLE : Any> {
    val table: TABLE
    val properties: Properties
    val contentWidth: Double
    val contentColumns: List<TornadoFXColumn<COLUMN>>

    val skinProperty: ObjectProperty<Skin<*>>
    var skin: Skin<*>?
}

class TornadoFXTreeTable(override val table: TreeTableView<*>) : TornadoFXTable<TreeTableColumn<*, *>, TreeTableView<*>> {
    override val properties: Properties = table.properties
    override val contentWidth: Double get() = contentWidthField.get(table) as Double
    override val contentColumns: List<TornadoFXColumn<TreeTableColumn<*, *>>>
        get() = table.columns.flatMap { if (it.columns.isEmpty()) listOf(it) else it.columns }.mapEach { toTornadoFXColumn() }

    private val contentWidthField by lazy { TreeTableView::class.java.getDeclaredField("contentWidth").also { it.isAccessible = true } }

    override val skinProperty: ObjectProperty<Skin<*>> = table.skinProperty()
    override var skin: Skin<*>?
        get() = table.skin
        set(value) {
            table.skin = value
        }

    // TODO Review
    var columnResizePolicy: Callback<TreeTableView.ResizeFeatures<Any>, Boolean>
        get() = table.columnResizePolicy
        set(value) {
            table.columnResizePolicy = value
        }
}

class TornadoFXNormalTable(override val table: TableView<*>) : TornadoFXTable<TableColumn<*, *>, TableView<*>> {
    override val properties: Properties = table.properties
    override val contentWidth: Double get() = contentWidthField.get(table) as Double
    override val contentColumns: List<TornadoFXColumn<TableColumn<*, *>>>
        get() = table.columns.flatMap { if (it.columns.isEmpty()) listOf(it) else it.columns }.mapEach { toTornadoFXColumn() }

    private val contentWidthField by lazy { TableView::class.java.getDeclaredField("contentWidth").also { it.isAccessible = true } }

    override val skinProperty: ObjectProperty<Skin<*>> get() = table.skinProperty()
    override var skin: Skin<*>?
        get() = table.skin
        set(value) {
            table.skin = value
        }
}
