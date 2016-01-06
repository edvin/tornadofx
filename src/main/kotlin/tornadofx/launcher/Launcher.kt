package tornadofx.launcher

import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.xml.bind.JAXB

@SuppressWarnings("unchecked")
class Launcher : Application() {

    override fun start(primaryStage: Stage) {
        val progressBar = ProgressBar()
        progressBar.prefWidth = 200.0

        val label = Label("Updating...")
        label.style = "-fx-font-weight: bold"

        val root = VBox(label, progressBar)
        root.spacing = 10.0
        root.padding = Insets(25.0, 25.0, 25.0, 25.0)
        root.isFillWidth = true

        val scene = Scene(root)
        primaryStage.initStyle(StageStyle.UNDECORATED)
        primaryStage.scene = scene
        primaryStage.title = manifest!!.name
        primaryStage.show()

        val sync = sync()

        progressBar.progressProperty().bind(sync.progressProperty())

        sync.setOnSucceeded { e ->
            try {
                app = launch(primaryStage)
            } catch (initError: Exception) {
                reportError("Launch", initError)
            }
        }

        sync.setOnFailed { e -> reportError("Sync", sync.exception) }
        Thread(sync).start()
    }


    fun createClassLoader(): URLClassLoader {
        val libs = manifest!!.files.map { it.toURL() }
        return URLClassLoader(libs.toTypedArray())
    }

    @Throws(Exception::class)
    fun launch(primaryStage: Stage): Application {
        val classLoader = createClassLoader()
        val appclass = classLoader.loadClass(manifest!!.launchClass)
        Thread.currentThread().contextClassLoader = classLoader
        val app = appclass.newInstance() as Application
        app.init()
        app.start(primaryStage)
        return app
    }


    fun sync(): Task<Any> {
        return object : Task<Any>() {
            protected override fun call(): Any? {
                if (offline)
                    return null

                val needsUpdate = manifest!!.files.filter { it.needsUpdate() }
                val totalBytes = needsUpdate.map { f -> f.size ?: 0 }.reduce { l1, l2 -> l1 + l2 }
                var totalWritten: Long = 0L

                for (lib in needsUpdate) {
                    updateMessage(lib.file!!.concat("..."))

                    val target = Paths.get(lib.file).toAbsolutePath()
                    Files.createDirectories(target.parent)

                    manifest!!.uri!!.resolve(lib.file).toURL().openStream().use({ input ->
                        target.toFile().forEachBlock { bytes, i -> }
                        Files.newOutputStream(target).use { output ->
                            val buf = ByteArray(65536)

                            do {
                                val size = input.read(buf)
                                if (size <= 0) {
                                    break
                                } else {
                                    output.write(buf, 0, size)
                                    totalWritten += size
                                    updateProgress(totalWritten, totalBytes)
                                }
                            } while (true)
                        }
                    })
                }

                ByteArrayOutputStream().use { mfstream ->
                    JAXB.marshal(manifest, mfstream)

                    val manifestPath = Paths.get("fxapp.xml")

                    val data = mfstream.toByteArray()

                    if (Files.notExists(manifestPath) || !Arrays.equals(Files.readAllBytes(manifestPath), data))
                        Files.write(manifestPath, data)
                }

                return null
            }
        }
    }

    @Throws(Exception::class)
    override fun stop() {
        if (app != null)
            app!!.stop()
    }

    private fun reportError(job: String, error: Throwable) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Failed to $job application"
        alert.headerText = "There was an error during $job of the application"

        val out = ByteArrayOutputStream()
        val writer = PrintWriter(out)
        error.printStackTrace(writer)
        writer.close()
        alert.contentText = out.toString()

        alert.showAndWait()
        Platform.exit()
    }

    companion object {
        private var manifest: FXManifest? = null
        private var app: Application? = null
        private var offline = false

        @JvmStatic fun main(args: Array<String>) {
            // If URI given explicitly, load from there
            if (args.size > 0) {
                loadManifest(URI.create(args[0]).resolve("fxapp.xml"))
            } else {
                // If no uri given, load from fxapp.fxml and try to reload from the uri given in the manifest
                val localURI = Paths.get("fxapp.xml").toUri()
                loadManifest(localURI)

                try {
                    loadManifest(manifest!!.fxAppURI)
                } catch (networkError: Exception) {
                    networkError.printStackTrace()
                    offline = true
                }

            }

            Application.launch()
        }

        private fun loadManifest(uri: URI) {
            manifest = JAXB.unmarshal(uri, FXManifest::class.java)
        }
    }

}
