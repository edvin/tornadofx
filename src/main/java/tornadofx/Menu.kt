package tornadofx

import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import tornadofx.Stylesheet.Companion.contextMenu
import tornadofx.WizardStyles.Companion.graphic

//Menu-related operator functions
operator fun <T : MenuItem> Menu.plusAssign(menuItem: T) {
    this.items += menuItem
}

operator fun MenuBar.plusAssign(menu: Menu) {
    this.menus += menu
}

operator fun <T : MenuItem> ContextMenu.plusAssign(menuItem: T) {
    this.items += menuItem
}

//MenuBar extensions
fun MenuBar.menu(name: String? = null, graphic: Node? = null, op: Menu.() -> Unit = {}): Menu {
    val menu = Menu(name, graphic).also(op)
    this += menu
    return menu
}

//ContextMenu extensions
fun ContextMenu.menu(name: String? = null, op: Menu.() -> Unit = {}): Menu {
    val menu = Menu(name).also(op)
    this += menu
    return menu
}

/**
 * Create a MenuItem. The op block will be configured as the `setOnAction`. This will be deprecated in favor of the `item` call, where the
 * op block operates on the MenuItem. This deprecation was made to align the menuitem builder with the other builders.
 */
@Deprecated("Use the item builder instead, which expects an action parameter", ReplaceWith("item(name, KeyCombination.valueOf(keyCombination), graphic).action(onAction)"))
fun ContextMenu.menuitem(name: String, keyCombination: String, graphic: Node? = null, onAction: () -> Unit = {}): MenuItem {
    return this.item(name, KeyCombination.valueOf(keyCombination), graphic).apply { action(onAction) }
}

fun ContextMenu.checkmenuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: CheckMenuItem.() -> Unit = {}): CheckMenuItem {
    val checkMenuItem = CheckMenuItem(name, graphic)
    keyCombination?.apply { checkMenuItem.accelerator = this }
    graphic?.apply { checkMenuItem.graphic = graphic }
    op(checkMenuItem)
    this += checkMenuItem
    return checkMenuItem
}

/**
 * Create a MenuItem. The op block will be configured as the `setOnAction`. This will be deprecated in favor of the `item` call, where the
 * op block operates on the MenuItem. This deprecation was made to align the menuitem builder with the other builders.
 */
@Deprecated("Use the item builder instead, which expects an action parameter", ReplaceWith("item(name, keyCombination, graphic).action(onAction)"))
fun ContextMenu.menuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: (ActionEvent) -> Unit = {}): MenuItem {
    val menuItem = MenuItem(name, graphic)
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = this }
    menuItem.setOnAction(onAction)
    this += menuItem
    return menuItem
}

/**
 * Create a MenuItem. The op block operates on the MenuItem where you can call `setOnAction` to provide the menu item action. Notice that this differs
 * from the deprecated `menuitem` builder where the op is configured as the `setOnAction` directly.
 */
fun ContextMenu.item(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}): MenuItem {
    val menuItem = MenuItem(name, graphic)
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = this }
    op(menuItem)
    this += menuItem
    return menuItem
}

/**
 * Create a MenuItem with the name property bound to the given observable string. The op block operates on the MenuItem where you can
 * call `setOnAction` to provide the menu item action. Notice that this differs from the deprecated `menuitem` builder where the op
 * is configured as the `setOnAction` directly.
 */
fun ContextMenu.item(name: ObservableValue<String>, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}): MenuItem {
    val menuItem = MenuItem(null, graphic)
    menuItem.textProperty().bind(name)
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = this }
    op(menuItem)
    this += menuItem
    return menuItem
}

/**
 * Add a separator to the contextmenu
 */
fun ContextMenu.separator(op: SeparatorMenuItem.() -> Unit = {}) {
    val separator = SeparatorMenuItem()
    op(separator)
    this += separator
}

//Menu extensions
fun Menu.menu(name: String? = null, keyCombination: KeyCombination? = null, graphic: Node? = null, op: Menu.() -> Unit = {}): Menu {
    val menu = Menu(name, graphic)
    keyCombination?.apply { menu.accelerator = this }
    op(menu)
    this += menu
    return menu
}

//Menu extensions
fun Menu.menu(name: String? = null, keyCombination: String, graphic: Node? = null, op: Menu.() -> Unit = {}) =
    menu(name, KeyCombination.valueOf(keyCombination), graphic, op)

/**
 * Create a MenuItem. The op block will be configured as the `setOnAction`. This will be deprecated in favor of the `item` call, where the
 * op block operates on the MenuItem. This deprecation was made to align the menuitem builder with the other builders.
 */
@Deprecated("Use the item builder instead, which expects an action parameter", ReplaceWith("item(name, KeyCombination.valueOf(keyCombination), graphic).action(onAction)"))
fun Menu.menuitem(name: String, keyCombination: String, graphic: Node? = null, onAction: () -> Unit = {}) =
    item(name, KeyCombination.valueOf(keyCombination), graphic).apply { action(onAction) }

/**
 * Create a MenuItem. The op block will be configured as the `setOnAction`. This will be deprecated in favor of the `item` call, where the
 * op block operates on the MenuItem. This deprecation was made to align the menuitem builder with the other builders.
 */
@Deprecated("Use the item builder instead, which expects an action parameter", ReplaceWith("item(name, keyCombination, graphic).action(onAction)"))
fun Menu.menuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: (ActionEvent) -> Unit = {}): MenuItem {
    val menuItem = MenuItem(name, graphic);
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = graphic }
    menuItem.setOnAction(onAction)
    this += menuItem
    return menuItem
}

/**
 * Create a MenuItem. The op block operates on the MenuItem where you can call `action` to provide the menu item action.
 * Notice that this differs from the deprecated `menuitem` builder where the op
 * is configured as the `setOnAction` directly.
 */
fun Menu.item(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}): MenuItem {
    val menuItem = MenuItem(name, graphic)
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = graphic }
    op(menuItem)
    this += menuItem
    return menuItem
}

/**
 * Create a CustomMenuItem. You must provide a builder inside the `CustomMenuItem` or assign to the `content` property
 * of the item. The item action is configured with the `action` builder.
 */
fun Menu.customitem(keyCombination: KeyCombination? = null, hideOnClick: Boolean = true, op: CustomMenuItem.() -> Unit = {}): CustomMenuItem {
    val menuItem = CustomMenuItem()
    menuItem.isHideOnClick = hideOnClick
    keyCombination?.also { menuItem.accelerator = it }
    op(menuItem)
    this += menuItem
    return menuItem
}

/**
 * Create a CustomMenuItem. You must provide a builder inside the `CustomMenuItem` or assign to the `content` property
 * of the item. The item action is configured with the `action` builder.
 */
fun MenuButton.customitem(keyCombination: KeyCombination? = null, hideOnClick: Boolean = true, op: CustomMenuItem.() -> Unit = {}): CustomMenuItem {
    val menuItem = CustomMenuItem()
    menuItem.isHideOnClick = hideOnClick
    keyCombination?.also { menuItem.accelerator = it }
    op(menuItem)
    items.add(menuItem)
    return menuItem
}

/**
 * Create a CustomMenuItem. You must provide a builder inside the `CustomMenuItem` or assign to the `content` property
 * of the item. The item action is configured with the `action` builder.
 */
fun ContextMenu.customitem(keyCombination: KeyCombination? = null, hideOnClick: Boolean = true, op: CustomMenuItem.() -> Unit = {}): CustomMenuItem {
    val menuItem = CustomMenuItem()
    menuItem.isHideOnClick = hideOnClick
    keyCombination?.also { menuItem.accelerator = it }
    op(menuItem)
    this += menuItem
    return menuItem
}
fun MenuButton.item(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}): MenuItem {
    val menuItem = MenuItem(name, graphic)
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = graphic }
    op(menuItem)
    items.add(menuItem)
    return menuItem
}

/**
 * Create a MenuItem. The op block operates on the MenuItem where you can call `setOnAction` to provide the menu item action.
 */
fun Menu.item(name: String, keyCombination: String, graphic: Node? = null, op: MenuItem.() -> Unit = {}) =
    item(name, KeyCombination.valueOf(keyCombination), graphic, op)

/**
 * Create a MenuItem with the name property bound to the given observable string. The op block operates on the MenuItem where you can
 * call `setOnAction` to provide the menu item action. Notice that this differs from the deprecated `menuitem` builder where the op
 * is configured as the `setOnAction` directly.
 */
fun Menu.item(name: ObservableValue<String>, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}): MenuItem {
    val menuItem = MenuItem(null, graphic)
    menuItem.textProperty().bind(name)
    keyCombination?.apply { menuItem.accelerator = this }
    graphic?.apply { menuItem.graphic = graphic }
    op(menuItem)
    this += menuItem
    return menuItem
}

fun Menu.separator() {
    this += SeparatorMenuItem()
}

fun Menu.radiomenuitem(name: String, toggleGroup: ToggleGroup? = null, keyCombination: KeyCombination? = null, graphic: Node? = null, op: RadioMenuItem.() -> Unit = {}): RadioMenuItem {
    val radioMenuItem = RadioMenuItem(name, graphic)
    toggleGroup?.apply { radioMenuItem.toggleGroup = this }
    keyCombination?.apply { radioMenuItem.accelerator = this }
    graphic?.apply { radioMenuItem.graphic = graphic }
    op(radioMenuItem)
    this += radioMenuItem
    return radioMenuItem
}

fun Menu.checkmenuitem(name: String, keyCombination: String, graphic: Node? = null, selected: Property<Boolean>? = null, op: CheckMenuItem.() -> Unit = {}) =
        checkmenuitem(name, KeyCombination.valueOf(keyCombination), graphic, selected, op)

fun Menu.checkmenuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, selected: Property<Boolean>? = null, op: CheckMenuItem.() -> Unit = {}): CheckMenuItem {
    val checkMenuItem = CheckMenuItem(name, graphic)
    keyCombination?.apply { checkMenuItem.accelerator = this }
    graphic?.apply { checkMenuItem.graphic = graphic }
    selected?.apply { checkMenuItem.selectedProperty().bindBidirectional(this) }
    op(checkMenuItem)
    this += checkMenuItem
    return checkMenuItem
}

fun MenuButton.radiomenuitem(name: String, toggleGroup: ToggleGroup? = null, keyCombination: KeyCombination? = null, graphic: Node? = null, op: RadioMenuItem.() -> Unit = {}): RadioMenuItem {
    val radioMenuItem = RadioMenuItem(name, graphic)
    toggleGroup?.apply { radioMenuItem.toggleGroup = this }
    keyCombination?.apply { radioMenuItem.accelerator = this }
    graphic?.apply { radioMenuItem.graphic = graphic }
    op(radioMenuItem)
    items.add(radioMenuItem)
    return radioMenuItem
}

fun MenuButton.checkmenuitem(name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: CheckMenuItem.() -> Unit = {}): CheckMenuItem {
    val checkMenuItem = CheckMenuItem(name, graphic)
    keyCombination?.apply { checkMenuItem.accelerator = this }
    graphic?.apply { checkMenuItem.graphic = graphic }
    op(checkMenuItem)
    items.add(checkMenuItem)
    return checkMenuItem
}

fun EventTarget.contextmenu(op: ContextMenu.() -> Unit = {}) = apply {
    val menu = (this as? Control)?.contextMenu ?: ContextMenu()
    op(menu)
    if (this is Control) {
        contextMenu = menu
    } else if (this is Node) {
        setOnContextMenuRequested { event ->
            menu.show(this, event.screenX, event.screenY)
            event.consume()
        }
    }
}

/**
 * Add a context menu to the target which will be created on demand.
 */
fun EventTarget.lazyContextmenu(op: ContextMenu.() -> Unit = {}) = apply {
    var currentMenu: ContextMenu? = null
    if (this is Node) {
        setOnContextMenuRequested { event ->
            currentMenu?.hide()

            currentMenu = ContextMenu()
            currentMenu!!.setOnCloseRequest { currentMenu = null }
            op(currentMenu!!)
            currentMenu!!.show(this, event.screenX, event.screenY)
            event.consume()
        }
    }
}