package tornadofx.launcher

import java.net.URI
import java.util.*
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@SuppressWarnings("unchecked")
@XmlRootElement(name = "Application")
class FXManifest {
    @XmlAttribute
    var name: String? = null
    @XmlAttribute
    var uri: URI? = null
    @XmlAttribute(name = "launch")
    var launchClass: String? = null
    @XmlElement(name = "lib")
    var files = ArrayList<LibraryFile>()

    val fxAppURI: URI
        get() = uri!!.resolve("fxapp.xml")
}

