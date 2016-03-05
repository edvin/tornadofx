package tornadofx

import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem


operator fun <T: MenuItem> Menu.plusAssign(menuItem: T): Unit {
    this.items += menuItem
}
operator fun MenuBar.plusAssign(menu: Menu): Unit {
    this.menus += menu
}
fun MenuBar.menu(name: String? = null, op: (Menu.() -> Unit)): Menu {
    val menu = Menu(name)
    menu.op()
    this += menu
    return menu
}
fun Menu.menu(name: String? = null, op: (Menu.() -> Unit)): Menu {
    val menu = Menu(name)
    menu.op()
    this += menu
    return menu
}

fun Menu.menuItem(name: String, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)): MenuItem {
    val menuItem = MenuItem(name,graphic);
    menuItem.setOnAction { onAction.invoke(it) }
    this += menuItem
    return menuItem
}