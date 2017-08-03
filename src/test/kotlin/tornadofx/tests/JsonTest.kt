package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import org.junit.Assert
import org.junit.Test
import tornadofx.*
import java.time.LocalDate

class JsonTest {

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

    class AutoPerson2 (
        firstName: String? = null,
        lastName: String? = null,
        dob: LocalDate? = null,
        type: Int? = null,
        global: Boolean? = null
    ): JsonModelAuto {

        var firstName by property(firstName)
        fun firstNameProperty() = getProperty(AutoPerson2::firstName)

        var lastName by property(lastName)
        fun lastNameProperty() = getProperty(AutoPerson2::lastName)

        var dob by property(dob)
        fun dobProperty() = getProperty(AutoPerson2::dob)

        var type by property(type)
        fun typeProperty() = getProperty(AutoPerson2::type)

        var global by property(global)
        fun globalProperty() = getProperty(AutoPerson2::global)
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

    @Test
    fun automodelRoundTrip() {
        val writtenP = AutoPerson().apply {
            firstName = "John"
            lastName = "Doe"
            dob = LocalDate.of(1970, 6, 12)
            type = 42
            global = true
        }
        val j = writtenP.toJSON().toString()
        val readP = loadJsonModel<AutoPerson>(j)
        val rewrittenJ = readP.toJSON().toString()

        Assert.assertEquals("John", readP.firstName)
        Assert.assertEquals("Doe", readP.lastName)
        Assert.assertEquals(LocalDate.of(1970, 6, 12), readP.dob)
        Assert.assertEquals(42, readP.type)
        Assert.assertEquals(true, readP.global)
        Assert.assertEquals(j, rewrittenJ)

    }

    @Test
    fun automodel2() {
        val p = AutoPerson2().apply {
            firstName = "John"
            lastName = "Doe"
            dob = LocalDate.of(1970, 6, 12)
            type = 42
            global = true
        }
        val json = """{"dob":"1970-06-12","firstName":"John","global":true,"lastName":"Doe","type":42}"""
        Assert.assertEquals(json, p.toJSON().toString())
        val l = loadJsonModel<AutoPerson2>(json)
        Assert.assertEquals("John", l.firstName)
        Assert.assertEquals("Doe", l.lastName)
        Assert.assertEquals(LocalDate.of(1970, 6, 12), l.dob)
        Assert.assertEquals(42, l.type)
        Assert.assertEquals(true, l.global)
        Assert.assertEquals(json, l.toJSON().toString())
    }

    @Test
    fun automodelRoundTrip2() {
        val writtenP = AutoPerson2().apply {
            firstName = "John"
            lastName = "Doe"
            dob = LocalDate.of(1970, 6, 12)
            type = 42
            global = true
        }
        val j = writtenP.toJSON().toString()
        val readP = loadJsonModel<AutoPerson2>(j)
        val rewrittenJ = readP.toJSON().toString()

        Assert.assertEquals("John", readP.firstName)
        Assert.assertEquals("Doe", readP.lastName)
        Assert.assertEquals(LocalDate.of(1970, 6, 12), readP.dob)
        Assert.assertEquals(42, readP.type)
        Assert.assertEquals(true, readP.global)
        Assert.assertEquals(j, rewrittenJ)

    }

    @Test
    fun firstAvailable() {
        val json = loadJsonObject("""{"dob":"1970-06-12","firstName":"John","global":true,"lastName":"Doe","type":42}""")
        val dob = json.date("date_of_birth", "dob")
        Assert.assertEquals(LocalDate.of(1970, 6, 12),dob)
    }
}
