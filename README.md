![TornadoFX Logo](graphics/tornado-fx-logo.png?raw=true "TornadoFX")
# TornadoFX

Lightweight JavaFX Framework for Kotlin

[![Travis CI](https://travis-ci.org/edvin/tornadofx.svg)](https://travis-ci.org/edvin/tornadofx)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/no.tornado/tornadofx/badge.svg)](https://search.maven.org/#search|ga|1|no.tornado.tornadofx)
[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Features

- Dependency injection
- Type safe builders
- Async task execution
- MVC
- Extremely light weight
- Small, easy to grasp API
- REST client with automatic JSON conversion
- Zero config, no XML, no annotations

## Getting started

- [Wiki](https://github.com/edvin/tornadofx/wiki)
- [Documentation](https://github.com/edvin/tornadofx/wiki/Documentation) 
- [Ask a Question](http://stackoverflow.com/questions/ask?tags=tornadofx)
- [Example Application](https://github.com/edvin/tornadofx-samples) 
- [Maven QuickStart Archetype](https://github.com/edvin/tornadofx-quickstart-archetype) 
- [Changelog](CHANGELOG.md)

### Generate a quickstart application with Maven

```bash
mvn archetype:generate -DarchetypeGroupId=no.tornado \
  -DarchetypeArtifactId=tornadofx-quickstart-archetype \
  -DarchetypeVersion=1.0.1
```

### Add TornadoFX to an existing project

```xml
<dependency>
	<groupId>no.tornado</groupId>
	<artifactId>tornadofx</artifactId>
	<version>1.3.2</version>
</dependency>
```

### What does it look like? (Code snippets)

Create a View

```kotlin
class HelloWorld : View() {
	override val root = HBox(Label("Hello world")) 
}
```
    
Load the root node from `HelloWorld.fxml` and post process after the FXML is loaded
  
```kotlin
class HelloWorld : View() {
	override val root: HBox by fxml()
	
	@FXML lateinit var myLabel: Label
	
	init {
		myLabel.text = "Hello world"
	}
}
```
> No need to implement `Initializable` to perform post processing

Start your application and show the primary `View` by extending the `App` class
    
```kotlin
class HelloWorldApp : App {
	override val primaryView = HelloWorld::class

	init {
		importStylesheet("/styles.css")
	}
}
```

Create a Customer model object that can be converted to and from JSON:
    
```kotlin
class Customer : JsonModel {
	val id = SimpleIntegerProperty()
	val name = SimpleStringProperty()
	
	fun updateModel(json: JsonObject) = json.apply {
		id.value = int("id")
		name.value = string("name")
	}

	fun toJSON(json: JsonBuilder) = json
		.add("id", id.value)
		.add("name", name.value)		
}
```
    
Create a controller which downloads a JSON list of customers with the REST api:

```kotlin
class HelloWorldController : Controller() {
	val api : Rest by inject()
	
	fun loadCustomers(): ObservableList<Customer> = 
		api.get("customers").list().toModel() 
}
```
Notice that the `Rest` API was injected. You can inject your own Views, Fragments and Controllers
in the same way:

```kotlin
val controller : CustomerController by inject()
```
	
Configure the REST API with a base URI and Basic Authentication:
	
```kotlin
api.baseURI = "http://contoso.com/api"
api.setBasicAuth("user", "secret")
```
	
Load customers in the background and update a TableView on the UI thread:

```kotlin
background {
	controller.loadCustomers()
} ui {
	customerTable.items = it
}
```

Create an HBox with a Label and a TextField with type safe builders:

```kotlin
hbox {
	label("Hello world") {
		addClass("heading")
	}
	
	textfield {
		promptText = "Enter your name"
	}
}
```
	
Get and set per component configuration settings:
	
```kotlin
// set prefWidth from setting or default to 200.0
node.prefWidth(config.double("width", 200.0))

// set username and age, then save
with (config) {
	set("username", "john")
	set("age", 30)
	save()
}
```
	
Create a `Fragment` instead of a `View`. A `Fragment` is not a `Singleton` like `View`is, so you will
get a new instance every time you use the `find()` method, or if you choose to Inject a `Fragment` you 
will actually get a new instance every time you access it.
 	
```kotlin
class MyFragment : Fragment() {
	override val root = Hbox(..)
}
```
 	
Open it in a Modal Window:
 		 	 	
```kotlin
find(MyFragment::class).openModal()
``` 
	 	
Lookup and embed a `Fragment` or `View` inside another `Pane` in one go
  	 	
```kotlin
root += MyFragment::class
```

Inject a `Fragment` or `View` and embed inside another `Pane`
  	 
```kotlin
val myFragment: MyFragment by inject()
 
init {
	root += myFragment
}
```
