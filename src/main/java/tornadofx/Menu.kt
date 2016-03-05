package tornadofx

import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.*


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

fun Menu.separator() {
    this += SeparatorMenuItem()
}

fun Menu.radioMenuItem(name: String? = null, graphic: Node? = null, op: (RadioMenuItem.() -> Unit)): RadioMenuItem {
    val radioMenuItem = RadioMenuItem(name,graphic)
    radioMenuItem.op()
    this += radioMenuItem
    return radioMenuItem
}
fun Menu.checkMenuItem(name: String? = null, graphic: Node? = null, op: (CheckMenuItem.() -> Unit)): CheckMenuItem {
    val checkMenuItem = CheckMenuItem(name,graphic)
    checkMenuItem.op()
    this+= checkMenuItem
    return checkMenuItem
}