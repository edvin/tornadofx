package tornadofx

import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.concurrent.Task
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import java.util.function.Predicate

/**
 * A wrapper delegate for an observable list of items that can be bound to a list control like TableView, ListView etc.
 *
 * The wrapper makes the data sortable and filterable. Configure a filter by setting the
 * predicate property or by calling filterWhen to automatically update the predicate when
 * an observable value changes.
 *
 ** Usage:
 *
 * ```kotlin
 * val table = TableView<Person>()
 * val data = SortedFilteredList(persons).bindTo(table)
 * ```
 *
 * Items can be updated by calling `data.setAll` or `data.addAll` at a later time.
 */
@Suppress("UNCHECKED_CAST")
class SortedFilteredList<T>(val items: ObservableList<T> = FXCollections.observableArrayList(), initialPredicate: (T) -> Boolean = { true }) : ObservableList<T> by items {
    var predicate: (T) -> Boolean = initialPredicate
        set(value) { filteredItems.predicate = Predicate { value(it) } }

    val filteredItems = FilteredList(items, predicate)
    val sortedItems = SortedList(filteredItems)

    /**
     * Bind this data object to the given TableView.
     *
     * The `tableView.items` is set to the underlying sortedItems.
     *
     * The underlying sortedItems.comparatorProperty` is automatically bound to `tableView.comparatorProperty`.
     */
    fun bindTo(tableView: TableView<T>): SortedFilteredList<T> {
        tableView.items = sortedItems
        sortedItems.comparatorProperty().bind(tableView.comparatorProperty())
        return this
    }

    /**
     * Bind this data object to the given TableView.
     *
     * The `listView.items` is set to the underlying sortedItems.
     *
     */
    fun bindTo(listView: ListView<T>): SortedFilteredList<T> {
        listView.items = sortedItems
        return this
    }

    /**
     * Update the filter predicate whenever the given observable changes. The filter expression
     * receives both the observable value and the current list item to evaluate.
     *
     * Convenient for filtering based on a TextField:
     *
     * <pre>
     * textfield {
     *     promptText = "Filtrering"
     *     data.filterWhen(textProperty(), { query, item -> item.matches(query) } )
     * }
     * </pre>
     */
    fun <Q> filterWhen(observable: ObservableValue<Q>, filterExpr: (Q, T) -> Boolean) {
        observable.addListener { observableValue, oldValue, newValue ->
            predicate = { filterExpr(newValue, it) }
        }
    }
}

fun <T> List<T>.observable() = FXCollections.observableList(this)

fun <T> task(func: () -> T) = object : Task<T>() {
    override fun call(): T {
        return func()
    }
}.apply {
    setOnFailed({ Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception) })
    Thread(this).start()
}

infix fun <T> Task<T>.success(func: (T) -> Unit): Task<T> {
    Platform.runLater {
        setOnSucceeded { func(value) }
    }
    return this
}