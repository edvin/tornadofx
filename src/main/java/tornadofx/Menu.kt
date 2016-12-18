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
fun MenuBar.menu(name: String? = null, op: (Menu.() -> Unit)? = null): Menu {
    val menu = Menu(name)
    op?.invoke(menu)
    this += menu
    return menu
}

//ContextMenu extensions
fun ContextMenu.menu(name: String? = null, op: (Menu.() -> Unit)? = null): Menu {
    val menu = Menu(name)
    op?.invoke(menu)
    this += menu
    return menu
}
fun ContextMenu.menuitem(name: String, keyCombination: String, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)? = null): MenuItem {
    return this.menuitem(name, KeyCombination.valueOf(keyCombination),graphic,onAction)
}

fun ContextMenu.menuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)? = null): MenuItem {
    val menuItem = MenuItem(name,graphic);
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = this }
    onAction?.apply { menuItem.setOnAction { onAction.invoke(it) } }
    this += menuItem
    return menuItem
}

/**
 * Add a separator to the contextmenu
 */
fun ContextMenu.separator(op: (SeparatorMenuItem.()->Unit)? = null){
    val separator = SeparatorMenuItem()
    op?.invoke(separator)
    this+=separator
}

//Menu extensions
fun Menu.menu(name: String? = null, op: (Menu.() -> Unit)? = null): Menu {
    val menu = Menu(name)
    op?.invoke(menu)
    this += menu
    return menu
}
fun Menu.menuitem(name: String, keyCombination: String, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)? = null): MenuItem {
    return this.menuitem(name, KeyCombination.valueOf(keyCombination),graphic,onAction)
}

fun Menu.menuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: ((ActionEvent) -> Unit)? = null): MenuItem {
    val menuItem = MenuItem(name,graphic);
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = graphic }
    onAction?.apply { menuItem.setOnAction { this.invoke(it) } }
    this += menuItem
    return menuItem
}

fun Menu.separator() {
    this += SeparatorMenuItem()
}

fun Menu.radiomenuitem(name: String, toggleGroup: ToggleGroup? = null, keyCombination: KeyCombination?, graphic: Node? = null, op: (RadioMenuItem.() -> Unit)? = null): RadioMenuItem {
    val radioMenuItem = RadioMenuItem(name,graphic)
    toggleGroup?.apply { radioMenuItem.toggleGroup = this }
    keyCombination?.apply { radioMenuItem.accelerator = this }
    graphic?.apply { radioMenuItem.graphic = graphic }
    op?.let { it.invoke(radioMenuItem) }
    this += radioMenuItem
    return radioMenuItem
}

fun Menu.checkmenuitem(name: String, keyCombination: KeyCombination?, graphic: Node? = null, op: (CheckMenuItem.() -> Unit)? = null): CheckMenuItem {
    val checkMenuItem = CheckMenuItem(name,graphic)
    keyCombination?.apply { checkMenuItem.accelerator = this }
    graphic?.apply { checkMenuItem.graphic = graphic }
    op?.let { it.invoke(checkMenuItem) }
    this+= checkMenuItem
    return checkMenuItem
}

fun Control.contextmenu(op: (ContextMenu.() -> Unit)? = null): Node {
    val menu = ContextMenu()
    op?.invoke(menu)
    contextMenu = menu
    return this
}