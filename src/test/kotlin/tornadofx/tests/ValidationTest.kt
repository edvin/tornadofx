package tornadofx.tests

import javafx.scene.control.TextField
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.FX
import tornadofx.ValidationContext
import tornadofx.ValidationMessage
import tornadofx.ValidationSeverity
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidationTest {
    val primaryStage = FxToolkit.registerPrimaryStage()
    val context = ValidationContext()

    @Test fun simple_validation() {
        val input = TextField()

        val validator = context.addValidator(input, input.textProperty()) {
            if (it!!.length < 5) error("Too short") else null
        }

        // Simulate user input
        FX.runAndWait {
            input.text = "abc"

            // Validation should fail
            assertFalse(validator.validate())

            // Extract the validation result
            val result = validator.result

            // The severity should be error
            assertTrue(result is ValidationMessage && result.severity == ValidationSeverity.Error)

            // Confirm valid input passes validation
            input.text = "longvalue"
            assertTrue(validator.validate())
            assertNull(validator.result)
        }
    }
}