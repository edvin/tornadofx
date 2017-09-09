package tornadofx.testapps

import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import tornadofx.*
import tornadofx.tests.Customer
import tornadofx.tests.CustomerModel

class WizardTestApp : WorkspaceApp(WizardCustomerList::class)

class CustomerWizard : Wizard("Create customer", "Provide customer information") {
    val customer: CustomerModel by inject()

    override val canGoNext = currentPageComplete
    override val canFinish = allPagesComplete

    init {
        add<WizardStep1>()
        add<WizardStep2>()
        customer.item = Customer()
        graphic = resources.imageview("/tornadofx/tests/person.png")
        showSteps = true
        stepLinksCommits = true
        showHeader = true
        numberedSteps = true
        showStepsHeader = false
        enableStepLinks = true
        enterProgresses = true
    }
}

class WizardStep1 : View("Customer Data") {
    val customer: CustomerModel by inject()
    val wizard: CustomerWizard by inject()

    override val complete = customer.valid(customer.name)

    override val root = form {
        fieldset(title) {
            field("Type") {
                combobox(customer.type, Customer.Type.values().toList())
            }
            field("Name") {
                textfield(customer.name) {
                    required()
                    // Make sure we get focus instead of the type combo
                    whenDocked { requestFocus() }
                }
            }
            hbox {
                alignment = Pos.BASELINE_RIGHT
                hyperlink("Skip") {
                    action {
                        wizard.currentPage = wizard.pages.last()
                    }
                }
            }
        }
    }
}

class WizardStep2 : View("Address") {
    val customer: CustomerModel by inject()
    override val complete = customer.valid(customer.zip, customer.city)

    override val root = form {
        fieldset(title) {
            field("Zip/City") {
                textfield(customer.zip) {
                    prefColumnCount = 5
                    required()
                }
                textarea(customer.city) {
                    required()
                }
            }
        }
    }
}

class WizardCustomerList : View() {
    val customers = FXCollections.observableArrayList<Customer>()

    override val root = tableview(customers) {
        column("Name", Customer::nameProperty)
        columnResizePolicy = SmartResize.POLICY
    }

    override fun onDock() {
        with(workspace) {
            button("_Add").action {
                find<CustomerWizard> {
                    onComplete { customers.add(customer.item) }
                    openModal()
                }
            }
        }
    }
}