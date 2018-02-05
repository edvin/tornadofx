![TornadoFX Logo](graphics/tornado-fx-logo.png?raw=true "TornadoFX")
# TornadoFX

JavaFX Framework for Kotlin

[![Travis CI](https://travis-ci.org/edvin/tornadofx.svg)](https://travis-ci.org/edvin/tornadofx)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/no.tornado/tornadofx/badge.svg)](https://search.maven.org/#search|ga|1|no.tornado.tornadofx)
[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Features

- Supports both MVC, MVP and their derivatives
- Dependency injection
- Type safe GUI builders
- Type safe CSS builders
- First class FXML support
- Async task execution
- EventBus with thread targeting
- Hot reload of Views and Stylesheets
- OSGi support
- REST client with automatic JSON conversion
- Zero config, no XML, no annotations

## Important version note

TornadoFX requires Kotlin 1.1.2 and jvmTarget 1.8. Make sure you update your IDE plugins (Kotlin + TornadoFX).

After updating IntelliJ IDEA, make sure your Kotlin target version is 1.1 (Project Settings -> Modules -> Kotlin -> Language Version / API Version)

Remember to update your build system to configure the `jvmTarget` as well.

For Maven, you add the following configuration block to `kotlin-maven-plugin`:

```xml
<configuration>
    <jvmTarget>1.8</jvmTarget>
</configuration>
```

For Gradle, it means configuring the `kotlinOptions` of the Kotlin compilation task:

```gradle
compileKotlin {
    kotlinOptions.jvmTarget= "1.8"
}
```

Failing to do so will yield errors about the compiler not being able to inline certain calls.

You also need a full rebuild of your code after a version upgrade. If you run into trouble, try to clean caches and restart IDEA (File -> Invalidate caches / Restart).
 
## Getting started

- [Screencasts](https://www.youtube.com/user/MrEdvinsyse)
- [Guide](https://edvin.gitbooks.io/tornadofx-guide/content/) We are gradually migrating all information from the Wiki into the Guide
- [KDocs](https://tornadofx.io/dokka/tornadofx/tornadofx/index.html)
- [Wiki](https://github.com/edvin/tornadofx/wiki)
- [Slack](https://kotlinlang.slack.com/messages/tornadofx/details/)
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
  -DarchetypeVersion=1.7.14
```

### Add TornadoFX to your project

#### Maven

```xml
<dependency>
    <groupId>no.tornado</groupId>
    <artifactId>tornadofx</artifactId>
    <version>1.7.14</version>
</dependency>
```

### Gradle

```groovy
compile 'no.tornado:tornadofx:1.7.14'
```

### Snapshots are published to Sonatype

Configure your build environment to use snapshots if you want to try out the latest features:

```xml
 <repositories>
   <repository>
     <id>snapshots-repo</id>
     <url>https://oss.sonatype.org/content/repositories/snapshots</url>
     <releases><enabled>false</enabled></releases>
     <snapshots><enabled>true</enabled></snapshots>
   </repository>
 </repositories>
```

Snapshots are published every day at GMT 16:00 if there has been any changes.

### What does it look like? (Code snippets)

Create a View

```kotlin
class HelloWorld : View() {
    override val root = hbox {
        label("Hello world")
    }
}
```
    
Load the root node from `HelloWorld.fxml` and inject controls by `fx:id`
  
```kotlin
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import tornadofx.*

class HelloWorld : View() {
    override val root: HBox by fxml()
    val myLabel: Label by fxid()
    
    init {
        myLabel.text = "Hello world"
    }
}
```

Start your application and show the primary `View` and add a type safe stylesheet
    
```kotlin
import javafx.scene.text.FontWeight
import tornadofx.*

class HelloWorldApp : App(HelloWorld::class, Styles::class)

class Styles : Stylesheet() {
    init {
        label {
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
            backgroundColor += c("#cecece")
        }    
    }    
}
```
> Start app and load a type safe stylesheet

Use [Type Safe Builders](https://github.com/edvin/tornadofx/wiki/Type-Safe-Builders) to quickly create complex user interfaces

```kotlin
class MyView : View() {
    private val persons = FXCollections.observableArrayList(
            Person(1, "Samantha Stuart", LocalDate.of(1981,12,4)),
            Person(2, "Tom Marks", LocalDate.of(2001,1,23)),
            Person(3, "Stuart Gills", LocalDate.of(1989,5,23)),
            Person(3, "Nicole Williams", LocalDate.of(1998,8,11))
    )

    override val root = tableview(persons) {
        column("ID", Person::id)
        column("Name", Person::name)
        column("Birthday", Person::birthday)
        column("Age", Person::age)
        columnResizePolicy = SmartResize.POLICY
    }
}
```

**RENDERED UI**

![](https://i.imgur.com/AGMCP8S.png)

Create a Customer model object that can be converted to and from JSON and exposes both a JavaFX Property and getter/setter pairs:

```kotlin
import tornadofx.getValue
import tornadofx.setValue

class Customer : JsonModel {
    val idProperty = SimpleIntegerProperty()
    var id by idProperty

    val nameProperty = SimpleStringProperty()
    var name by nameProperty

    override fun updateModel(json: JsonObject) {
        with(json) {
            id = int("id") ?: 0
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
        heading {
            textFill = mainColor
            fontSize = 20.px
            fontWeight = BOLD
        }
        
        button {
            padding = box(10.px, 20.px)
            fontWeight = BOLD
        }

        val flat = mixin {
            backgroundInsets += box(0.px)
            borderColor += box(Color.DARKGRAY)
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
    
Create a `Fragment` instead of a `View`. A `Fragment` is not a `Singleton` like `View` is, so you will
create a new instance and you can reuse the Fragment in multiple ui locations simultaneously.
     
```kotlin
class MyFragment : Fragment() {
    override val root = hbox {
    }
}
```
     
Open it in a Modal Window:
                   
```kotlin
find(MyFragment::class).openModal()
``` 
         
Lookup and embed a `View` inside another `Pane` in one go
           
```kotlin
add(MyFragment::class)
```

Inject a `View` and embed inside another `Pane`
       
```kotlin
val myView: MyView by inject()
 
init {
    root.add(myFragment)
}
```

Swap a View for another (change Scene root or embedded View)

```kotlin
button("Go to next page") {
    action {
        replaceWith(PageTwo::class, ViewTransition.Slide(0.3.seconds, Direction.LEFT)
    }
}
```

Open a View in an internal window over the current scene graph

```kotlin
button("Open") {
    action {
        openInternalWindow(MyOtherView::class)
    }
}
```
