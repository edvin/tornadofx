# Change Log

All notable changes to this project will be documented in this file.

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