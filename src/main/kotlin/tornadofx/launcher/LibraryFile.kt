package tornadofx.launcher

import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.Adler32
import javax.xml.bind.annotation.XmlAttribute

class LibraryFile() {
    @XmlAttribute
    var file: String? = null
    @XmlAttribute
    var checksum: Long? = null
    @XmlAttribute
    var size: Long? = null

    fun needsUpdate(): Boolean {
        val path = Paths.get(file)
        try {
            return !Files.exists(path) || Files.size(path) != size || checksum(path) != checksum
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    constructor(basepath: Path, file: Path) : this() {
        this.file = basepath.relativize(file).toString()
        this.size = Files.size(file)
        this.checksum = checksum(file)
    }

    fun toURL(): URL {
        try {
            return Paths.get(file).toFile().toURI().toURL()
        } catch (whaat: MalformedURLException) {
            throw RuntimeException(whaat)
        }

    }

    companion object {
        fun checksum(path: Path) = Adler32().apply {
            path.toFile().forEachBlock { buf, read ->
                update(buf, 0, read)
            }
        }.value
    }

}