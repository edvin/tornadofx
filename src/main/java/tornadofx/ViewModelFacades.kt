package tornadofx

import javafx.beans.Observable
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet

interface BindingAware

/**
 * A property wrapper that will report it's ViewModel relation
 * to a central location to make it possible for validators
 * to retrieve the correct ValidationContext from the ViewModel
 * this property is bound to.
 */
interface BindingAwareProperty<T> : BindingAware, Property<T> {
    fun recordBinding(observableValue: ObservableValue<*>?) {
        if (observableValue is Observable) {
            ViewModel.propertyToFacade[observableValue] = this
            ViewModel.propertyToViewModel[observableValue] = bean as ViewModel
        }
    }
}

class BindingAwareSimpleBooleanProperty(viewModel: ViewModel, name: String?) : SimpleBooleanProperty(viewModel, name), BindingAwareProperty<Boolean> {

    override fun bind(rawObservable: ObservableValue<out Boolean>?) {
        super.bind(rawObservable)
        recordBinding(rawObservable)
    }

    override fun bindBidirectional(other: Property<Boolean>?) {
        super.bindBidirectional(other)
        recordBinding(other)
    }
}

class BindingAwareSimpleStringProperty(viewModel: ViewModel, name: String?) : SimpleStringProperty(viewModel, name), BindingAwareProperty<String> {
    override fun bind(rawObservable: ObservableValue<out String>?) {
        super.bind(rawObservable)
        recordBinding(rawObservable)
    }

    override fun bindBidirectional(other: Property<String>?) {
        super.bindBidirectional(other)
        recordBinding(other)
    }
}

class BindingAwareSimpleObjectProperty<T>(viewModel: ViewModel, name: String?) : SimpleObjectProperty<T>(viewModel, name), BindingAwareProperty<T> {
    override fun bind(rawObservable: ObservableValue<out T>?) {
        super.bind(rawObservable)
        recordBinding(rawObservable)
    }

    override fun bindBidirectional(other: Property<T>?) {
        super.bindBidirectional(other)
        recordBinding(other)
    }
}

class BindingAwareSimpleListProperty<T>(viewModel: ViewModel, name: String?) : SimpleListProperty<T>(viewModel, name) {
    override fun bind(newObservable: ObservableValue<out ObservableList<T>>?) {
        super.bind(newObservable)
        recordBinding(newObservable)
    }

    override fun bindContentBidirectional(list: ObservableList<T>?) {
        super.bindContentBidirectional(list)
        recordBinding(list)
    }

    fun recordBinding(observableValue: Observable?) {
        if (observableValue != null) {
            ViewModel.propertyToFacade[observableValue] = this
            ViewModel.propertyToViewModel[observableValue] = bean as ViewModel
        }
    }

    /**
     * Return a unique id for this object instead of the default hashCode which is dependent on the children.
     * Without this override we wouldn't be able to identify the facade in our internal maps.
     */
    override fun hashCode() = System.identityHashCode(this)
    override fun equals(other: Any?) = this === other
}

class BindingAwareSimpleSetProperty<T>(viewModel: ViewModel, name: String?) : SimpleSetProperty<T>(viewModel, name) {
    override fun bind(newObservable: ObservableValue<out ObservableSet<T>>?) {
        super.bind(newObservable)
        recordBinding(newObservable)
    }

    override fun bindContentBidirectional(list: ObservableSet<T>?) {
        super.bindContentBidirectional(list)
        recordBinding(list)
    }

    fun recordBinding(observableValue: Observable?) {
        if (observableValue != null) {
            ViewModel.propertyToFacade[observableValue] = this
            ViewModel.propertyToViewModel[observableValue] = bean as ViewModel
        }
    }

    /**
     * Return a unique id for this object instead of the default hashCode which is dependent on the children.
     * Without this override we wouldn't be able to identify the facade in our internal maps.
     */
    override fun hashCode() = System.identityHashCode(this)
    override fun equals(other: Any?) = this === other
}

class BindingAwareSimpleMapProperty<S, T>(viewModel: ViewModel, name: String?) : SimpleMapProperty<S, T>(viewModel, name) {
    override fun bind(newObservable: ObservableValue<out ObservableMap<S, T>>?) {
        super.bind(newObservable)
        recordBinding(newObservable)
    }

    override fun bindContentBidirectional(list: ObservableMap<S, T>?) {
        super.bindContentBidirectional(list)
        recordBinding(list)
    }

    fun recordBinding(observableValue: Observable?) {
        if (observableValue != null) {
            ViewModel.propertyToFacade[observableValue] = this
            ViewModel.propertyToViewModel[observableValue] = bean as ViewModel
        }
    }

    /**
     * Return a unique id for this object instead of the default hashCode which is dependent on the children.
     * Without this override we wouldn't be able to identify the facade in our internal maps.
     */
    override fun hashCode() = System.identityHashCode(this)
    override fun equals(other: Any?) = this === other
}

class BindingAwareSimpleFloatProperty(viewModel: ViewModel, name: String?) : SimpleFloatProperty(viewModel, name), BindingAwareProperty<Number> {

    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        recordBinding(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        recordBinding(other)
    }
}

class BindingAwareSimpleDoubleProperty(viewModel: ViewModel, name: String?) : SimpleDoubleProperty(viewModel, name), BindingAwareProperty<Number> {

    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        recordBinding(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        recordBinding(other)
    }
}

class BindingAwareSimpleLongProperty(viewModel: ViewModel, name: String?) : SimpleLongProperty(viewModel, name), BindingAwareProperty<Number> {

    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        recordBinding(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        recordBinding(other)
    }
}

class BindingAwareSimpleIntegerProperty(viewModel: ViewModel, name: String?) : SimpleIntegerProperty(viewModel, name), BindingAwareProperty<Number> {

    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        recordBinding(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        recordBinding(other)
    }
}