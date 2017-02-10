# Change Log

All notable changes to this project will be documented in this file.

## [1.6.2-SNAPSHOT]

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

## [1.6.1]

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

## [1.6.0]

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

## [1.5.9]

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
