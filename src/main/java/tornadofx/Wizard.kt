package tornadofx

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import kotlin.reflect.KClass

abstract class Wizard(title: String? = null, heading: String? = null) : View(title) {
    val pages = FXCollections.observableArrayList<UIComponent>()

    private val currentPageProperty = SimpleObjectProperty<UIComponent>()
    private var currentPage by currentPageProperty

    private val hasNextProperty = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) < pages.size - 1 }
    private val currentPageComplete = SimpleBooleanProperty(false)

    private val canGoNext = hasNextProperty.and(currentPageComplete)
    private val canGoBack = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) > 0 }
    private val canFinish = booleanListBinding(pages) { complete }

    val backButtonTextProperty = SimpleStringProperty("< Back")
    val nextButtonTextProperty = SimpleStringProperty("Next >")
    val finishButtonTextProperty = SimpleStringProperty("Finish")
    val cancelButtonTextProperty = SimpleStringProperty("Cancel")

    val graphicProperty = SimpleObjectProperty<Node>()
    var graphic by graphicProperty

    open fun getNextPage() = pages.indexOf(currentPage) + 1
    open fun getPreviousPage() = pages.indexOf(currentPage) - 1

    fun next() {
        currentPage.onSave()
        if (currentPage.isComplete) {
            currentPage = pages[getNextPage()]
            root.scene.window.sizeToScene()
        }
    }

    fun back() {
        currentPage = pages[getPreviousPage()]
    }

    override val root = borderpane {
        addClass(WizardStyles.wizard)
        top {
            hbox {
                addClass(WizardStyles.header)
                vbox(5) {
                    label(titleProperty)
                    label(headingProperty) {
                        addClass(WizardStyles.heading)
                        visibleWhen { titleProperty.isEqualTo(headingProperty).not() }
                    }
                }
                spacer()
                label {
                    addClass(WizardStyles.graphic)
                    graphicProperty().bind(this@Wizard.graphicProperty)
                }
            }
        }
        center {
            stackpane {
                addClass(WizardStyles.content)
                currentPageProperty.onChange {
                    clear()
                    if (it != null) add(it)
                }
            }
        }
        // TODO: Optional list of wizard pages - if anybody needs it
/*
        left {
            stackpane {
                addClass(WizardStyles.stepInfo)
                vbox(5) {
                    bindChildren(pages) {
                        label(it.titleProperty) {
                            toggleClass(WizardStyles.bold, it.isDockedProperty)
                        }
                    }
                }
            }
        }
*/
        bottom {
            buttonbar {
                addClass(WizardStyles.buttons)
                button(type = ButtonBar.ButtonData.BACK_PREVIOUS) {
                    textProperty().bind(backButtonTextProperty)
                    enableWhen { canGoBack }
                    action { back() }
                }
                button(type = ButtonBar.ButtonData.NEXT_FORWARD) {
                    textProperty().bind(nextButtonTextProperty)
                    enableWhen { canGoNext }
                    action { next() }
                }
                button(type = ButtonBar.ButtonData.CANCEL_CLOSE) {
                    textProperty().bind(cancelButtonTextProperty)
                    action { close() }
                }
                button(type = ButtonBar.ButtonData.FINISH) {
                    textProperty().bind(finishButtonTextProperty)
                    enableWhen { canFinish }
                    action {
                        onSave()
                        if (isComplete) close()
                    }
                }
            }
        }
    }

    init {
        importStylesheet(WizardStyles::class)
        this@Wizard.heading = heading?: ""
        pages.onChange {
            if (currentPage == null && !pages.isEmpty()) currentPage = pages.first()
        }
        currentPageProperty.onChange {
            if (it != null) currentPageComplete.cleanBind(it.complete)
        }
    }

    companion object {
        fun open(wizard: KClass<out Wizard>, scope: Scope = Scope()) {
            find(wizard, scope).openModal()
        }
    }
}

class WizardStyles : Stylesheet() {
    companion object {
        val wizard by cssclass()
        val header by cssclass()
        val stepInfo by cssclass()
        val heading by cssclass()
        val graphic by cssclass()
        val content by cssclass()
        val buttons by cssclass()
        val bold by cssclass()
    }

    init {
        wizard {
            bold {
                fontWeight = FontWeight.BOLD
            }
            stepInfo {
                backgroundColor += Color.WHITE
                alignment = Pos.CENTER
            }
            header {
                label {
                    fontSize = 16.px
                    fontWeight = FontWeight.BOLD
                }
                heading {
                    fontSize = 12.px
                    fontWeight = FontWeight.NORMAL
                }
                padding = box(10.px)
                spacing = 10.px
                borderColor += box(Color.TRANSPARENT, Color.TRANSPARENT, Color.LIGHTGRAY, Color.TRANSPARENT)
                backgroundColor += Color.WHITE
                graphic {
                    prefWidth = 48.px
                    prefHeight = 48.px
                }
            }
            content {
                padding = box(10.px)
                borderColor += box(Color.TRANSPARENT, Color.TRANSPARENT, Color.LIGHTGRAY, Color.TRANSPARENT)
            }
            buttons {
                padding = box(10.px)
            }
        }
    }
}