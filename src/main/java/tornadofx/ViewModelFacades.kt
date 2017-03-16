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
    fun bindPropertyToViewModel(observableValue: ObservableValue<*>) {
        ViewModel.propertyToViewModel[observableValue] = bean as ViewModel
    }    
}

class BindingAwareSimpleBooleanProperty : SimpleBooleanProperty, BindingAwareProperty<Boolean> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Boolean) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Boolean>?) {
        super.bind(rawObservable)
        ViewModel.propertyToViewModel[rawObservable] = bean as ViewModel
    }

    override fun bindBidirectional(other: Property<Boolean>?) {
        super.bindBidirectional(other)
        ViewModel.propertyToViewModel[other] = bean as ViewModel
    }
}

class BindingAwareSimpleObjectProperty<T> : SimpleObjectProperty<T>, BindingAwareProperty<T> {
    constructor(viewModel: ViewModel, name: String?, initialValue: T) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out T>?) {
        super.bind(rawObservable)
        ViewModel.propertyToViewModel[rawObservable] = bean as ViewModel
    }

    override fun bindBidirectional(other: Property<T>?) {
        super.bindBidirectional(other)
        ViewModel.propertyToViewModel[other] = bean as ViewModel
    }
}

class BindingAwareSimpleFloatProperty : SimpleFloatProperty, BindingAwareProperty<Number> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Float) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        ViewModel.propertyToViewModel[rawObservable] = bean as ViewModel
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        ViewModel.propertyToViewModel[other] = bean as ViewModel
    }
}

class BindingAwareSimpleDoubleProperty : SimpleDoubleProperty, BindingAwareProperty<Number> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Double) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        ViewModel.propertyToViewModel[rawObservable] = bean as ViewModel
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        ViewModel.propertyToViewModel[other] = bean as ViewModel
    }
}

class BindingAwareSimpleIntegerProperty : SimpleIntegerProperty, BindingAwareProperty<Number> {
    constructor(viewModel: ViewModel, name: String?, initialValue: Int) : super(viewModel, name, initialValue)
    constructor(viewModel: ViewModel, name: String?) : super(viewModel, name)
    
    override fun bind(rawObservable: ObservableValue<out Number>?) {
        super.bind(rawObservable)
        ViewModel.propertyToViewModel[rawObservable] = bean as ViewModel
    }

    override fun bindBidirectional(other: Property<Number>?) {
        super.bindBidirectional(other)
        ViewModel.propertyToViewModel[other] = bean as ViewModel
    }
}


