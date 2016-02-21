# Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- Node.gridpaneConstraints extension function (https://github.com/edvin/tornadofx/issues/12)


### Changed
- Builder for Tab now require the content pane as input parameter to avoid confusion (https://github.com/edvin/tornadofx/issues/8)
- UIComponent#openModal() no longer requests focus for root component - caller can decide instead

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