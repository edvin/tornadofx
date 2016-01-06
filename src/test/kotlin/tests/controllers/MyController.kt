package tests.controllers

import javafx.collections.ObservableList
import tests.models.Serie
import tornadofx.Controller
import tornadofx.Rest
import tornadofx.list
import tornadofx.toModel

class MyController : Controller() {
    val api : Rest by inject()

    fun getMessage(): ObservableList<Serie> {
        return api.get("series").list().toModel()
    }
}