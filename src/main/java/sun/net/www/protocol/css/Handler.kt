package sun.net.www.protocol.css

import tornadofx.Stylesheet
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.nio.charset.StandardCharsets
import java.util.*

class Handler : URLStreamHandler() {
    override fun openConnection(url: URL): URLConnection {
        return CSSURLConnection(url)
    }

    class CSSURLConnection(url: URL) : URLConnection(url) {
        override fun connect() { }
        override fun getInputStream(): InputStream {
            if (url.port == 64) return Base64.getDecoder().decode(url.host).inputStream()
            val stylesheet = Class.forName(url.host).newInstance() as Stylesheet
            return stylesheet.render().byteInputStream(StandardCharsets.UTF_8)
        }
    }
}
