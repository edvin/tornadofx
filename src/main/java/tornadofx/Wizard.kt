@file:Suppress("unused")

package tornadofx

import javafx.application.Platform
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import java.util.*

abstract class Wizard @JvmOverloads constructor(title: String? = null, heading: String? = null) : View(title) {
    private val wizardBundle: ResourceBundle = ResourceBundle.getBundle("tornadofx/i18n/Wizard")

    val pages: ObservableList<UIComponent> = FXCollections.observableArrayList<UIComponent>()

    val currentPageProperty = SimpleObjectProperty<UIComponent>()
    var currentPage: UIComponent by currentPageProperty

    val enterProgressesProperty: BooleanProperty = SimpleBooleanProperty(false)
    var enterProgresses by enterProgressesProperty

    val hasNext = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) < pages.size - 1 }
    val hasPrevious = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) > 0 }
    val allPagesComplete: BooleanExpression get() = booleanListBinding(pages) { complete }

    val currentPageComplete: BooleanExpression = SimpleBooleanProperty(false)

    override val complete = SimpleBooleanProperty(false)

    open val canFinish: BooleanExpression = SimpleBooleanProperty(true)
    open val canGoNext: BooleanExpression = hasNext
    open val canGoBack: BooleanExpression = hasPrevious

    val stepsTextProperty = SimpleStringProperty(wizardBundle["steps"])
    val backButtonTextProperty = SimpleStringProperty(wizardBundle["back"])
    val nextButtonTextProperty = SimpleStringProperty(wizardBundle["next"])
    val finishButtonTextProperty = SimpleStringProperty(wizardBundle["finish"])
    val cancelButtonTextProperty = SimpleStringProperty(wizardBundle["cancel"])

    val showStepsHeaderProperty = SimpleBooleanProperty(true)
    var showStepsHeader by showStepsHeaderProperty

    val stepLinksCommitsProperty = SimpleBooleanProperty(true)
    var stepLinksCommits by stepLinksCommitsProperty

    val showStepsProperty = SimpleBooleanProperty(true)
    var showSteps by showStepsProperty

    val numberedStepsProperty = SimpleBooleanProperty(true)
    var numberedSteps by numberedStepsProperty

    val enableStepLinksProperty = SimpleBooleanProperty(false)
    var enableStepLinks by enableStepLinksProperty

    val showHeaderProperty = SimpleBooleanProperty(true)
    var showHeader by showHeaderProperty

    val graphicProperty = SimpleObjectProperty<Node>()
    var graphic: Node by graphicProperty

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
                removeWhen(showHeaderProperty.not())
                vbox(5) {
                    label(titleProperty)
                    label(headingProperty) {
                        addClass(WizardStyles.heading)
                        visibleWhen(titleProperty.isEqualTo(headingProperty).not())
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

                        hyperlink("") {
                            textProperty().bind(stringBinding(numberedStepsProperty) { "${if (numberedSteps) (pages.indexOf(page) + 1).toString() + ". " else ""}${page.title}" })
                            toggleClass(WizardStyles.bold, isPageActive)
                            action {
                                if (stepLinksCommits && pages.indexOf(page) > pages.indexOf(currentPage)) {
                                    currentPage.onSave()
                                    if (currentPage.isComplete) currentPage = page
                                } else {
                                    currentPage = page
                                }
                            }
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
                    runLater {
                        enableWhen(canGoBack)
                    }
                    action { back() }
                }
                button(type = ButtonBar.ButtonData.NEXT_FORWARD) {
                    textProperty().bind(nextButtonTextProperty)
                    runLater {
                        enableWhen(canGoNext.and(hasNext))
                    }
                    action { next() }
                }
                button(type = ButtonBar.ButtonData.CANCEL_CLOSE) {
                    textProperty().bind(cancelButtonTextProperty)
                    action { close() }
                }
                button(type = ButtonBar.ButtonData.FINISH) {
                    textProperty().bind(finishButtonTextProperty)
                    runLater {
                        enableWhen(canFinish)
                    }
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

    private val completeListeners = arrayListOf<() -> Unit>()

    fun onComplete(resultListener: () -> Unit) {
        completeListeners.add(resultListener)
    }

    fun onComplete(resultListener: Runnable) {
        completeListeners.add({ resultListener.run() })
    }

    override fun onSave() {
        super.onSave()
        isComplete = true
    }

    init {
        importStylesheet<WizardStyles>()
        this.heading = heading ?: ""
        currentPageProperty.addListener { _, oldPage, newPage ->
            if (newPage != null) {
                (currentPageComplete as BooleanProperty).bind(newPage.complete)
                Platform.runLater {
                    newPage.root.lookupAll("*").find { it.isFocusTraversable }?.requestFocus()
                    newPage.callOnDock()
                }
            }
            oldPage?.callOnUndock()
        }
    }

    override fun onDock() {
        complete.onChange {
            if (it) completeListeners.forEach { it() }
        }

        // Enter completes current page and goes to next, finishes on last
        root.addEventFilter(KeyEvent.KEY_PRESSED) {
            if (enterProgresses && it.code == KeyCode.ENTER) {
                if (it.target is TextArea && !it.isControlDown) return@addEventFilter

                if (allPagesComplete.value) {
                    currentPage.onSave()
                    onSave()
                    close()
                } else if (currentPageComplete.value && canGoNext.value) {
                    next()
                }
            }
        }
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