package tornadofx

import javafx.beans.binding.BooleanExpression
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight

abstract class Wizard(title: String? = null, heading: String? = null) : View(title), InstanceScoped {
    val pages = FXCollections.observableArrayList<UIComponent>()

    val currentPageProperty = SimpleObjectProperty<UIComponent>()
    var currentPage by currentPageProperty

    val hasNext = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) < pages.size - 1 }
    val hasPrevious = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) > 0 }
    val allPagesComplete: BooleanExpression get() = booleanListBinding(pages) { complete }

    open val canFinish: BooleanExpression = SimpleBooleanProperty(true)
    open val canGoNext: BooleanExpression = hasNext
    open val canGoBack: BooleanExpression = hasPrevious

    val stepsTextProperty = SimpleStringProperty("Steps")
    val backButtonTextProperty = SimpleStringProperty("< Back")
    val nextButtonTextProperty = SimpleStringProperty("Next >")
    val finishButtonTextProperty = SimpleStringProperty("Finish")
    val cancelButtonTextProperty = SimpleStringProperty("Cancel")

    val showStepsHeaderProperty = SimpleBooleanProperty(true)
    var showStepsHeader by showStepsHeaderProperty

    val showStepsProperty = SimpleBooleanProperty(true)
    var showSteps by showStepsProperty

    val enableStepLinksProperty = SimpleBooleanProperty(false)
    var enableStepLinks by enableStepLinksProperty

    val showHeaderProperty = SimpleBooleanProperty(true)
    var showHeader by showHeaderProperty

    val graphicProperty = SimpleObjectProperty<Node>()
    var graphic by graphicProperty

    open fun getNextPage() = pages.indexOf(currentPage) + 1
    open fun getPreviousPage() = pages.indexOf(currentPage) - 1

    fun next() {
        currentPage.onSave()
        if (currentPage.isComplete) {
            currentPage = pages[getNextPage()]
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
                removeWhen { showHeaderProperty.not() }
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
                bindChildren(pages) { page ->
                    val isPageActive = currentPageProperty.isEqualTo(page)
                    page.root.apply {
                        visibleWhen { isPageActive }
                    }
                }
            }
        }
        left {
            vbox {
                addClass(WizardStyles.stepInfo)
                removeWhen { showStepsProperty.not() }
                label(stepsTextProperty) {
                    addClass(WizardStyles.stepsHeading)
                    removeWhen { showStepsHeaderProperty.not() }
                }
                vbox(5) {
                    bindChildren(pages) { page ->
                        val isPageActive = currentPageProperty.isEqualTo(page)
                        hyperlink("${pages.indexOf(page) + 1}. ${page.title}") {
                            toggleClass(WizardStyles.bold, isPageActive)
                            action { currentPage = page }
                            enableWhen { enableStepLinksProperty }
                        }
                    }
                }
            }
        }
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
                        currentPage.onSave()
                        if (currentPage.isComplete) {
                            onSave()
                            if (isComplete) close()
                        }
                    }
                }
            }
        }
    }

    init {
        importStylesheet(WizardStyles::class)
        this.heading = heading ?: ""
    }
}

class WizardStyles : Stylesheet() {
    companion object {
        val wizard by cssclass()
        val header by cssclass()
        val stepInfo by cssclass()
        val stepsHeading by cssclass()
        val heading by cssclass()
        val graphic by cssclass()
        val content by cssclass()
        val buttons by cssclass()
        val bold by cssclass()
    }

    init {
        wizard {
            hyperlink {
                borderStyle += BorderStrokeStyle.NONE
                borderWidth += box(0.px)
                underline = false
            }
            hyperlink and visited {
                unsafe("-fx-text-fill", raw("-fx-accent"))

            }
            hyperlink and visited and hover {
                unsafe("-fx-text-fill", raw("-fx-accent"))
            }
            hyperlink and disabled {
                textFill = Color.BLACK
                opacity = 1.0
            }
            bold {
                fontWeight = FontWeight.BOLD
            }
            stepInfo {
                backgroundColor += Color.WHITE
                padding = box(15.px)
                stepsHeading {
                    fontWeight = FontWeight.BOLD
                    underline = true
                    padding = box(15.px, 0.px)
                }
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
            }
            buttons {
                padding = box(10.px)
                borderColor += box(Color.LIGHTGRAY, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
            }
        }
    }
}