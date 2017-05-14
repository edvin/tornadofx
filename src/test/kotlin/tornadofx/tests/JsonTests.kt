package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import org.junit.Assert
import org.junit.Test
import tornadofx.*
import java.time.LocalDate

class JsonTests {

    class AutoPerson : JsonModelAuto {
        val firstNameProperty = SimpleStringProperty()
        var firstName by firstNameProperty

        var lastName by property<String>()
        fun lastNameProperty() = getProperty(AutoPerson::lastName)

        var dob by property<LocalDate>()
        fun dobProperty() = getProperty(AutoPerson::dob)

        var type: Int? = null
        var global = false
    }

    @Test
    fun automodel() {
        val p = AutoPerson().apply {
            firstName = "John"
            lastName = "Doe"
            dob = LocalDate.of(1970, 6, 12)
            type = 42
            global = true
        }
        val json = """{"dob":"1970-06-12","firstName":"John","global":true,"lastName":"Doe","type":42}"""
        Assert.assertEquals(json, p.toJSON().toString())
        val l = loadJsonModel<AutoPerson>(json)
        Assert.assertEquals("John", l.firstName)
        Assert.assertEquals("Doe", l.lastName)
        Assert.assertEquals(LocalDate.of(1970, 6, 12), l.dob)
        Assert.assertEquals(42, l.type)
        Assert.assertEquals(true, l.global)
        Assert.assertEquals(json, l.toJSON().toString())
    }
}
