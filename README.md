# Tornado FX

Lightweight JavaFX Framework for Kotlin

## Features

- Dependency injection
- Type safe builders
- Async task execution
- Clean and easy View / Controller
- Extremely light weight
- Small, easy to grasp API
- REST client with support for model objects
- Elegant syntax

See the [Wiki](https://github.com/edvin/tornadofx/wiki) for documentation.

### Add Tornado FX to your project

    <dependency>
        <groupId>no.tornado</groupId>
        <artifactId>fx</artifactId>
        <version>1.2</version>
    </dependency>

### Some code snippets to get you started

Create a View:

```kotlin
class HelloWorld : View() {
	override val root = HBox(Label("Hello world")) 
}
```
    
You can also load the root node from `HelloWorld.fxml` and implement `Initializable` to perform
  some configuration steps after loading the fxml:
  
```kotlin
class HelloWorld : View(), Initializable {
	override val root: HBox by fxml()
	
	@FXML Label myLabel;
	
	fun initialize(location: URL, resources: ResourceBundle) {
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

Create a Customer model object that can be converted to and from JSON:
    
```kotlin
class Customer : JsonModel {
	val id = SimpleIntegerProperty()
	val name = SimpleStringProperty()
	
	fun updateModel(json: JsonObject) = json.apply {
		id.value = int("id")
		name.value = string("name")
	}

	fun toJSON(json: JsonObjectBuilder) = json
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

The JSON payload from the above example would look like this:
	
```json
[{"id": 1, "name": "John Doe"},{"id": 2, "name": "Jane Doe"}]
```
	
Configure the REST API with a base URI and Basic Authentication:
	
```kotlin
api.baseURI = "http://contoso.com/api"
api.setBasicAuth("user", "secret")
```
	
Inject the controller in your View:

```kotlin
val controller : HelloWorldController by inject()
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
	
Create a `Fragment`
 	
```kotlin
class MyFragment : Fragment() {
	override val root = Hbox(..)
}
```
 	
Open it in a Modal Window:
 		 	 	
```kotlin
find(MyFragment::class).openModal()
``` 
	 	
Embed a `Fragment` or `View` inside another `Pane`
  	 	
```kotlin
root += find(MyFragment::class)
```

Inject a `Fragment` or `View` and embed inside another `Pane`
  	 
```kotlin
val myFragment: MyFragment by inject()
 
init {
	root += myFragment
}
```