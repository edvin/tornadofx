package tornadofx

import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCombination

//Menu-related operator functions
operator fun <T: MenuItem> Menu.plusAssign(menuItem: T): Unit {
    this.items += menuItem
}

operator fun MenuBar.plusAssign(menu: Menu): Unit {
    this.menus += menu
}

operator fun <T: MenuItem> ContextMenu.plusAssign(menuItem: T): Unit{
    this.items += menuItem
}

//MenuBar extensions
fun MenuBar.menu(name: String? = null, op: (Menu.() -> Unit)): Menu {
    val menu = Menu(name)
    menu.op()
    this += menu
    return menu
}

//ContextMenu extensions
fun ContextMenu.menu(name: String? = null, op: (Menu.() -> Unit)): Menu {
    val menu = Menu(name)
    menu.op()
    this += menu
    return menu
}
fun ContextMenu.menuitem(name: String, keyCombination: String, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)): MenuItem {
    return this.menuitem(name, KeyCombination.valueOf(keyCombination),graphic,onAction)
}

fun ContextMenu.menuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)): MenuItem {
    val menuItem = MenuItem(name,graphic);
    keyCombination?.let { menuItem.accelerator = it }
    graphic?.let { menuItem.graphic = graphic }
    menuItem.setOnAction { onAction.invoke(it) }
    this += menuItem
    return menuItem
}

//Menu extensions
fun Menu.menu(name: String? = null, op: (Menu.() -> Unit)): Menu {
    val menu = Menu(name)
    menu.op()
    this += menu
    return menu
}
fun Menu.menuitem(name: String, keyCombination: String, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)): MenuItem {
    return this.menuitem(name, KeyCombination.valueOf(keyCombination),graphic,onAction)
}

fun Menu.menuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)): MenuItem {
    val menuItem = MenuItem(name,graphic);
    keyCombination?.let { menuItem.accelerator = it }
    graphic?.let { menuItem.graphic = graphic }
    menuItem.setOnAction { onAction.invoke(it) }
    this += menuItem
    return menuItem
}

fun Menu.separator() {
    this += SeparatorMenuItem()
}

fun Menu.radiomenuitem(name: String, keyCombination: KeyCombination?, graphic: Node? = null, op: (RadioMenuItem.() -> Unit)): RadioMenuItem {
    val radioMenuItem = RadioMenuItem(name,graphic)
    keyCombination?.let { radioMenuItem.accelerator = it }
    graphic?.let { radioMenuItem.graphic = graphic }
    radioMenuItem.op()
    this += radioMenuItem
    return radioMenuItem
}
fun Menu.checkmenuitem(name: String, keyCombination: KeyCombination?, graphic: Node? = null, op: (CheckMenuItem.() -> Unit)): CheckMenuItem {
    val checkMenuItem = CheckMenuItem(name,graphic)
    keyCombination?.let { checkMenuItem.accelerator = it }
    graphic?.let { checkMenuItem.graphic = graphic }
    checkMenuItem.op()
    this+= checkMenuItem
    return checkMenuItem
}