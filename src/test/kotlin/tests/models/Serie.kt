package tests.models

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.JsonModel
import javax.json.JsonObject

class Serie : JsonModel {
    val id = SimpleIntegerProperty()
    val title = SimpleStringProperty()

    override fun updateModel(json: JsonObject) {
        id.value = json.getInt("id")
        title.value = json.getString("title")
    }

}