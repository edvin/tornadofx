package tornadofx.osgi

import org.osgi.service.url.URLStreamHandlerService
import org.osgi.service.url.URLStreamHandlerSetter
import sun.net.www.protocol.css.Handler
import tornadofx.FX
import tornadofx.Stylesheet
import tornadofx.osgi.impl.bundleContext
import java.io.InputStream
import java.net.InetAddress
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.*

class CSSURLStreamHandlerService : Handler(), URLStreamHandlerService {
    private lateinit var realHandler: URLStreamHandlerSetter

    override fun openConnection(url: URL): URLConnection = CSSURLConnection(url)

    class CSSURLConnection(url: URL) : URLConnection(url) {
        override fun connect() {
        }

        override fun getInputStream(): InputStream {
            if (url.port == 64) return Base64.getDecoder().decode(url.host).inputStream()
            val owningBundle = bundleContext.getBundle(url.query.toLong())!!
            val stylesheet = owningBundle.loadClass(url.host).newInstance() as Stylesheet
            val rendered = stylesheet.render()
            if (FX.dumpStylesheets) println(rendered)
            return rendered.byteInputStream(StandardCharsets.UTF_8)
        }
    }

    override fun parseURL(realHandler: URLStreamHandlerSetter, u: URL, spec: String, start: Int, limit: Int) {
        this.realHandler = realHandler
        parseURL(u, spec, start, limit)
    }

    override fun parseURL(u: URL?, spec: String?, start: Int, limit: Int) {
        super.parseURL(u, spec, start, limit)
    }

    override public fun openConnection(u: URL?, p: Proxy?): URLConnection {
        return super.openConnection(u, p)
    }

    override fun hashCode(u: URL?): Int {
        return super.hashCode(u)
    }

    override fun sameFile(u1: URL?, u2: URL?): Boolean {
        return super.sameFile(u1, u2)
    }

    override fun setURL(u: URL?, protocol: String?, host: String?, port: Int, authority: String?, userInfo: String?, path: String?, query: String?, ref: String?) {
        realHandler.setURL(u, protocol, host, port, authority, userInfo, path, query, ref)
    }

    override fun equals(u1: URL?, u2: URL?): Boolean {
        return super.equals(u1, u2)
    }

    override fun toExternalForm(u: URL?): String {
        return super.toExternalForm(u)
    }

    override fun getHostAddress(u: URL?): InetAddress {
        return super.getHostAddress(u)
    }

    override fun hostsEqual(u1: URL?, u2: URL?): Boolean {
        return super.hostsEqual(u1, u2)
    }

    override fun getDefaultPort(): Int {
        return super.getDefaultPort()
    }
}
