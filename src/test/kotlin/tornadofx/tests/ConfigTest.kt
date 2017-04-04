package tornadofx.tests

import javafx.application.Application
import javafx.stage.Stage
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import tornadofx.*
import org.junit.rules.TemporaryFolder
import org.testfx.api.FxToolkit
import java.util.function.Supplier


/**
 * Tests for the Config-Api
 *
 * @author nimakro
 */
class ConfigTest {

    @Rule @JvmField
    val testFolder = TemporaryFolder()

    class TestApp(testFolder: TemporaryFolder): App(TestView::class) {
        // This is the default location we just override it to use the tempFolder provided by junit.
        override val configBasePath = testFolder.newFolder("conf").toPath()
    }

    class GlobalConfigApp(testFolder: TemporaryFolder): App(TestView::class) {
        // This is the default location we just override it to use the tempFolder provided by junit.
        override val configBasePath = testFolder.newFolder("conf").toPath()

        init {
            // Using the default context and default configPath which is configBasePath
            config {
                set("username" to "user")
                set("password" to "pwd")
                save()
            }

            config {
                assertThat(string("username"), `is`("user"))
                assertThat(string("password"), `is`("pwd"))

                // Test default
                assertThat(long("x").orElse(40L), `is`(40L))
            }
        }
    }

    @Test
    fun testLocalConfigComponent() {
        val app = TestApp(testFolder)
        FxToolkit.registerPrimaryStage()
        FxToolkit.setupApplication{  -> app }

        object : Component() {
            init {
               // Using the default context and default configPath which is configBasePath
               config {
                   set("username" to "user")
                   set("password" to "pwd")
                   save()
               }

               config {
                    assertThat(string("username"), `is`("user"))
                    assertThat(string("password"), `is`("pwd"))

                    // Test default
                    assertThat(long("x").orElse(40L), `is`(40L))
               }
            }
        }
    }

    @Test
    fun testGlobalConfigInComponent() {
        val app = TestApp(testFolder)
        FxToolkit.registerPrimaryStage()
        FxToolkit.setupApplication{  -> app }

        object : Controller() {
            init {
                // Using the default context and default configPath which is configBasePath
                config(Context.GLOBAL) {
                    set("username" to "user")
                    set("password" to "pwd")
                    save()
                }
            }
        }

        object: Controller() {
            init {
                config(Context.GLOBAL) {
                    assertThat(string("username"), `is`("user"))
                    assertThat(string("password"), `is`("pwd"))

                    // Test default
                    assertThat(long("x").orElse(40L), `is`(40L))
                }
            }
        }
    }

    @Test
    fun testGlobalConfigInApp() {
        FxToolkit.registerPrimaryStage()
        FxToolkit.setupApplication{ -> GlobalConfigApp(testFolder)}
    }

    @Test
    fun testGlobalConfigInAppAndComponent() {
        val path = testFolder.newFolder("conf").toPath()
        FxToolkit.registerPrimaryStage()
        val app = Supplier<Application> { ->
                object : App(TestView::class) {
                    // This is the default location we just override it to use the tempFolder provided by junit.
                    override val configBasePath = path

                    init {
                        // Using the default context and default configPath which is configBasePath
                        config {
                            set("username" to "user")
                            set("password" to "pwd")
                            save()
                        }
                    }

                }
        }
        FxToolkit.setupApplication(app)
        FxToolkit.cleanupApplication(app.get())
        FxToolkit.setupApplication(app)

        object: Controller() {
            init {
                config(Context.GLOBAL) {
                    assertThat(string("username"), `is`("user"))
                    assertThat(string("password"), `is`("pwd"))

                    // Test default
                    assertThat(long("x").orElse(40L), `is`(40L))
                }
            }
        }
    }
}
