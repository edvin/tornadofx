package tornadofx.launcher

import javax.xml.bind.JAXB
import java.io.IOException
import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

object CreateManifest {

    @JvmStatic
    fun main(args: Array<String>) {
        val baseURI = URI.create(args[0])
        val launchClass = args[1]
        val appPath = Paths.get(args[2])
        val name = args[3]
        val manifest = create(baseURI, launchClass, appPath, name)
        JAXB.marshal(manifest, appPath.resolve("fxapp.xml").toFile())
    }

    fun create(baseURI: URI, launchClass: String, appPath: Path, name: String): FXManifest {
        val manifest = FXManifest()
        manifest.uri = baseURI
        manifest.launchClass = launchClass
        manifest.name = name

        Files.walkFileTree(appPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!Files.isDirectory(file) && file.toString().endsWith(".jar"))
                    manifest.files.add(LibraryFile(appPath, file))
                return FileVisitResult.CONTINUE
            }
        })

        return manifest
    }

}
