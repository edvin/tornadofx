package tornadofx.testapps

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.util.StringConverter
import tornadofx.*
import java.util.*

class ComboBoxTestApp : App(ComboboxTest::class)

class ComboboxTest : View("Combobox test") {
    val items = listOf("Item 1", "Item 2", "Item 3")
    val selectedItem = SimpleStringProperty(items.first())

    override val root = stackpane {
        setPrefSize(400.0, 400.0)
        combobox(selectedItem, items) {
            cellFormat {
                text = it
            }
        }
    }
}

class AutoCompleteComboBoxExtensionTestApp : App(AutoCompleteComboBoxExtensionTest::class)

class AutoCompleteComboBoxExtensionTest : View("AutoComplete comboBox extension test") {
    val itemsGlobalObject = Locale.getISOCountries().asList().map { Locale("", it) }
    val itemsGlobal = itemsGlobalObject.map { it.displayCountry }
    val selectedItem = SimpleStringProperty(itemsGlobal.first())
    val selectedItem2 = SimpleStringProperty(itemsGlobal.first())
    val selectedItem3 = SimpleStringProperty(itemsGlobal.first())
    val selectedItemObject = SimpleObjectProperty(itemsGlobalObject.first())
    val selectedItemObject2 = SimpleObjectProperty(itemsGlobalObject.first())
    val selectedItemObject3 = SimpleObjectProperty(itemsGlobalObject.first())

    val selectedItemE = SimpleStringProperty(itemsGlobal.first())
    val selectedItem2E = SimpleStringProperty(itemsGlobal.first())
    val selectedItemObjectE = SimpleObjectProperty(itemsGlobalObject.first())
    val selectedItemObject2E = SimpleObjectProperty(itemsGlobalObject.first())
    val selectedItemObject3E = SimpleObjectProperty(itemsGlobalObject.first())

    override val root = form {
        setPrefSize(400.0, 400.0)
        fieldset {
            field("Default") {
                combobox(selectedItem, itemsGlobal) {
                    makeAutocompletable()
                }
                textfield(selectedItem)
                useMaxSize = true
            }
            /**
             * Example using automatic popup width
             */
            field("With automatic popup width") {
                combobox(selectedItem2, itemsGlobal) {
                    makeAutocompletable(automaticPopupWidth = true)
                }
                textfield(selectedItem2)
                useMaxSize = true
            }
            /**
             * Example using custom filter using startswith instead of contains
             */
            field("With custom Filter") {
                combobox(selectedItem3, itemsGlobal) {
                    makeAutocompletable {
                        itemsGlobal.filter { current -> converter.toString(current).startsWith(it, true) }
                    }
                }
                textfield(selectedItem3)
                useMaxSize = true
            }
            /**
             * Example using converter
             */
            field("Default with custom converter") {
                combobox(selectedItemObject, itemsGlobalObject) {
                    converter = LocaleStringConverter()
                    makeAutocompletable()
                }
                label(selectedItemObject)
                useMaxSize = true
            }
            /**
             * Example using converter and custom filter
             */
            field("With custom converter and filter") {
                combobox(selectedItemObject2, itemsGlobalObject) {
                    converter = LocaleStringConverter()
                    makeAutocompletable {
                        itemsGlobalObject.observable().filtered { current -> current.displayCountry.contains(it, true) || current.isO3Country.contains(it, true) || current.country.contains(it, true) }
                    }
                }
                label(selectedItemObject2)
                useMaxSize = true
            }
            /**
             * Example using custom cell factory
             */
            field("With custom cell factory") {
                combobox(selectedItemObject3, itemsGlobalObject) {
                    converter = LocaleStringConverter()
                    cellFormat {
                        text = "Locale: " + converter.toString(it)
                    }
                    makeAutocompletable {
                        itemsGlobalObject.observable().filtered { current -> current.displayCountry.contains(it, true) || current.isO3Country.contains(it, true) || current.country.contains(it, true) }
                    }
                }
                label(selectedItemObject3)
            }
            field("Editable Default") {
                combobox(selectedItemE, itemsGlobal) {
                    val tmpValue = value
                    isEditable = true
                    value = tmpValue
                    makeAutocompletable()
                }
                textfield(selectedItemE)
            }
            /**
             * Example using custom filter using startswith instead of contains
             */
            field("Editable With custom Filter") {
                combobox(selectedItem2E, itemsGlobal) {
                    val tmpValue = value
                    isEditable = true
                    value = tmpValue
                    makeAutocompletable {
                        itemsGlobal.filter { current -> converter.toString(current).startsWith(it, true) }
                    }
                }
                textfield(selectedItem2E)
            }
            /**
             * Example using converter
             */
            field("Editable Default with custom converter") {
                combobox(selectedItemObjectE, itemsGlobalObject) {
                    val tmpValue = value
                    isEditable = true
                    value = tmpValue
                    converter = LocaleStringConverter()
                    makeAutocompletable()
                }
                label(selectedItemObjectE)
            }
            /**
             * Example using converter and custom filter
             */
            field("Editable With custom converter and filter") {
                combobox(selectedItemObject2E, itemsGlobalObject) {
                    val tmpValue = value
                    isEditable = true
                    value = tmpValue
                    converter = LocaleStringConverter()
                    makeAutocompletable {
                        itemsGlobalObject.observable().filtered { current -> current.displayCountry.contains(it, true) || current.isO3Country.contains(it, true) || current.country.contains(it, true) }
                    }
                }
                label(selectedItemObject2E)
            }
            /**
             * Example using custom cell factory
             */
            field("Editable With custom cell factory") {
                combobox(selectedItemObject3E, itemsGlobalObject) {
                    val tmpValue = value
                    isEditable = true
                    value = tmpValue
                    converter = LocaleStringConverter()
                    cellFormat {
                        text = "Locale: " + converter.toString(it)
                    }
                    makeAutocompletable {
                        itemsGlobalObject.observable().filtered { current -> current.displayCountry.contains(it, true) || current.isO3Country.contains(it, true) || current.country.contains(it, true) }
                    }
                }
                label(selectedItemObject3E)
            }
        }
    }
}

class LocaleStringConverter : StringConverter<Locale>() {
    val mapLocale = hashMapOf<String, Locale>()
    override fun fromString(string: String?): Locale? {
        return if (string == null) null else mapLocale[string]
    }

    override fun toString(locale: Locale?): String? {
        val output = locale?.displayCountry + ", " + locale?.isO3Country
        if (locale != null && !mapLocale.containsKey(output)) mapLocale.put(output, locale)
        return output
    }
}
