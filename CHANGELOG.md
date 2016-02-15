# Change Log
All notable changes to this project will be documented in this file.

## [1.2.3] - 2016-02-15

### Added
- Property support for builders, i.e `textfield(customer.nameProperty)`
- More builders

### Changed
- Rest client now uses PoolingHttpClientConnectionManager to support multiple threads
- HttpResponse.consume() never throws exception

## [1.2.2] - 2016-02-08
- Recompiled for Kotlin RC