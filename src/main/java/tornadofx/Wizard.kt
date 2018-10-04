package tornadofx

import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Parent
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

    val pages: ObservableList<UIComponent> = FXCollections.observableArrayList()

    val currentPageProperty: ObjectProperty<UIComponent> = SimpleObjectProperty()
    var currentPage: UIComponent by currentPageProperty

    val enterProgressesProperty: BooleanProperty = SimpleBooleanProperty(false)
    var enterProgresses: Boolean by enterProgressesProperty

    val hasNext: BooleanBinding = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) < pages.size - 1 }
    val hasPrevious: BooleanBinding = booleanBinding(currentPageProperty, pages) { value != null && pages.indexOf(value) > 0 }
    val allPagesComplete: BooleanExpression get() = booleanListBinding(pages) { complete }

    val currentPageComplete: BooleanExpression = SimpleBooleanProperty(false) // TODO Review type

    override val complete: BooleanProperty = SimpleBooleanProperty(false)
    private val completeListeners = mutableListOf<() -> Unit>()

    open val canFinish: BooleanExpression = SimpleBooleanProperty(true)
    open val canGoNext: BooleanExpression = hasNext
    open val canGoBack: BooleanExpression = hasPrevious

    val stepsTextProperty: StringProperty = SimpleStringProperty(wizardBundle["steps"])
    val backButtonTextProperty: StringProperty = SimpleStringProperty(wizardBundle["back"])
    val nextButtonTextProperty: StringProperty = SimpleStringProperty(wizardBundle["next"])
    val finishButtonTextProperty: StringProperty = SimpleStringProperty(wizardBundle["finish"])
    val cancelButtonTextProperty: StringProperty = SimpleStringProperty(wizardBundle["cancel"])

    val showStepsHeaderProperty: BooleanProperty = SimpleBooleanProperty(true)
    var showStepsHeader: Boolean by showStepsHeaderProperty

    val stepLinksCommitsProperty: BooleanProperty = SimpleBooleanProperty(true)
    var stepLinksCommits: Boolean by stepLinksCommitsProperty

    val showStepsProperty: BooleanProperty = SimpleBooleanProperty(true)
    var showSteps: Boolean by showStepsProperty

    val numberedStepsProperty: BooleanProperty = SimpleBooleanProperty(true)
    var numberedSteps: Boolean by numberedStepsProperty

    val enableStepLinksProperty: BooleanProperty = SimpleBooleanProperty(false)
    var enableStepLinks: Boolean by enableStepLinksProperty

    val showHeaderProperty: BooleanProperty = SimpleBooleanProperty(true)
    var showHeader: Boolean by showHeaderProperty

    val graphicProperty: ObjectProperty<Node> = SimpleObjectProperty()
    var graphic: Node by graphicProperty

    override val root: Parent = borderpane {
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
                    action { onCancel() }
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

    init {
        importStylesheet<WizardStyles>()
        this.heading = heading.orEmpty()
        currentPageProperty.addListener { _, oldPage, newPage ->
            if (newPage != null) {
                (currentPageComplete as BooleanProperty).bind(newPage.complete)
                runLater {
                    newPage.root.lookupAll("*").find { it.isFocusTraversable }?.requestFocus()
                    newPage.callOnDock()
                }
            }
            oldPage?.callOnUndock()
        }

        runLater {
            // For when the instance is created
            currentStage?.setOnCloseRequest {
                it.consume()
                onCancel()
            }
            // For when the instance is reused and the stage is changed
            root.sceneProperty().select { it.windowProperty() }.onChange {
                it?.setOnCloseRequest {
                    it.consume()
                    onCancel()
                }
            }
        }
    }

    open fun getNextPage(): Int = pages.indexOf(currentPage) + 1
    open fun getPreviousPage(): Int = pages.indexOf(currentPage) - 1

    fun next() {
        currentPage.onSave()
        if (currentPage.isComplete) {
            currentPage = pages[getNextPage()]
        }
    }

    fun back() {
        currentPage = pages[getPreviousPage()]
    }

    fun cancel(): Unit = close()

    open fun onCancel(): Unit = cancel()

    fun onComplete(resultListener: () -> Unit) {
        completeListeners.add(resultListener)
    }

    fun onComplete(resultListener: Runnable): Unit = onComplete { resultListener.run() }

    override fun onSave() {
        super.onSave()
        isComplete = true
    }

    override fun onDock() {
        complete.onChange {
            if (it) completeListeners.withEach { this() }
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
        val wizard: CssRule by cssclass()
        val header: CssRule by cssclass()
        val stepInfo: CssRule by cssclass()
        val stepsHeading: CssRule by cssclass()
        val heading: CssRule by cssclass()
        val graphic: CssRule by cssclass()
        val content: CssRule by cssclass()
        val buttons: CssRule by cssclass()
        val bold: CssRule by cssclass()
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
