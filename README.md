# Tornado FX

Lightweight JavaFX Framework for Kotlin

## Features

- Dependency injection
- Type safe builders
- REST / JSON API
- Async task execution
- Clean and easy View / Controller
- Extremely light weight
- Small, easy to grasp API
- Elegant syntax

See the [Wiki](https://github.com/edvin/tornadofx/wiki) for documentation.

### Add Tornado FX to your project

    <dependency>
        <groupId>no.tornado</groupId>
        <artifactId>fx</artifactId>
        <version>1.2</version>
    </dependency>

### Some code snippets to get you started

Create your first View:

    class HelloWorld : View() {
    	override val root = HBox(Label("Hello world")) 
    }

Start your application by extending the App class. Inject a stylesheet as well:
    
    class HelloWorldApp : App {
    	override val primaryView = HelloWorld::class

		init {
			importStylesheet("/styles.css")
		}
    }
    
Define the root node of your view with FXML from `HelloWorld.fxml` instead:

    override val root: HBox by fxml()
    
Create a controller which downloads a JSON list of customers with the REST api:

	class HelloWorldController : Controller() {
		val api : Rest by inject()
		
		fun loadCustomers(): ObservableList<Customer> = 
			api.get("customers").list().toModel() 
	}
	
Configure the REST API with a base URI and Basic Authentication:
	
	api.baseURI = "http://contoso.com/api"
	api.setBasicAuth("user", "secret")
	
Inject the controller in your View:
	
	val controller : HelloWorldController by inject()
	
Run a background job and update a TableView on the UI thread when the list is available:

	background {
		controller.loadCustomers()
	} ui { customers ->
		customerTable.items = customers
	}

Create an HBox with a Label and a TextField with type safe builders:

	hbox {
		label("Hello world") {
			addClass("heading")
		}
		
		textfield {
			promptText = "Enter your name"
		}
	}
	
Get and set per component configuration settings:
	
	node.prefWidth(config.double("width"))
	
	with (config) {
		set("username", "john")
		set("age", 30)
		save()
	}
	
Create a `Fragment`
 	
	class MyPopup : Fragment() {
		override val root = Hbox(..)
	}
 	
Open it in a Modal Window:
 		 	 	
	find(MyPopup::class).openModal()
	 	
	 	