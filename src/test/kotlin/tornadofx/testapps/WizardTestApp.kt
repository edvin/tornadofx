package tornadofx.testapps

import javafx.geometry.Pos
import javafx.scene.control.Alert
import tornadofx.*
import tornadofx.tests.Customer
import tornadofx.tests.CustomerModel

class WizardTestApp : App(WizardTestView::class)
class WizardWorkspaceApp : WorkspaceApp(CustomerWizard::class)

class WizardTestView : View("Wizard Test") {
    override val root = stackpane {
        paddingAll = 100
        button("Create customer").action { find<CustomerWizard>().openModal() }
    }
}

class CustomerWizard : Wizard("Create customer", "Provide customer information") {
    val customer: CustomerModel by inject()

    override val canGoNext = currentPageComplete
    override val canFinish = allPagesComplete

    init {
        add(WizardStep1::class)
        add(WizardStep2::class)
        customer.item = Customer()
        graphic = imageview(resources.url("/tornadofx/tests/person.png").toExternalForm(), false)
        showSteps = true
        stepLinksCommits = true
        showHeader = true
        numberedSteps = true
        showStepsHeader = false
        enableStepLinks = true
    }

    override fun onSave() {
        alert(Alert.AlertType.INFORMATION, "Saving customer", "Collected data: ${customer.item}")
        super.onSave()
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
                    prefColumnCount = 7
                    required()
                }
                textfield(customer.city) {
                    required()
                }
            }
        }
    }
}
