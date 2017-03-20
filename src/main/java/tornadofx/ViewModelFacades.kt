package tornadofx

import javafx.beans.property.*
import javafx.beans.value.ObservableValue

/**
 * A property wrapper that will report it's ViewModel relation
 * to a central location to make it possible for validators
 * to retrieve the correct ValidationContext from the ViewModel
 * this property is bound to.
 */
interface BindingAwareProperty<T> : Property<T> {
    fun bindPropertyToViewModel(observableValue: ObservableValue<*>?) {
        if (observableValue != null) {
            ViewModel.propertyToViewModel[observableValue] = bean as ViewModel
        }
    }
}

class BindingAwareSimpleBooleanProperty : SimpleBooleanProperty, BindingAwareProperty<Boolean> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Boolean) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Boolean>?) {
        super.bind(rawObservable)
        bindPropertyToViewModel(rawObservable)
    }

    override fun bindBidirectional(other: Property<Boolean>?) {
        super.bindBidirectional(other)
        bindPropertyToViewModel(other)
    }
}

class BindingAwareSimpleStringProperty : SimpleStringProperty, BindingAwareProperty<String> {
    constructor(viewModel: ViewModel, name: String?, initialValue: String) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out String>?) {
        super.bind(rawObservable)
        bindPropertyToViewModel(rawObservable)
    }

    override fun bindBidirectional(other: Property<String>?) {
        super.bindBidirectional(other)
        bindPropertyToViewModel(other)
    }
}

class BindingAwareSimpleObjectProperty<T> : SimpleObjectProperty<T>, BindingAwareProperty<T> {
    constructor(viewModel: ViewModel, name: String?, initialValue: T) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out T>?) {
        super.bind(rawObservable)
        bindPropertyToViewModel(rawObservable)
    }

    override fun bindBidirectional(other: Property<T>?) {
        super.bindBidirectional(other)
        bindPropertyToViewModel(other)
    }
}

class BindingAwareSimpleFloatProperty : SimpleFloatProperty, BindingAwareProperty<Number> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Float) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        bindPropertyToViewModel(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        bindPropertyToViewModel(other)
    }
}

class BindingAwareSimpleDoubleProperty : SimpleDoubleProperty, BindingAwareProperty<Number> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Double) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        bindPropertyToViewModel(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        bindPropertyToViewModel(other)
    }
}

class BindingAwareSimpleLongProperty : SimpleLongProperty, BindingAwareProperty<Number> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Long) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        bindPropertyToViewModel(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        bindPropertyToViewModel(other)
    }
}

class BindingAwareSimpleIntegerProperty : SimpleIntegerProperty, BindingAwareProperty<Number> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Int) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        bindPropertyToViewModel(rawObservable)
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        bindPropertyToViewModel(other)
    }
}