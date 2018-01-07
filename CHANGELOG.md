# Change Log

## [1.7.15-SNAPSHOT]

### Fixed
- SmartResize.Policy manual resize broken (https://github.com/edvin/tornadofx/issues/570)
- TableView bound to ListProperty should rebind when value changes
- Allow calling Workspace.disableNavigation() when workspace is empty

### Changes
- AnchorPaneConstraint properties now accept any Number, not just Double
- AbstractField.textProperty was renamed to labelProperty to avoid confusion with the textProperty() exposed by textfields inside of a field
- ItemViewModel.bind `defaultValue` parameter
- Node builders inside of `MenuItem` will automatically assign the node to the `graphic` property of the menu item
- The App class (main application entrypoint) no longer requires a primary view parameter, in case you want to show a tray icon or determinine what view to show some other way

### Additions
- TextInputControl.filterInput allows you to discriminate what kind of input should be accepted for a text input control
- String.isLong(), isInt(), isDouble() and isFloat()
- checkmenuitem builder accepts string for keycombination and selected property

## [1.7.14]

### Fixed

- `runAsync` would skip the success/fail steps if no work was done in the op block 
- TableView Pojo Column references support boolean "is" style properties (https://github.com/edvin/tornadofx/issues/560)
- TabPane.tab<UIComponent> inside of an UIComponent is now scope aware
- TreeView.lazyPopulate should never assign null list if filter results in no items

### Changes
- Kotlin 1.2.10
- Node builders inside of `ButtonBase` will automatically assign the node to the `graphic` property of the Button

### Additions
- ConfigProperties (`config`) is now `Closable` so it can be used with `use`

## [1.7.13]

### Fixed

- Navigation button issue when already docked view was docked again (https://github.com/edvin/tornadofx/issues/526)
- Internal thread pools are shutdown on app exit. Running threads in the default thread pool will still block application stop.
- ComboBox.makeAutoCompletable() inspects listView.prefWidth for being bound before attemting to set it (https://github.com/edvin/tornadofx/issues/530)
- Wizard.canGoBack override caused NPE (https://github.com/edvin/tornadofx/issues/211)

### Changes
- Kotlin 1.2.0
- ItemViewModel.bindTo(itemFragment) supports all itemb fragments now, not just ListCellFragment
- lambda's that return unit are no longer nullable. use the default-lambda instead
- ChildInterceptor is now an Interface.
- Component.messages are fetched using the classloader that defined the Component subclass (https://github.com/edvin/tornadofx/issues/553)

### Additions

- `cellFragment` support for DataGrid
- ObservableValue<String>.isBlank() and ObservableValue<String>.isNotBlank() which returns BooleanBinding. Useful for binding to TextField enabled/visible state
- Added `owner` and `title` parameters to `alert` and other dialog builders (https://github.com/edvin/tornadofx/issues/522)
- TextInputControl.editableWhen
- `multiSelect()` for TreeView, TreeTableView, TableView and ListView
- Stylesheets can now be added specificly to a `Parent`- Node with `addStylesheet`

## [1.7.12]

### Fixed
- Fixed #434 leaf nodes are now getting set as expected for `lazypopulate`.
- Style builder can be applied to PopupControl, Tab, TableColumnBase (https://github.com/edvin/tornadofx/issues/476)
- Better handling of Column.makeEditable() for properties that implement Property<Number>

### Changes
- Refactoring: Moved all extension functions and properties targeting `TreeView`
  from `Nodes.kt` to `TreeView.kt`. 
- `alert` builder accepts optional owner parameter (https://github.com/edvin/tornadofx/issues/483)

### Additions
- `fitToParentHeight/Width/Size` as well as `fitToHeight/Width/Size(region)` helpers (https://github.com/edvin/tornadofx/pull/519)
- `beforeShutdown` allows you to register shutdown hooks
- `DataGridPaginator` component to help with pagination for `DataGrid`
- runAsync supports `daemon` parameter to control thread characteristics (https://github.com/edvin/tornadofx/pull/508)
- Node.`runAsyncWithOverlay`
- `Latch`, a subclass of CountdownLatch that exposes a `lockedProperty` and provides immediate release ability
- Inline type safe stylesheet on Parent using the `stylesheet` builder
- Tab.close()
- JsonBuilder.add() supports Iterable<Any> (Turned into JsonArray)
- Added `customitem` menu item builder (https://github.com/edvin/tornadofx/pull/488)
- The default lefCheck for `lazypopulate` is now also recognizing an empty list as a leaf.
- menubutton builder (https://github.com/edvin/tornadofx/issues/461)
- MenuButton.item builder
- Added Fragment support for`TreeCell`

## [1.7.11]

### Fixed

- Accessing last item in DataGridFocusModel was not possible
- Severe performance bug in SmartResize policy causing it to add exessive number of listeners (https://github.com/edvin/tornadofx/issues/460)

### Changes

- Parameters passed to Views will now be updated if you do another find() (https://github.com/edvin/tornadofx/issues/443)
- SingleAssign now throws UninitializedPropertyAccessException instead of Exception
- Removed inc() and dec() from properties
- rangeTo from properties is now lazy
- loadFont size parameter is changed from Double to Number
- Lots of internal refactoring thanks to @tieskedh
- Kotlin 1.1.4
- Wizard and ViewModel are now internationalized
- imageview() builder accepts ObservableValue<Image?> (https://github.com/edvin/tornadofx-guide/issues/43)
- added option to increment and decrement spinners by scrolling (https://github.com/edvin/tornadofx/pull/425)
- onUndock is now called for the View currently embedded as the scene root of a Window when it closes (https://github.com/edvin/tornadofx/issues/427)
- Launch<AppClass> helper for nicer main functions (https://github.com/edvin/tornadofx/pull/431)

### Additions

- TreeTableView.bindSelected()
- CheckMenuItem.bind()
- Button builders with text property support
- Collection Property Delegates (https://github.com/edvin/tornadofx/pull/454)
- Workspace.create button and corresponding UIComponent onCreate callback and creatable property
- Lots of reified functions
- The default ErrorHandler shows structured information about failed HTTP requests
- RestException containing request, response and the underlying exception
- Added JsonBuilder.add(key, Iterable<JsonModel>) to avoid having to call toJSON() on it (https://github.com/edvin/tornadofx/issues/414)
- ViewModel partial rollback (https://github.com/edvin/tornadofx/issues/420)
- FX.addChildInterceptor that can veto or custom add builder children to their parent. Useful for MigPane for example.
- Tab.select() for easier selection of tab without having to access tabPane.selectionModel
- TabPane.contains(UIComponent) and Iterable<Node>.contains(UIComponent)
- Override -fx-accent with type-safe CSS property accentColor
- Component.paramsProperty can be used to detec changes to incoming parameters (overriden on new find)

## [1.7.10]

### Fixed

- Fieldset captions are gone (https://github.com/edvin/tornadofx/issues/399)
- Fieldset padding is missing (https://github.com/edvin/tornadofx/issues/401)
- AutoCompleteComboBoxSkin no longer throws upon reconfiguration

### Changes

- AutoCompleteComboBoxSkin: Added an option to use automatic width for the popup

## [1.7.9] - 2017-08-04

### Additions

- weak delegate for easier construction of weak references that need a deinit callback
- The following extension functions (`managedWhen`, `visibleWhen`, `hiddenWhen`, `disableWhen`, `enableWhen`, `removeWhen`, `onHover`) 
  now return the node the are called on.
- TableColumn.cellFragment to match ListView.cellFragment + SmartTableCell which encapsulate cellFormat, cellCache and cellFragment
- bindChildren(observableSet, converter) to completement the observableList version
- sequentialTransition, parallelTransition builders (https://github.com/edvin/tornadofx/issues/373)
- ObservableList<*>.sizeProperty keeps track of the number of items in an ObservableList
- KeyboardLayout which can export to keyboard-layout-editor.com format
- ObservableValue<T>.onChangeOnce() and ObservableValue<T>.onChangeTimes(n) will disconnect listener after n events
- ViewModel.markDirty(property) to explicitly set a property dirty, for example when a bound list is changed internally
- ViewModel supports binding maps
- `MutableMap.toProperty(key) { generateProperty() }` writes back into the map on change

### Fixed

- Form and Field properties got updated to the new more concise syntax propertyName() vs. property
- LazyTreeItem will now only set children once after getChildren is called.
- DataGrid properly updates when operating on a bound list (https://github.com/edvin/tornadofx/issues/385)
- DataGrid reselects same index if item at selected index is removed (https://github.com/edvin/tornadofx/issues/386)
- imageview builder now accepts null from an `ObservableValue<String>`
- TreeView.cellFormat now unbinds the textProperty and the graphicProperty
- Reified type parameter to ViewModel.bind() to solve properties that are null at the binding call (https://github.com/edvin/tornadofx/issues/365)
- ViewModel.bind() for properties that are null at the binding call + now supports Long amd ObservableList as well
- Fixed Chart.series() bug (https://github.com/edvin/tornadofx/issues/354)
- External/synced changes to bound ViewModel properties should not affect dirty state (https://github.com/edvin/tornadofx/issues/358)
- showModal/showWindow now resizes the window before calling onDock, so the View can override placement easier (https://github.com/edvin/tornadofx/issues/360)
- Avoid extension function confusion on `Configurable` by introducing a new `ConfigProperties` subclass and changing extension functions to member functions (https://github.com/edvin/tornadofx/issues/362)
- TreeTableView.resizeColumnsToFitContent() now waits until the skin is available instead of naively deferring to the next pulse
- Nested tableColumns with valueProvider lambda now nest correctly

### Changes

- Kotlin 1.1.3-2
- DataGrid receives focus on click
- TableView refactoring, all cell manipulation functions are encapsulated in a SmartTableCell
- ItemViewModel's bind methods accept properties that return nullable values (https://github.com/edvin/tornadofx/issues/389)
- ViewModel binding mechanism has been rewritten and supports lists much better now

## [1.7.8] - 2017-06-25

### Additions

- Stage.uiComponent()
- ViewModel.clearDecorators()
- Extensions for StringProperty and BooleanProperty

### Fixed

- Improved ProgressIndicator size for `runAsyncWithProgress`

### Changes

- Kotlin 1.1.3
- `openModal` and `openWindow` returns the Stage
- `dialog` builder operates on a StageAwareFieldset so it can close the dialog easier by calling `close()`
- All JSON extractor functions support vararg keys, will pick the first available (https://github.com/edvin/tornadofx/issues/350)
- ValidationContext.validate(decorateErrors = false) clears decorators
- Property.plus(), minus(), etc now return Bindings instead of Properties

## [1.7.7] - 2017-06-15

### Additions

- Extension functions to NumberProperty classes (obsNumOne + obsNumTwo etc)

### Fixed

- Reverted cellFormat change from 1.7.6 (https://github.com/edvin/tornadofx/issues/349)
- Accessing json properties from app.config inside a view looked up view.config instead of app.config (https://github.com/edvin/tornadofx/issues/346)

### Changes


## [1.7.6] - 2017-06-13

### Additions

- UIComponent.forwardWorkspaceActions(target) will override the current receiver of button states and action callbacks
- replaceWith(component: KClass<T>) accepts `sizeToScene` and `centerOnScreen`
- titledpane builder that accepts the title as ObservableValue<String>
- TaskStatus.completed and FXTask.completedProperty can be used to listen to changes in completion state of a task
- runLater with optional delay: `runLater { }` and `runLater(10.seconds) { .. }`
- ObservableValue.awaitUntil waits on the UI thread without blocking until a given value is set before resuming execution
- ViewModel.bind can create observable properties from mutable vars: `val name = bind(MyObject::name)`
- Rest.Response.Status enum with all official http status codes. (https://github.com/edvin/tornadofx/issues/330)
- `hbox` and `vbox` builders now have optional `alignment` parameter
- `Workspace.dockOnSelect` Will automatically dock the given `UIComponent` if the `ListMenuItem` is selected.
- Rest client supports Digest Authentication
- Inline commands can be defined with `command { }` builder pattern
- hyperlink builder has optional graphic parameter
- UIComponent has `currentStage`, `setWindowMinSize(width, height)` and `setWindowMaxSize(width, height)`
- DrawerItem has `expandedProperty` and `expanded` var (https://github.com/edvin/tornadofx/issues/332)
- UIComponent.replaceWith has `centerOnScreen` parameter
- Shortcut overload for Commands: `shortcut(shortcut, command, param)`

### Fixed

- TableColumn.useTextField() accepts Property<*> - no longer requires ObjectProperty<*>
- Workspace navigation now behaves more like a browser with regards to back/forward button functionality
- ConcurrentModificationException in EventBus fire mechanism
- UIComponent.headingProperty is bound to titleProperty by default, will be unbound if assigned value
- DefaultErrorHandler correctly handles errors with no stacktrace available (https://github.com/edvin/tornadofx/issues/328)
- Non-bound properties inside ViewModels can locate it's ValidationContext, and hence can now be used with validators
- SortedFilteredList will not overwrite the backing list when column sorting is enabled in TableView (setAll disabled) (https://github.com/edvin/tornadofx/issues/344)
- RowExpanders containing nested TableViews no longer inherits white label text when owning row is selected
- Calling `cellFormat` on a TableCell that already has a formatter will now add the new formatter as a decorator instead of overwriting the old
- `cellDecorator` only decorates cells with items. It previously ran also when a cell item became null

### Changes

- Kotlin 1.1.2-5
- Workspace will preemptively register for current scope in init()
- `runAsyncWithProgress` will display the progress indicator in the `graphic` property if the parent is `Labeled`
- Cleaned up menu and item builders, might require parameter adjustment in some cases
- UIComponent.currentWindow is fetched from `root.scene.stage`, falls back to `modalStage` or `primaryStage`
- ListMenu.activeItem accepts null to signal that no menu item is active
- Removed `children` parameter from `hbox` and `vbox` builders - they were early remnants from before we realized how powerful builders could be :)
- `action` delegate no longer has `ActionEvent` as parameter so it can be used for no-args function references. Fallback to `setOnAction` if you need the event.
- `Injectable` was a misnomer and has been deprectated in favor of `ScopedInstance`
- TaskStatus no longer disconnects the current task when the task is completed

## [1.7.5] - 2017-05-19

**Important notice**: The `field` builder used to operate on the `inputContainer` inside the `Field`. This has been changed so that it now operates on the
field itself. If you did something like `parent.isVisible = false` to hide the field, you must now change your code to `isVisible = false`. This new
behavior is more as one would expect and hopefully the change won't cause any trouble to anyone.


### Additions

- ListMenu.item builder gets tag parameter (can be used to identify the item)
- EventTarget.tag and tagProperty, useful for identifying Tabs, ListMenuItem and other components used in "selected" situations.
- Map.queryString creates an URL encoded query string from a Map. Useful for REST calls.
- Tab.enableWhen/disableWhen/visibleWhen
- TabPane.tab builder takes optional tag parameter. If no text parameter is supplied, tag.toString() is used
- Node.cache will create and cache a node inside another node. Useful for Cell implementations to reduce memory footprint. `graphic = cache { createNode() }`
- Rest client supports PATCH (https://github.com/edvin/tornadofx/issues/320)
- warning(), error(), confirmation() and information() shortcuts to alert()
- Command bindings accepts optional parameter using invoke: `button { command = someCommand(someParam) }` or `button { command = someCommand with someParam }`
- ChoiceBox now supports Commanding
- TextField now supports Commanding
- TreeTableSmartResize.POLICY - activate with smartResize() (https://github.com/edvin/tornadofx/issues/316)
- removeWhen/visibleWhen/enableWhen/disableWhen etc functions now also take an observable instead of a function that returns an observable.
- The `label` builder is now capable of taking a graphic node `label("some text", graphic)`
- ComboBoxBase.required() validator
- SmartResize.POLICY can now be installed by calling `smartResize()` on any `TableView`
- SmartResize will automatically resize if the itemsProperty of the TableView changes value
- Workspace.showHeadingLabelProperty controls whether the heading is shown in the Workspace toolbar or not
- TableView/TreeTableView requestResize() will reapply SmartResize policy, useful after content change
- Column.makeEditable() works for all number subclasses
- Workspace `navigateForward` and `navigateBack` explicit functions
- Style builder for MenuItem (https://github.com/edvin/tornadofx/issues/327)
- imageview builder overloads that accepts observable urls or images

### Fixed

- AutoJsonModel supports String types
- HTTPUrlConnection based Rest Client Engine will read data from response even when not successful
- Support view reloading in OSGi environment
- Live Views did not reload changed classes correctly
- Fixed equals/hashCode in FXEventRegistration, could cause events to not fire on similar components
- lazyPopulate child factory was called on UI thread (https://github.com/edvin/tornadofx/issues/318)
- SmartResize.requestResize() some times resulted in misaligned column headers
- JsonModelAuto supports more types and doesn't produce duplicates (before: name and nameProperty - now: just name)
- SmartResize flickers (https://github.com/edvin/tornadofx/issues/321)
- Workspace navigation (viewPos index) stays intact even when views are manually removed from the `viewStack`
- ObservableValue.select() notice changes to outer property (https://github.com/edvin/tornadofx/issues/326)
- Ignore duplicate onUndock call when both parent and scene are set to null

### Changes

- Removed Workspace experimental warning
- alert content parameter is now optional
- `commandProperty` and `commandParameterProperty` are now writable so you can choose between bind or assign
- CSS warning should not be issued in OSGi environment, since bundle activator installs CSS URL Handler
- All shape builders accepts `Number` instead of `Double` so you can write `circle(10, 10, 5)` instead of `circle(10.0, 10.0, 5.0)`
- ComboBox.validator moved to ComboBoxBase.validator to support ColorPicker and DatePicker as well
- Removed InstanceScoped and removed it from Wizard. It was not needed.
- Deprecated `menuitem` builders in favor of `item` builders, which work the same way as other builders with respect to action (IDEA provides quick fix)
- TreeView.lazyPopulate() is now data driven. If the returned list is observable, changes will be reflected in the tree (https://github.com/edvin/tornadofx/issues/317)
- field builder now operates on the field itself instead of the inputContainer. You can now hide() the field directly in the function reference.
- TableColumn.useProgressBar() supports Number subtypes instead of only Double

## [1.7.4] - 2017-04-28

### Additions

- `wrapper` builder which builds a node around the existing View root
- `ListMenu` control and corresponding `listmenu` builders
- `validator` function takes optional `model` parameter for use with properties not currently registered with the ViewModel (FXML support)
- `ToggleGroup.selectedValueProperty()` is a writable property of any type you choose. Set `togglebutton(value)` or `radiobutton(value)` to configure the value represented by each toggle.
- `Wizard.enterProgresses = true` will go to next page when complete and finish on last page (https://github.com/edvin/tornadofx/issues/310)
- `ViewModel.onCommit(commits: List<Commit>)` callback with more information about the commit
- imageview builder that takes an image in addition to the existing one that takes a url
- fxml delegate supports setting optional root element
- Improved Java interop
- Java version of FX.find() can be called without specifying scope 
- `Tab.whenSelected` callback when the tab is selected

### Fixed

- Java version of Component.find() defaults to current component scope instead of DefaultScope
- NPE in layout debugger (https://github.com/edvin/tornadofx/issues/305)

### Changes

- Kotlin 1.1.2
- findParentOfType accepts subclasses
- splitpane() now has an optional orientation parameter 
- Clicking outside of a modal InternalWindow click now play an error sound to indicate modality (https://github.com/edvin/tornadofx/issues/308)

## [1.7.3] - 2017-04-19

### Additions
- ScrollPane.edgeToEdge boolean var to control the "edge-to-edge" style class (https://github.com/edvin/tornadofx/issues/302)
- Android SDK compatibilty (See https://github.com/edvin/tornadofx-android-compat)
- Added `baseColor` CSS property
- `lazyContextmenu` to add context menus that instantiate when the menu actually opens.

### Changes
- Improved Java interop
- Removed faulty choicebox builder and replaced it with one similar to the combobox builder
- `authInterceptor` was deprecated in favor of better named `requestInterceptor`

### Fixes
- Fixed ViewModel validation bug for ComboBox, ChoiceBox and Spinner
- Autocomplete ComboBox listview matches width of ComboBox by default
- JsonStructure.save(path) actually saves (https://github.com/edvin/tornadofx/pull/300)


## [1.7.2] - 2017-04-14

- `shortpress`/`longpress` actions (https://github.com/edvin/tornadofx/pull/286)
- Form layout supports arbitrary layout containers inside fieldsets (to support multiple rows of fields or any other form layout)
- radiomenuitem builder default argument for keyCombination (https://github.com/edvin/tornadofx/issues/298)
- ViewModel bindings configured with autocommit must pass validation before the value is committed
- find<Component> takes op block to let you operate on the found Component directly
- Node.toggleButton behaves correctly if no togglegroup is available (https://github.com/edvin/tornadofx/issues/296)
- ViewModel partial commit and validate: `commit(field1, field2)`
- Wizard component
- ViewModel.valid property will be updated as validators are added
- UIComponent.closeable property and corresponding default configuration in `Workspace.defaultCloseable`
- TabPane.add(SomeView::class) will bind towards title and closeable state of the UIComponent (https://github.com/edvin/tornadofx/issues/294)
- TreeView.populate() is now data driven. If the returned list is observable, changes will be reflected in the tree

## [1.7.1] - 2017-04-06

- Node.findParentOfType will now also find UIComponents
- Configurable default states for `savable`, `refreshable` and `deletable` (Workspace.defaultXXX property)
- `Workspace.delete` button and `onDelete`, `deletableWhen` and `onDelete` on `UIComponent`
- `TabPane.connectWorkspaceActions` makes the `TabPane` a target for save/refresh/delete actions
- Autocomplete tooltip mode for non editable ComboBox (https://github.com/edvin/tornadofx/pull/293)
- `UIComponent.app` points to the current application instance
- `config` base path configurable via `App.configBasePath`
- Per component `config` path configurable via `UIComponent.configPath`
- Global configuration object `app.config` works like the one in `UIComponent`, saves to `conf/app.properties` by default
- TabPane.contentUiComponent will retrieve the UIComponent embedded in the selected tab
- UIComponent callbacks for `onNavigateBack` and `onNavigateForward` can veto Workspace navigation
- Improved TableView.selectOnDrag (https://github.com/edvin/tornadofx/issues/262)
- Functions to load and save Json objects and JsonModel
- Rest Client supports absolute URI's without appending base URI (https://github.com/edvin/tornadofx/issues/289)
- `replaceWith` gets `sizeToScene` boolean parameter, defaults to false (https://github.com/edvin/tornadofx/issues/283)
- `shortcut("keyCombo") { .. }` and `shortcut(KeyCombo) { .. }` configures key press initiated actions
- UIComponent.accelerators map now works from any View, not just Views embedded in a Workspace (https://github.com/edvin/tornadofx/issues/253)
- Added Scope.hasActiveWorkspace to check if the workspace inside the current scope has been activated
- `Button.shortcut` also works when button is embedded in sub view (https://github.com/edvin/tornadofx/issues/253)
- DataGrid correctly calculates horizontal scrollbar
- DataGrid.maxRows will constrain the max number of rows and override maxCellsInRow if needed (https://github.com/edvin/tornadofx/issues/287)
- DataGrid properties are now StylableObjectProperties to make them bindable
- `config` can now read and write JsonObject and JsonArray
- TableView.bindSelected uses listener instead of unidirectional binding
- Simplified ItemViewModel binding: `val name = bind(Customer::nameProperty)` instead of the old `val name = bind { item?.nameProperty }`
- Any?.toProperty() will wrap any value in an observable property, even nullable properties
- TableColumnBase.style builder
- Node.managedWhen builder binding
- Int/Double Spinner builders merged into one Number builder for better compatibility
- Spinner builders have defaults for min (0), max(100), initialValue (property.value if supplied) (https://github.com/edvin/tornadofx/issues/274)
- paddingLeft/paddingRight converted from Double to Number
- JsonObject.contains(text) and JsonModel.contains(text)
- Button.action() shortcut istead of Button.setOnAction()
- ObservableList.invalidate()
- Dialog.toFront()
- Node.whenVisible
- ListCellFragment.onEdit
- ItemViewModel allows passing in the itemProperty
- First togglebutton builder inside a togglegroup will be selected by default (disable with `selectFirst = false`)
- ToggleButton.whenSelected
- SortedFilteredList refilters when items change (add, remove, permutate)
- SortedFilteredList is editable and supports all functions of the ObservableList interface
- ObservableXXXValue.onChange functions should support nullable values
- Changed semantics of `Node.removeWhen` to switch visible/managed state instead of adding/removing from parent
- Internal: ViewModel maintains a map between bound properties towards the ViewModel to support validators in a cleaner way without reflection calls to private APIs (https://github.com/edvin/tornadofx/issues/276)
- Kotlin 1.1.1 and JvmTarget 1.8
- SortedFilteredList.refilter() causes the existing predicate to be reevaluated
- openModal(resizable) and openWindow(resizable) optional parameter
- TextInputControl.trimWhitespace() enforces on focus lost instead of onChange (prevented adding words with whitespace)
- ViewModel.bind accepts cast to IntegerProperty/DoubleProperty/FloatProperty/BooleanProperty even when binding is null at construction time
- Added `loadFont` helper function

## [1.7.0] - 2017-03-04

- EventTarget.bindComponents(sourceList, converter) syncs the child nodes of the event target to the given observable list of UIComponents via the converter
- EventTarget.bindChildren(sourceList, converter) syncs the child nodes of the event target to the given observable list via the converter
- ObservableList.bind(sourceList, converter) syncs two lists and converts from one type to another on the fly
- API Break: Removed Node.margin helper because it shadowed margin property on Nodes which had their own margin property
- ValidationContext.validate() has optional `decorateErrors` parameter
- ValidationContext and ViewModel has `valid` observable boolean value
- Kotlin 1.1 dependency
- Added MenuItem.visibleWhen
- Fixed: `workspace.dockInNewScope(params)` operates on current scope instead of the new
- `buttonbar` builder in `form` now creates and operates on a `ButtonBar`
- `contextmenu` builder now works on any Node, not just Control
- EventBus `subscribe(times = n)` parameter will unregister listener after it has fired `n` times (http://stackoverflow.com/questions/42465786/how-to-unsubscribe-events-in-tornadofx)
- TextInputControl `trimWhitespace()`, `stripWhitespace()`, `stripNonNumeric()`, `stripNonInteger` continually strips or trims whitespace in inputs
- JSON `datetime` function has optional `millis` parameter to convert to/from milliseconds since epoch instead of seconds
- `JsonConfig.DefaultDateTimeMillis = true` will cause `datetime` to convert to/from milliseconds since epoch by default
- Improved Form prefWidth calculations
- MenuItem.enableWhen function
- Custom tab support for Views. Views can be docked in tabs and even delegate to refreshable and savable for the surrounding View
- resources stream/url/get helpers are not non-nullable
- Added resources helper to App class
- Added TrayIcon support (https://gitallhub.com/edvin/tornadofx/issues/255)
- EventBus `fire()` function is now available from the App class
- `ComboBox.makeAutocompletable()`

## [1.6.2] - 2017-02-21

- resizeColumnsToFitContent takes nested columns into account
- SmartResize.POLICY takes nested columns into account
- scrollpane builder now has fitToWidth and fitToHeight params
- typesafe pojo column builders for TableView and TreeTableView eg. column( "Name", MyPojo::getName )
- spinner builders takes property param
- `include(fxmlFile)` builder support
- `fxml()` Views now supports nested includes / controllers injected via `fxid()` (name of controller is `fx:id` + "Controler")
- SqueezeBox.fillHeight property
- Added svgicon builder
- Removed Node.alignment helper, it was misleading
- Added collapsible parameter to titledpane builder
- Added Component.hostServices property to access a JavaFX HostServices instance
- Improved TableView.column builder so it finds mutable properties even when constructor params with same name is present (https://github.com/edvin/tornadofx/issues/247)
- Workspace.viewStack is public
- Workspace detects dynamic components anywhere inside the WorkspaceArea
- TableView.selectOnDrag() will select rows or columns depending on the current selection mode
- resources.text, resources.image and resources.imageview helpers
- Workspace has NavigationMode Stack (default) and Tabs
- `closeModal()` deprecated in favor of `close()` since it will also close tabs and non-modal + internal windows
- SqueezeBox has multiselect option (still defaults to true)
- ContextMenu.checkboxmenuitem builder
- UIComponent.icon property used by Workspace and Drawer
- Workspace Drawer support (workspace.leftDrawer/rightDrawer)
- Drawer component
- SqueezeBox panes are now `closeable`
- Form buttonbar alignment is now working correctly
- UIComponent.currentWindow property
- openModal/openWindow defaults to currentWindow as owner (https://github.com/edvin/tornadofx/issues/246)
- Accordion.fold has `expanded` parameter
- Fixed: ComboBox with cellFormat does not show bound element (https://github.com/edvin/tornadofx/issues/245)

## [1.6.1] -2017-01-26

- whenSaved and whenRefreshed lambdas as alternative to overriding onSave and onRefresh
- Workspace onSave and onDock delegates to the docked View
- InputStream.toJSON and .toJSONArray + resources.json(key) and resources.jsonArray(key)
- Color.derive and Color.ladder
- Rest.Response implements Closeable so it can be `use`ed (https://github.com/edvin/tornadofx/issues/237)
- UIComponent `disableSave()` and `disableRefresh()`
- can now bind to a pojo by providing only a single getter ( eg. person.observable( JavaPerson::getId ) )
  - API break: previously returned a PojoProperty - now returns an ObjectProperty<T>
  - uses javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder and will now propogate PropertyChangeEvents from the pojo
- UIComponent.headingProperty is ObservableValue<String> for easier binding
- `field` builder supports `orientation` parameter which will cause input container to be a VBox instead of an HBox (https://github.com/edvin/tornadofx/issues/190)
- UIComponents can now be instantiated manually instead of via inject() and find()
- Input Control builders now support ObservableValue instead of just Property for automatic binding
- ListView.useCheckbox()
- ItemViewModel.asyncItem helper to reload the underlying item
- Corrected Workspace.dockInNewScope, docking was performed in the old scope (!)

## [1.6.0] - 2017-01-18

- Workspaces (https://edvin.gitbooks.io/tornadofx-guide/content/16.%20Workspaces.html)
- OpenXXX functions: Windows opens centered over owner if owner is passed in as parameter (https://github.com/edvin/tornadofx/issues/231)
- API break: View params are now map(property-ref, value) instead of vararg Pair(String, value)
- menu builder correctly supports sub-menus
- Introduced `item` menu item builder, should be used in favor of `menuitem`, which took the onAction callback insteadof an operation on the MenuItem as the op block parameter (breaks with the other builders)
- menu builder accepts graphic parameter
- ViewModel autocommit bindings doesn't affect dirty state any more
- buttonbar builder for forms
- InternalWindow now has `overlayPaint` that defaults `c("#000", 0.4)
- builderInternalWindow added
- ItemViewModel constructor takes optional initial value
- `ObservableList.asyncItems` and `ListProperty.asyncItems`
- `confirm()` function that executes an action if the user confirms
- di delegate overload to support injecting a dependency by name (in addition to type)
- `builderFragment` and `builderWindow` builders - fragment and window by just supplying a title and builder
- `ObservableList<T>.onChange` to easy listening to change events from observable lists
- `setInScope()` now uses correct KClass when entering the injectable into the components map
- `ItemViewModel.isEmpty` boolean, complements `empty` property
- `setStageIcon(icon)` will replace all existing icons with the supplied (https://github.com/edvin/tornadofx/issues/228)
- `TableColumn.useCheckbox(editable = true)` now fires edit/commit events when value is changed
- Create nested, observable, writable properties using the `observableValue.select()` function
- ViewModel `bind` has optional parameter `forceObjectProperty` to avoid creating `IntegerProperty` for ints etc, so you can have nullable values
- `TableView.onEditCommit()` handler fires when a cell is edited. No need to manage domain object value, just add your business logic
- Fixed scope support. `DefaultScope(MyController::class)` or `MyController::class.scope(DefaultScope)`
- TableColumn hasClass/addClass/removeClass/toggleClass supports type safe stylesheets
- Lots of functions that earlier accepted Double now accept Number
- TableView.enableCellEditing() makes table editable and enables cell selection
- TableView.regainFocusAfterEdit() - make sure TableView doesn't look focus after cell edit
- TableColumn.makeEditable(converter) - supply specific converter for editable fields
- TableColumn.converter(converter) - supply specific converter for read only text fields
- TableColumn.makeEditable() supports BigDecimal
- Added scope.set(injectable) as easier alternative to setInScope(injectable, scope)
- tableview builder that takes `ObservableValue<ObservableList<T>>`, supporting automatic rebind when items change
- vbox and hbox builders supports any Number as spacing parameter, not just Double
- `runAsync` exposes `TaskStatus` model for binding towards task states: running, message, title, progress, value
- `runAsync` now run in the context of `Task` so you can access `updateMessage()` etc
- progressbar and progressindicator builders binds to `Property<Number>` instead of `Property<Double>` to support `DoubleProperty`
- Added `insets()` builder
- Fixed a race condition in Slideshow with overlapping transitions (https://github.com/edvin/tornadofx/issues/225)
- Node `onHover { doSomething() }` helper, param is boolean indicating hover state
- Node builder bindings: disableWhen, enableWhen, visibleWhen, hiddenWhen, removeWhen
- ObservableValue<Boolean>.toBinding() converts observable boolean to BooleanBinding
- TableCell.useCombobox now supports every kind of Property (bug)
- Observable padding properties for Region `paddingXXXProperty` (top/right/bottom/left/vertical/horizontal/all)
- Padding vars for Region: `paddingXXX' (top/right/bottom/left/vertical/horizontal/all)
- Added `proxyprop` helper to create proxied properties
- DataGrid `maxCellsInRow` property (also CSS styleable as `-fx-max-cells-in-row`)
- Added `DataGrid.asyncItems` to load items async with more concise syntax
- Added `DataGrid.bindSelected` to bind selected item to another property or ViewModel
- Fixed a ViewModel binding bug causing errors if external changes were made to a bound facade
- Added `squeezebox` builder. SqueezeBox is an accordion that allows multiple open titledpanes, added using `fold()`
- `cellCache` supports builders. Earlier, builders would be appended to the ListView, creating undesirable results
- `Scene.reloadViews()` is removed from the public API, no need to call it manually
- `titledpane` builder now accepts op parameter like every other builder. node parameter is now optional
- Fieldset.wrapWidth is now Number instead of Double

## [1.5.9] - 2016-12-24

- UIComponent has `isdockedProperty` and `isDocked` boolean telling you if the ui component is currently docked
- Added CSS elements to type safe stylesheets so you can now target f.ex HBox even if it doesn't have a CSS class
- Pass parameters to ui components using inject/find. Inject params via `val myParam : Int by param()` in target view.
- booleanBinding and stringBinding now adds observable receiver as dependency
- Eventbus: `FXEvent` class with `subscribe()`, `unsubscribe` and `fire` functions (https://edvin.gitbooks.io/tornadofx-guide/content/15.%20EventBus.html)
- InternalWindow is public, close() will also close InternalWindow
- `setInScope(value, scope)` allows you to preemptively configure an injectable property
- Allow Labeled.bind() to work on ObservableValue<T> instead of just Property<T>
- HttpClientEngine now adds default json headers to request
- Fixed Bug: Unconsumed POST requests are not posted to the server completely
- Add Connection: Keep-Alive and User-Agent headers to the default rest client engine

## [1.5.8] - 2016-11-24

- WritableValue<T>.assignIfNull(creatorFn) assigns to the value by calling creator unless it is already non-null
- Button.accelerator(KeyCombination) adds shortcuts to buttons (https://github.com/edvin/tornadofx/issues/205)
- Slideshow component and slideshow builder
- openInternalWindow(SomeOtherView::class) opens a window ontop of the current scene graph
- bindStringProperty respects given format (https://github.com/edvin/tornadofx/issues/210)
- Proxy support for Rest client (Set `client.proxy = Proxy()`)
- Pane builder (https://github.com/edvin/tornadofx/issues/208)
- Iterable<Node>.style will apply styles to all elements in collection
- Added `Node.alignment` property that knows how to apply alignment depending on the parent
- Added `Node.margin` property that knows how to apply margin depending on the parent
- canvas builder
- All constraint builders no longer set default values for properties that are not overridden
- Added canvas() builder
- Kotlin 1.0.5-2
- Added `stackpaneConstraints` builder (margin/alignment) (https://github.com/edvin/tornadofx/issues/206)
- Added `Node.hgrow` and `Node.vgrow` properties (https://github.com/edvin/tornadofx/issues/204)
- ComboBox.cellFormat also formats button cell by default with option to override
- UIComponent.openWindow() opens a new modeless Window
- TreeView.bindSelected(itemProperty) and TreeView.bindSelected(itemViewModel)
- Rest POST supports InputStream (https://github.com/edvin/tornadofx/pull/200)
- Removed deprecated `findFragment` - use `find` instead
- ViewModel.ignoreDirtyStateProperties list of properties that should not be considered when calculating dirty state
- Removed deprecated `replaceWith` overloads (https://github.com/edvin/tornadofx/issues/199)
- Scope support
- ViewModel is now `Component` and `Injectable` so it supports injection.
- addClass/removeClass/toggleClass now also works for pseudo classes (https://github.com/edvin/tornadofx/issues/198)
- ItemViewModel().bindTo(listCellFragment)
- resources.stream("some-resource") locates InputStream for resource
- Added custom renderers to custom CSS Properties (https://github.com/edvin/tornadofx/issues/203)

## [1.5.7] - 2016-10-21

- Fixed LayoutDebugger not showing debugged scene correctly (https://github.com/edvin/tornadofx/issues/192)
- App.shouldShowPrimaryStage() can be used to initially hide the primary stage
- Node.onDoubleClick handler
- chooseDirectory function
- ListView.bindSelected(itemProperty) and ListView.bindSelected(itemViewModel)
- TableView.bindSelected(itemProperty) and TableView.bindSelected(itemViewModel)
- Added ItemViewModel to reduce boiler plate for ViewModels with one source object
- SortedFilteredList now supports editing writeback to the underlying observable list
- View.replaceWith now updates scene property to support Live Views (https://github.com/edvin/tornadofx/issues/191)
- ViewModel bind return value is now optional to support eventually available items
- ViewModel detects changes to the source object and applies to the model counterpart automatically
- ViewModel `bind(autocommit = true) { .. }` option
- Mnemonic in Field labels (form -> field -> input.mnemonicTarget())
- Added ItemFragment and ListCellFragment. Will add TableCellFragment etc shortly.
- Added TreeView.cellDecorator
- Node.hide and Node.show
- Node.toggleClass(class, observableBooleanValue)
- Removed cell as `this` for `cellCache`. The cell could change, so taking it into account was a mistake.
- App MainView parameter can now be a `Fragment` as well as `View`
- ListView `cellCache` provider to create a cached graphic node per item
- Kotlin 1.0.4
- The `di()` delegate no longer calls out to the `DIContainer` for every access, effectively caching the lookup
- The `fxid()` delegate can now inject any type, not just `EventTarget` subclasses
- Added non-null `onChange` overrides for primitive `ObservableValue`s
- Fixed bug with `Node.fade` reversed animations (was also affecting `ViewTransitions`)
- Deprecated confusing CSS `add` function if favor of `and`

## [1.5.6] - 2016-09-19

- ViewModel.onCommit() function that will be called after a successful commit
- TableView SmartResize Policy (https://github.com/edvin/tornadofx/wiki/TableView-SmartResize)
- `dynamicContent` builder that will replace content in a Node when an observable value changes
- Alternative `TableView.column` builder with auto-conversion to observable value (`column("Title", ReturnType::class) { value { it.value.somePropertyOrValue })`
- DataGrid component 
- TableColumn `cellCache` provider to create a cached graphic node per item
- Padding shortcuts (paddingRight, paddingLeft, paddingTop, paddingBottom) to Region
- TableView support for Nested Columns (`nestedColumn(title) { // add child columns here }`)
- TableView support for expanded row node (`rowExpander { // create node to show on expand here }`) (https://edvin.gitbooks.io/tornadofx-guide/content/5.%20Builders%20II%20-%20Data%20Controls.html#row-expanders) 
- Fixed bug where image URLs defined in CSS were rendered wrong
- Added support for skipping snake-casing in CSS rules (names still have to be valid css identifiers)
- Fixed bug where CSS selectors defined with strings would have their capitalization changed (`".testThing"` => `".test-thing"`, `cssclass("testThing")` => `.test-thing`)
- Updated the `ViewTransition` code to be more flexible (including now working with any `Node`, not just `View`s and `Fragment`s).
    - Also added several new built in `ViewTransition`s
- Added new `Node` animation helper functions for various transformations
- FXML files can now contain `fx:controller` attribute to help with content assist, if `hasControllerAttribute = true` is passed to the `fxml` delegate (https://github.com/edvin/tornadofx/issues/179)
- Fix exception in chooseFile when user cancels in Multi mode

## [1.5.5] - 2016-08-19

- Stylesheets can be loaded via ServiceLoader (`META-INF/services/tornadofx.Stylesheet` with reference to the stylesheet class)
- Default constructor was re-added to `tornadofx.App` to support `Run View` in IDEA Plugin
- `resizeColumnsToFitContent` has `afterResize` callback parameter
- SortedFilteredList.asyncItems function
- SortedFilteredList can now be assigned as items to tableview/listview builder without calling `bindTo`
- `DefaultErrorHandler.filter` listens to uncaught errors and can consume them to avoid the default error dialog.
- `json.add(key, JsonModel)` automatically converts to JSON
- CSS DSL now supports imports through constructor parameters. e.g. `class DialogStyle : StyleSheet(BaseStyle::class) { ... }`
- Fixed a bug in `View.replaceWith` which caused the whole scene to change even when for sub views

## [1.5.4] - 2016-08-03

This release fixes an issue with type safe stylesheets. `importStylesheet(Styles::class)` would fail unless an OSGi runtime was available.

## [1.5.3] - 2016-08-02

### Added

- `App.createPrimaryScene` overridable function to specify how the scene for the primary View is created
- OSGI manifest metadata
- LayoutDebugger can edit new Node properties: `spacing`
- Stylesheets can be dynamically added at runtime and will affect all active scenes
- Convenience methods for creating bindings on any object. e.g. `stringBinding(person, person.firstNameProperty, person.lastNameProperty) { "$firstName, #lastName" }`
- View/Fragment takes optional title in constructor

### Changed

- UIComponent.showModal now supports reopening even if modalStage was never removed
- `fieldset` block now operates on an `HBox` instead of `Pane` so you can write `alignment = Pos.BASELINE_RIGHT` to right-align buttons etc
- Set modalStage before showAndWait() (https://github.com/edvin/tornadofx/pull/151)
- `Parent.find` and `UIComponent.find` renamed to `lookup` for better alignment with JavaFX `lookup` and to avoid confusion with `find(View::class)` 
- Improved `BorderPane` builders, they also now accept `UIComponent` references instead of instances
- Builders now operate on `EventTarget` instead of `Pane` and as a result, many builders have improved syntax and functionality
- Reduced boilerplate for `App` creation (you can now use `class MyApp : App(MyView::class, Styles::class)`
- ViewModel `commit` and `rollback` run on the UI thread because decorators might be invoked 
- ViewModel `commit` accepts a function that will be run if the commit is successful
- `find` can now also find `Fragments`, so `findFragment` is deprecated
- `lookup` takes an optional op that operates on the UIComponent it found
- `TreeTableView/TableView.populate` accepts any kind of `Iterable<T>` instead of `List`

## [1.5.2] - 2016-07-21

### Added

- Validation support
- Decoration support
- Node.removeFromParent()
- Dimension arithmetics (https://github.com/edvin/tornadofx/pull/146)
- Get a reference to objects in other Components via `get(ComponentType::propertyName)` and set them via `set(ComponentType::propertyName, value`
- `Node.replaceChildren` replaces current children with new ones created with builder
- `Node.runAsyncWithProgress` shows a progress indicator instead of while async task is running 
- `runAsync` on Injectable class references (`CustomerController::class.runAsync { listContacts(customerId) }`)
- `runAsync` on Injectable class function references (`CustomerController::listContacts.runAsync(customerId)`)
- `ObservableValue.onChange` listener
- `UIComponent.whenDocked` and `UIComponent.whenUndocked`
- LayoutDebugger (https://github.com/edvin/tornadofx/wiki/Layout-Debugger)
- ViewModel (https://github.com/edvin/tornadofx/wiki/ViewModel)
- TableView `cellDecorator`
- ComboBox `cellFormat` formatter function
- TreeView `lazyPopulate` alternative to `populate` that lazily creates children as needed
- TreeItem nesting extension functions (https://github.com/edvin/tornadofx/issues/134)
- TableView `selectWhere()`, `moveToTopWhere()` and `moveToBottomWhere()` (https://github.com/edvin/tornadofx/issues/134)
- Group builder `group`
- Improved tab for tabpane builder `tab("Name") { operateOnTab(); content { .. } }`
- Create bindings dependent on an ObservableValue: `objectBinding` + `integerBinding`, `longBinding` etc for all applicable types
- New, simplified method of creating properties `val nameProperty = SimpleStringProperty(); var name by nameProperty` (https://github.com/edvin/tornadofx/pull/143)
- Extract a JsonObject and turn it into a JsonModel by with `json.jsonModel("key")`
- `kotlin-reflect.jar` is now a default dependency. It can be removed if you don't use any of the TableView.`column` functions. Over time, more features will probably require it.
- Replace View function `UIComponent.replaceWith` now accepts `KClass<View>` and `KClass<Fragment>` as well as UIComponent instances
- label() and text() builders now accepts an ObservableValue<String> for unidirectional binding
- Added non-null JSON getters (`getLong(key)` returns Long while `long(key)` returns Long?)
- Improved compatibility with ScenicView by not creating inline/crossinline cellformatters (https://youtrack.jetbrains.com/issue/KT-13148)
 
### Changed

- ImageView builder now loads image lazily by default
- CSSUrlHandler force install moved to CSS.CompanionObject to make sure it happens in time
- addClass/removeClass now accepts vararg
- alert() function now returns Alert object
- Fixed bug: Inherited properties cannot be accessed via getProperty - NoSuchFieldException (https://github.com/edvin/tornadofx/issues/141)
- Uncaught exceptions will now be logged to the console to ensure error message delivery even when UI is not initialized
- Fixed CheckBoxCell binding (https://github.com/edvin/tornadofx/issues/140)
- Builder op block made optional on many builders (https://github.com/edvin/tornadofx/issues/126)
- Fixed bug in chooseFile (returned list with null instead of empty list when nothing was selected

## [1.5.1] - 2016-06-29

### Added

- Shape builders (https://github.com/edvin/tornadofx/issues/129)
- Animation builders (https://github.com/edvin/tornadofx/issues/131)
- Replace View function: `UIComponent.replaceWith`
- `fxid()` specific fx:id name as optional parameter
- webview builder

### Changed

- Call `onUndock` when UIComponent.modalStage closes
- Rewrite of the CSS sub structure, cleaner selector syntax, negative dimensions, no need for `select` keyword for single selectors

## [1.5.0] - 2016-06-10

### Added

- Multivalue property support in type safe stylesheets (API break)
- `UIComponent.onDock` and `UIComponent.onUndock`

## [1.4.10] - 2016-06-02

### Added

- Default Rest Engine supports gzip/deflate
- Default Rest Engine adds Accept: application/json by default

### Changed

- Moved box/c functions outside CssBlock so they can be used in Stylesheet companion object
- Better error reporting and logging for missing or wrong fx:id vs fxid() usage

## [1.4.9] - 2016-05-29

### Added

- Convert Iterable<JsonModel> to JsonArray (`Iterable<JsonModel>.toJSON()`)
- Clipboard functions (https://github.com/edvin/tornadofx/issues/110)
- ContextMenu builder
- TreeTableView column builder that takes an observable value creator
- TreeTableView `rowItem` accessor
- TreeTableView `onUserSelect`
- Preferences can be saved and loaded using the prefences function, see https://github.com/edvin/tornadofx/pull/107

## [1.4.8] - 2016-05-20

### Added

- Inline type safe styles
- Easier navigation of View to Node and Node to View (https://github.com/edvin/tornadofx/issues/112)
- Fragments can be declaratively created via `fragment` delegate
- Type Safe CSS URL Handler will be force-installed if the JVM does not pick it up

### Changed

- Upgrade to Kotlin 1.0.2

## [1.4.7] - 2016-05-10

### Added

- Form Builder (https://github.com/edvin/tornadofx/issues/111)

## [1.4.6] - 2016-05-06

### Added

- openModal supports new optional `block` and `owner` parameters
- Spinner builder (https://github.com/edvin/tornadofx/issues/106)
- POJO Binding Support (https://github.com/edvin/tornadofx/issues/104)

### Changed

- App can be started without overriding `primaryView` -> startup parameter `--view-class=package.View`
- addClass, removeClass, toggleClass returns the Node it was applied to (for chaining support)

## [1.4.5] - 2016-04-29

### Changed

- Live Views no longer reloads nested UIComponents twice (https://github.com/edvin/tornadofx/issues/98)
- Added log info message when a View is reloaded
- `openModal` did not configure tornadofx.scene correctly, causing issues with Live Views
- `Node.setId(Styles.someId)` did not set the correct value

### Removed

- SingleViewApp increased framework complexity and had too many caveats so it was removed
- UIComponent.pack/unpack was removed because their function was not needed and not intuitive (https://github.com/edvin/tornadofx/issues/98#issuecomment-215674901)

## [1.4.4] - 2016-04-27

### Added

- Program parameters `--live-stylesheets`, `--live-views`, `--dump-stylesheets` and `--dev-mode` 
- Hot View Reload (https://github.com/edvin/tornadofx/issues/96) 
- `children(nodeList)` builder helper to redirect built children to a specific node list (https://github.com/edvin/tornadofx/issues/95) 
- `buttonbar` builder (https://github.com/edvin/tornadofx/issues/95)
- `ButtonBar.button` builder (https://github.com/edvin/tornadofx/issues/95)
- `togglegroup` builder

## [1.4.3] - 2016-04-23

### Added

- Type Safe CSS Builders (https://github.com/edvin/tornadofx/issues/80) [Docs here](https://github.com/edvin/tornadofx/wiki/Type-Safe-CSS)
- TableColumn/TreeTableColumn `addClass`/`hasClass`/`removeClass`/`toggleClass` functions
- Binding support (https://github.com/edvin/tornadofx/issues/91)
- `FX.registerApplication(app, stage)` function for easy integration with existing apps (https://github.com/edvin/tornadofx/issues/89) [Docs here](https://github.com/edvin/tornadofx/wiki/Integrate-with-existing-JavaFX-Applications)
- `colorpicker` builder (https://github.com/edvin/tornadofx/issues/76)
- `chooseFile` File Chooser (https://github.com/edvin/tornadofx/issues/76)
- `pagination` builder (https://github.com/edvin/tornadofx/issues/76)
- Configurable alert dialog 
- `Node.bindClass` applies an observable style class to a node (https://github.com/edvin/tornadofx/issues/88)
- Toolbar.spacer and ToolBar.separator builders

### Changed

- Fixed dual instantiation of SingleViewApp
- `runAsync` replaces `background` to avoid collisions with `Region.background`. `background` is now deprecated

## [1.4.2] - 2016-04-14

### Added

- External dependency injection support - Guice, Spring++ (https://github.com/edvin/tornadofx/issues/79)
- `SingleViewApp` for small/example applications (https://github.com/edvin/tornadofx/issues/74)
- `SortedFilteredList` for sorting and filtering data in list controls (https://github.com/edvin/tornadofx/issues/62)
- `TableView.makeIndexColumn` (https://github.com/edvin/tornadofx/pull/64)
- `tableview` builder accepts optional item list
- `TableColumn` cell factories: `useComboBox`, `useTextField`, `useChoiceBox`, `useProgressBar`, `useCheckbox` and `useDatePicker` (https://github.com/edvin/tornadofx/issues/67)
- `TableColumn.enableTextWrap` (https://github.com/edvin/tornadofx/pull/65)
- `TableColumn` cell factory that wraps `PropertyValueFactory` for better POJO support (https://github.com/edvin/tornadofx/pull/75)
- `splitpane` builder (https://github.com/edvin/tornadofx/issues/72)
- `anchorpane` builder (https://github.com/edvin/tornadofx/issues/84)
- `accordion` builder (https://github.com/edvin/tornadofx/pull/73)
- `JsonStructure.toPrettyString` (https://github.com/edvin/tornadofx/pull/77)
- `text` builder (https://github.com/edvin/tornadofx/issues/76)
- `textflow` builder (https://github.com/edvin/tornadofx/issues/76)
- `htmleditor` builder (https://github.com/edvin/tornadofx/pull/83)
- `hyperlink` builder (https://github.com/edvin/tornadofx/pull/78)
- `passwordfield` builder (https://github.com/edvin/tornadofx/pull/78)
- `radiobutton` builder (https://github.com/edvin/tornadofx/pull/78)
- `togglebutton` builder (https://github.com/edvin/tornadofx/pull/78)
- `slider` builder (https://github.com/edvin/tornadofx/pull/81)
- `separator` builder (https://github.com/edvin/tornadofx/pull/82)
- `choicebox` builder (https://github.com/edvin/tornadofx/pull/82)

### Changed

- Upgrade to Kotlin 1.0.1-2
- `Node.toggleClass` could potentially add duplicates
- `TableView/TreeTableView.resizeColumnsToFitContent` scans 50 rows by default
- HttpURLEngine correctly sets Content-Type header
- Som builders were not all lowercase (titledPane renamed to titledpane)

## [1.4.1] - 2016-03-28

### Added

- Chart builders (https://github.com/edvin/tornadofx/issues/55) [Examples here](https://github.com/edvin/tornadofx/wiki/Charts)
- Tooltip builder (https://github.com/edvin/tornadofx/issues/58)

## [1.4.0] - 2016-03-26

This version is binary incompatible if you used the REST Client, perform a clean build
 when upgrading.

As `Apache HttpClient` is no longer required, most of the `HttpResponse` extension functions
 has been removed and reimplemented in the `Rest.Response` interface. If you accessed `HttpClient`
 classes directly, you will need to adjust some code. See the [REST Client documentation](https://github.com/edvin/tornadofx/wiki/REST-Client)
 for updated information.
 	
### Added

- Injection support in the `App` class (https://github.com/edvin/tornadofx/issues/54)
- `TableView/TreeTableView.resizeColumnsToFitContent` function (https://github.com/edvin/tornadofx/issues/48)
- `TreeTableView` and `TreeView` builders (https://github.com/edvin/tornadofx/issues/47)
- Easy access to application resources (https://github.com/edvin/tornadofx/issues/44)
- Alternative view location for [`fxml()` delegate](https://github.com/edvin/tornadofx/wiki/Components#ui-built-with-fxml)

### Changed

- Upgrade to Kotlin 1.0.1
- `Apache HttpClient` is now an optional dependency. Rest API uses `HttpURLConnection` by default (https://github.com/edvin/tornadofx/issues/40)

## [1.3.2] - 2016-03-07

### Added

- @FXML delegate (https://github.com/edvin/tornadofx/issues/34)
- i18n Support (https://github.com/edvin/tornadofx/issues/29)
- `TableView.column()` support for `ObservableValue` member fields
- `FX.runAndWait` sync execution helper
- `TableColumn.makeEditable` extension
- `JsonModelAuto` automatically converts from JavaFX Properties to JSON (requires kotlin-reflect)
- Menu/MenuItem builders
- More layout builders
- More constraints for builders
- `ListView` builder
- `ScrollPane` builder

### Changed

- `Fragment` should not be injectable (https://github.com/edvin/tornadofx/issues/31)

### Removed

- `FXTableView` removed, `column` functions are now extensions on `TableView`
- `TableView.addColumn` removed, replaced by the new `column` functions

## [1.3.1] - 2016-02-28

### Added
- Arbitrary properties per `Component` (https://github.com/edvin/tornadofx/issues/23)
- `singleAssign()` property delegates (https://github.com/edvin/tornadofx/issues/17)
- Tests for `singleAssign()`
- BorderPane builder (https://github.com/edvin/tornadofx/pull/16)
- `Node.gridpaneConstraints` extension function (https://github.com/edvin/tornadofx/issues/12)
- `Node.vboxConstraints` extension function
- `Node.hboxConstraints` extension function
- `TableView` builder (https://github.com/edvin/tornadofx/issues/11)
- Async loading of items for datadriven components (https://github.com/edvin/tornadofx/issues/14)
- `task`/`ui` to run async jobs outside of `Component`

### Changed
- Builder for Tab now require the content pane as input parameter to avoid confusion (https://github.com/edvin/tornadofx/issues/8)
- UIComponent#openModal() no longer requests focus for root component - caller can decide instead
- Property delegate now produces non-nullable property types
- `GridPane.row()` no long hogs the GridPane `userData` object to track rowId

## [1.3.0] - 2016-02-19

### Added
- Delegates for JavaFX Properties (https://github.com/edvin/tornadofx/issues/3)

### Changed
- Changed Maven coordinates to `no.tornado:tornadofx`
- Recompiled for Kotlin 1.0

## [1.2.3] - 2016-02-15

### Added
- Property support for builders, i.e `textfield(customer.nameProperty)`
- More builders

### Changed
- Rest client now uses PoolingHttpClientConnectionManager to support multiple threads
- HttpResponse.consume() never throws exception

## [1.2.2] - 2016-02-08
- Recompiled for Kotlin RC
