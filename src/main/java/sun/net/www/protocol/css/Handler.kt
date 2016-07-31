package sun.net.www.protocol.css

import tornadofx.FX
import tornadofx.Stylesheet
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.nio.charset.StandardCharsets
import java.util.*

open class Handler : URLStreamHandler() {
    override fun openConnection(url: URL): URLConnection = CSSURLConnection(url)

    class CSSURLConnection(url: URL) : URLConnection(url) {
        override fun connect() { }
        override fun getInputStream(): InputStream {
            if (url.port == 64) return Base64.getDecoder().decode(url.host).inputStream()
            val stylesheet = Class.forName(url.host).newInstance() as Stylesheet
            val rendered = stylesheet.render()
            if (FX.dumpStylesheets) println(rendered)
            return rendered.byteInputStream(StandardCharsets.UTF_8)
        }
    }

    class HandlerFactory : URLStreamHandlerFactory {
        override fun createURLStreamHandler(protocol: String) =
            if ("css" == protocol) Handler() else null
    }
}
