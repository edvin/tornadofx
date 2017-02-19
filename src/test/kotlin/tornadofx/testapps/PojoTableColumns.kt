package tornadofx.testapps

import javafx.collections.FXCollections
import tornadofx.*
import tornadofx.tests.JavaPerson

class PojoTableColumnsApp : App(PojoTableColumns::class)

class PojoTableColumns : View("Pojo Table Columns") {
    val people = FXCollections.observableArrayList(
            JavaPerson().apply { name = "John Doe"; primaryEmail = "john@doe.com"; secondaryEmail = "john.doe@gmail.com"},
            JavaPerson().apply { name = "Jane Doe"; primaryEmail = "jane@doe.com"; secondaryEmail = "jane.doe@gmail.com"}
    )

    override val root = tableview(people) {
        column("Name", JavaPerson::getName )
        nestedColumn("Email addresses") {
            column("Primary Email", JavaPerson::getPrimaryEmail )
            column("Secondary Email", JavaPerson::getSecondaryEmail )
        }
        resizeColumnsToFitContent()
    }
}