package tornadofx

import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCombination

//Menu-related operator functions
operator fun <T : MenuItem> Menu.plusAssign(menuItem: T) {
    if (FX.ignoreParentBuilder != FX.IgnoreParentBuilder.No) return
    this.items += menuItem
}

operator fun MenuBar.plusAssign(menu: Menu) {
    if (FX.ignoreParentBuilder != FX.IgnoreParentBuilder.No) return
    this.menus += menu
}

operator fun <T : MenuItem> ContextMenu.plusAssign(menuItem: T) {
    if (FX.ignoreParentBuilder != FX.IgnoreParentBuilder.No) return
    this.items += menuItem
}

//MenuBar extensions
fun MenuBar.menu(name: String? = null, graphic: Node? = null, op: Menu.() -> Unit = {}) = Menu(name, graphic).also {
    op(it)
    this += it
}

//ContextMenu extensions
fun ContextMenu.menu(name: String? = null, op: Menu.() -> Unit = {}) = Menu(name).also{
    op(it)
    this += it
}

/**
 * Create a MenuItem. The op block will be configured as the `setOnAction`. This will be deprecated in favor of the `item` call, where the
 * op block operates on the MenuItem. This deprecation was made to align the menuitem builder with the other builders.
 */
@Deprecated("Use the item builder instead, which expects an action parameter", ReplaceWith("item(name, KeyCombination.valueOf(keyCombination), graphic).action(onAction)"))
fun ContextMenu.menuitem(
        name: String, keyCombination: String, graphic: Node? = null, onAction: () -> Unit = {}
): MenuItem = item(name, KeyCombination.valueOf(keyCombination), graphic).apply { action(onAction) }

fun ContextMenu.checkmenuitem(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: CheckMenuItem.() -> Unit = {}
) = CheckMenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = graphic }
    op(it)
    this += it
}

/**
 * Create a MenuItem. The op block will be configured as the `setOnAction`. This will be deprecated in favor of the `item` call, where the
 * op block operates on the MenuItem. This deprecation was made to align the menuitem builder with the other builders.
 */
@Deprecated("Use the item builder instead, which expects an action parameter", ReplaceWith("item(name, keyCombination, graphic).action(onAction)"))
fun ContextMenu.menuitem(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: (ActionEvent) -> Unit = {}
) = MenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = this }
    it.setOnAction(onAction)
    this += it
}

/**
 * Create a MenuItem. The op block operates on the MenuItem where you can call `setOnAction` to provide the menu item action. Notice that this differs
 * from the deprecated `menuitem` builder where the op is configured as the `setOnAction` directly.
 */
fun ContextMenu.item(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}
) = MenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = this }
    op(it)
    this += it
}

/**
 * Create a MenuItem with the name property bound to the given observable string. The op block operates on the MenuItem where you can
 * call `setOnAction` to provide the menu item action. Notice that this differs from the deprecated `menuitem` builder where the op
 * is configured as the `setOnAction` directly.
 */
fun ContextMenu.item(
        name: ObservableValue<String>, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}
) = MenuItem(null, graphic).also {
    it.textProperty().bind(name)
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = this }
    op(it)
    this += it
}

/**
 * Add a separator to the contextmenu
 */
fun ContextMenu.separator(op: SeparatorMenuItem.() -> Unit = {}) {
    this += SeparatorMenuItem().also(op)
}

//Menu extensions
fun Menu.menu(
        name: String? = null, keyCombination: KeyCombination? = null, graphic: Node? = null, op: Menu.() -> Unit = {}
) = Menu(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    op(it)
    this += it
}

fun MenuButton.menu(
        name: String? = null, keyCombination: KeyCombination? = null, graphic: Node? = null, op: Menu.() -> Unit = {}
) = Menu(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    op(it)
    items.add(it)
}

//Menu extensions
fun Menu.menu(name: String? = null, keyCombination: String, graphic: Node? = null, op: Menu.() -> Unit = {}) =
    menu(name, KeyCombination.valueOf(keyCombination), graphic, op)

fun MenuButton.menu(name: String? = null, keyCombination: String, graphic: Node? = null, op: Menu.() -> Unit = {}) =
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
fun Menu.menuitem(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, onAction: (ActionEvent) -> Unit = {}
) = MenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = graphic }
    it.setOnAction(onAction)
    this += it
}

/**
 * Create a MenuItem. The op block operates on the MenuItem where you can call `action` to provide the menu item action.
 * Notice that this differs from the deprecated `menuitem` builder where the op
 * is configured as the `setOnAction` directly.
 */
fun Menu.item(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}
) = MenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = graphic }
    op(it)
    this += it
}

/**
 * Create a CustomMenuItem. You must provide a builder inside the `CustomMenuItem` or assign to the `content` property
 * of the item. The item action is configured with the `action` builder.
 */
fun Menu.customitem(
        keyCombination: KeyCombination? = null, hideOnClick: Boolean = true, op: CustomMenuItem.() -> Unit = {}
) = CustomMenuItem().also {
    it.isHideOnClick = hideOnClick
    keyCombination?.apply{ it.accelerator = this }
    op(it)
    this += it
}

/**
 * Create a CustomMenuItem. You must provide a builder inside the `CustomMenuItem` or assign to the `content` property
 * of the item. The item action is configured with the `action` builder.
 */
fun MenuButton.customitem(
        keyCombination: KeyCombination? = null, hideOnClick: Boolean = true, op: CustomMenuItem.() -> Unit = {}
) = CustomMenuItem().also {
    it.isHideOnClick = hideOnClick
    keyCombination?.apply{ it.accelerator = this }
    op(it)
    items.add(it)
}

/**
 * Create a CustomMenuItem. You must provide a builder inside the `CustomMenuItem` or assign to the `content` property
 * of the item. The item action is configured with the `action` builder.
 */
fun ContextMenu.customitem(
        keyCombination: KeyCombination? = null, hideOnClick: Boolean = true, op: CustomMenuItem.() -> Unit = {}
) = CustomMenuItem().also {
    it.isHideOnClick = hideOnClick
    keyCombination?.apply{ it.accelerator = this }
    op(it)
    this += it
}

fun MenuButton.item(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}
) = MenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = graphic }
    op(it)
    items += it
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
fun Menu.item(
        name: ObservableValue<String>, keyCombination: KeyCombination? = null, graphic: Node? = null, op: MenuItem.() -> Unit = {}
) = MenuItem(null, graphic).also {
    it.textProperty().bind(name)
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = graphic }
    op(it)
    this += it
}

fun Menu.separator() {
    this += SeparatorMenuItem()
}

fun Menu.radiomenuitem(
        name: String, toggleGroup: ToggleGroup? = null, keyCombination: KeyCombination? = null,
        graphic: Node? = null, value: Any? = null, op: RadioMenuItem.() -> Unit = {}
)  = RadioMenuItem(name, graphic).also {
    toggleGroup?.apply { it.toggleGroup = this }
    keyCombination?.apply { it.accelerator = this }
    it.properties["tornadofx.toggleGroupValue"] = value ?: name
    graphic?.apply { it.graphic = graphic }
    op(it)
    this += it
}

fun Menu.checkmenuitem(name: String, keyCombination: String, graphic: Node? = null, selected: Property<Boolean>? = null, op: CheckMenuItem.() -> Unit = {}) =
        checkmenuitem(name, KeyCombination.valueOf(keyCombination), graphic, selected, op)

fun Menu.checkmenuitem(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, selected: Property<Boolean>? = null,
        op: CheckMenuItem.() -> Unit = {}
) = CheckMenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = graphic }
    selected?.apply { it.selectedProperty().bindBidirectional(this) }
    op(it)
    this += it
}

fun MenuButton.radiomenuitem(
        name: String, toggleGroup: ToggleGroup? = null, keyCombination: KeyCombination? = null,
        graphic: Node? = null, value: Any? = null, op: RadioMenuItem.() -> Unit = {}
) = RadioMenuItem(name, graphic).also {
    toggleGroup?.apply { it.toggleGroup = this }
    keyCombination?.apply { it.accelerator = this }
    it.properties["tornadofx.toggleGroupValue"] = value ?: name
    graphic?.apply { it.graphic = graphic }
    op(it)
    items += it
}

fun MenuButton.checkmenuitem(
        name: String, keyCombination: KeyCombination? = null, graphic: Node? = null, op: CheckMenuItem.() -> Unit = {}
) = CheckMenuItem(name, graphic).also {
    keyCombination?.apply { it.accelerator = this }
    graphic?.apply { it.graphic = graphic }
    op(it)
    items += it
}

fun EventTarget.contextmenu(op: ContextMenu.() -> Unit = {}): ContextMenu {
    val menu = (this as? Control)?.contextMenu ?: ContextMenu()
    op(menu)
    if (this is Control) {
        contextMenu = menu
    } else (this as? Node)?.apply {
        setOnContextMenuRequested { event ->
            menu.show(this, event.screenX, event.screenY)
            event.consume()
        }
    }
    return menu
}

/**
 * Add a context menu to the target which will be created on demand.
 */
fun EventTarget.lazyContextmenu(op: ContextMenu.() -> Unit = {}) = apply {
    var currentMenu: ContextMenu? = null
    (this as? Node)?.setOnContextMenuRequested { event ->
        currentMenu?.hide()
        currentMenu = ContextMenu().also {
            it.setOnCloseRequest { currentMenu = null }
            op(it)
            it.show(this, event.screenX, event.screenY)
        }
        event.consume()
    }
}

fun ContextMenu.radiomenuitem(
        name: String, toggleGroup: ToggleGroup? = null, keyCombination: KeyCombination? = null,
        graphic: Node? = null, value: Any? = null, op: RadioMenuItem.() -> Unit = {}
)  = RadioMenuItem(name, graphic).also {
    toggleGroup?.apply { it.toggleGroup = this }
    keyCombination?.apply { it.accelerator = this }
    properties["tornadofx.toggleGroupValue"] = value ?: name
    graphic?.apply { it.graphic = graphic }
    op(it)
    this += it
}

