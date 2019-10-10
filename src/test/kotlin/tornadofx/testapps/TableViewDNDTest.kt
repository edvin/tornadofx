package tornadofx.testapps

import javafx.scene.control.TableRow
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import tornadofx.tests.Person
import tornadofx.tests.PersonRef

class DNDReorderTableViewApp : App(DNDReorderTableView::class)

class DNDReorderTableView : View() {
    val people = observableListOf(Person("Bob", 42), Person("Jane", 24), Person("John", 31))

    override val root = tableview(people) {
        column("Name", Person::nameProperty)
        column("Age", Person::ageProperty)

        setOnDragDetected { event ->
            val person = if (!selectionModel.isEmpty) selectedItem else null
            if (person != null) {
                val dragboard = startDragAndDrop(TransferMode.MOVE)
                dragboard.setContent(ClipboardContent().apply { put(Person.DATA_FORMAT, PersonRef(person.id)) })
            }
            event.consume()
        }

        // Handle DND per row
        setRowFactory {
            object : TableRow<Person>() {
                init {
                    setOnDragOver {
                        if (it.dragboard.hasContent(Person.DATA_FORMAT)) {
                            it.acceptTransferModes(TransferMode.MOVE)
                            it.consume()
                        }
                    }
                    setOnDragEntered {
                        if (it.dragboard.hasContent(Person.DATA_FORMAT)) {
                            val ref = it.dragboard.getContent(Person.DATA_FORMAT) as PersonRef
                            style {
                                if (ref.id != item.id) {
                                    fontWeight = FontWeight.BOLD
                                } else {
                                    textFill = Color.RED
                                }
                            }
                            it.consume()
                        }
                    }
                    setOnDragExited {
                        style = null
                        it.consume()
                    }
                    setOnDragDropped {
                        if (it.dragboard.hasContent(Person.DATA_FORMAT)) {
                            val draggedRef = it.dragboard.getContent(Person.DATA_FORMAT) as PersonRef
                            val droppedPerson = item
                            val targetIndex = items.indexOf(droppedPerson)
                            val sourcePerson = items.find { it.id == draggedRef.id }
                            items.move(sourcePerson, targetIndex)
                            it.isDropCompleted = true
                        } else {
                            it.isDropCompleted = false
                        }
                        it.consume()
                    }
                }
            }
        }
    }
}