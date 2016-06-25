![TornadoFX Logo](graphics/tornado-fx-logo.png?raw=true "TornadoFX")
# TornadoFX

Lightweight JavaFX Framework for Kotlin

[![Travis CI](https://travis-ci.org/edvin/tornadofx.svg)](https://travis-ci.org/edvin/tornadofx)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/no.tornado/tornadofx/badge.svg)](https://search.maven.org/#search|ga|1|no.tornado.tornadofx)
[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Features

- Supports both MVC, MVP and their derivatives
- Dependency injection
- Type safe GUI builders
- First class FXML support
- Type safe CSS builders
- Async task execution
- Hot reload of Views and Stylesheets
- REST client with automatic JSON conversion
- Zero config, no XML, no annotations

## Getting started

- [Screencast introduction](https://www.youtube.com/watch?v=rjc8_HGHy3c)
- [Book (EAP)](https://edvin.gitbooks.io/tornadofx-guide/content/)
- [Wiki](https://github.com/edvin/tornadofx/wiki)
- [Slack](https://kotlinlang.slack.com/messages/tornadofx/details/)
- [User Forum](https://groups.google.com/forum/#!forum/tornadofx)
- [Dev Forum](https://groups.google.com/forum/#!forum/tornadofx-dev)
- [Stack Overflow](http://stackoverflow.com/questions/ask?tags=tornadofx)
- [Documentation](https://github.com/edvin/tornadofx/wiki/Documentation) 
- [IntelliJ IDEA Plugin](https://github.com/edvin/tornadofx-idea-plugin) 
- [Example Application](https://github.com/edvin/tornadofx-samples) 
- [Maven QuickStart Archetype](https://github.com/edvin/tornadofx-quickstart-archetype) 
- [Changelog](CHANGELOG.md)

### Generate a quickstart application with Maven

```bash
mvn archetype:generate -DarchetypeGroupId=no.tornado \
  -DarchetypeArtifactId=tornadofx-quickstart-archetype \
  -DarchetypeVersion=1.5.0
```

### Add TornadoFX to your project

#### Maven

```xml
<dependency>
	<groupId>no.tornado</groupId>
	<artifactId>tornadofx</artifactId>
	<version>1.5.0</version>
</dependency>
```

### Gradle

```groovy
compile 'no.tornado:tornadofx:1.5.0'
```

### What does it look like? (Code snippets)

Create a View

```kotlin
class HelloWorld : View() {
	override val root = HBox(Label("Hello world")) 
}
```
    
Load the root node from `HelloWorld.fxml` and inject controls by `fx:id`
  
```kotlin
class HelloWorld : View() {
	override val root: HBox by fxml()
	val myLabel: Label by fxid()
	
	init {
		myLabel.text = "Hello world"
	}
}
```

Start your application and show the primary `View` by extending the `App` class
    
```kotlin
class HelloWorldApp : App {
	override val primaryView = HelloWorld::class

	init {
		importStylesheet("/styles.css")
	}
}
```
> Start app and load a stylesheet

Use [Type Safe Builders](https://github.com/edvin/tornadofx/wiki/Type-Safe-Builders) to quickly create complex user interfaces

```kotlin
class MyView : View() {

    override val root = VBox()

    private val persons = FXCollections.observableArrayList<Person>(
            Person(1, "Samantha Stuart", LocalDate.of(1981,12,4)),
            Person(2, "Tom Marks", LocalDate.of(2001,1,23)),
            Person(3, "Stuart Gills", LocalDate.of(1989,5,23)),
            Person(3, "Nicole Williams", LocalDate.of(1998,8,11))
    )

    init {
        with(root) {
            tableview(persons) {
                column("ID", Person::id)
                column("Name", Person::name)
                column("Birthday", Person::birthday)
                column("Age", Person::age)
            }
        }
    }
}
```

**RENDERED UI**

![](https://i.imgur.com/AGMCP8S.png)

Create a Customer model object that can be converted to and from JSON and complies with JavaFX Property guidelines:
    
```kotlin
class Customer : JsonModel {
    var id by property<Int>()
    fun idProperty() = getProperty(Customer::id)

    var name by property<String>()
    fun nameProperty() = getProperty(Customer::name)

    override fun updateModel(json: JsonObject) {
        with(json) {
            id = int("id")
            name = string("name")
        }
    }

    override fun toJSON(json: JsonBuilder) {
        with(json) {
            add("id", id)
            add("name", name)
        }
    }
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
	
Configure the REST API with a base URI and Basic Authentication:
	
```kotlin
with (api) {
    baseURI = "http://contoso.com/api"
    setBasicAuth("user", "secret")
}
```
	
Load customers in the background and update a TableView on the UI thread:

```kotlin
runAsync {
	controller.loadCustomers()
} ui {
	customerTable.items = it
}
```

Load customers and apply to table declaratively:

```kotlin
customerTable.asyncItems { controller.loadCustomers() }
```

Define a type safe CSS stylesheet:

```kotlin
class Styles : Stylesheet() {
    companion object {
        // Define css classes
        val heading by cssclass()
        
        // Define colors
        val mainColor = c("#bdbd22")
    }

    init {
		s(heading) {
		    textFill = mainColor
		    fontSize = 20.px
		    fontWeight = BOLD
		}

        val flat = mixin {
            backgroundInsets = box(0.px)
            borderColor = box(Color.DARKGRAY)
        }
        
        s(button) {
            padding = box(10.px, 20.px)
            fontWeight = BOLD
        }

        s(button, textInput) {
            +flat
        }
    }
}
```

Create an HBox with a Label and a TextField with type safe builders:

```kotlin
hbox {
	label("Hello world") {
		addClass(heading)
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
create a new instance and you can reuse the Fragment in multiple ui locations simultaneously.
 	
```kotlin
class MyFragment : Fragment() {
	override val root = Hbox(..)
}
```
 	
Open it in a Modal Window:
 		 	 	
```kotlin
MyFragment().openModal()
``` 
	 	
Lookup and embed a `View` inside another `Pane` in one go
  	 	
```kotlin
root += MyFragment::class
```

Inject a `View` and embed inside another `Pane`
  	 
```kotlin
val myView: MyView by inject()
 
init {
	root += myFragment
}
```

Swap a View for another (change Scene root or parent/pos)

```kotlin
val pageTwo: PageTwo by inject()

button("Go to next page") {
    setOnAction {
    	replaceWith(pageTwo)
    }
}
```