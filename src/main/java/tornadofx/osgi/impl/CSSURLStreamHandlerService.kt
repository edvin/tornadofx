package tornadofx.osgi.impl

import org.osgi.service.url.URLStreamHandlerService
import org.osgi.service.url.URLStreamHandlerSetter
import tornadofx.*
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.nio.charset.StandardCharsets
import java.util.*

internal class CSSURLStreamHandlerService : URLStreamHandler(), URLStreamHandlerService {
    private lateinit var realHandler: URLStreamHandlerSetter

    override fun openConnection(url: URL): URLConnection = CSSURLConnection(url)

    class CSSURLConnection(url: URL) : URLConnection(url) {
        override fun connect() {
        }

        override fun getInputStream(): InputStream {
            if (url.port == 64) return Base64.getDecoder().decode(url.host).inputStream()
            val owningBundle = fxBundleContext.getBundle(url.query.substringBefore("&").substringBefore("?").toLong())!!
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

    override fun setURL(u: URL?, protocol: String?, host: String?, port: Int, authority: String?, userInfo: String?, path: String?, query: String?, ref: String?) {
        realHandler.setURL(u, protocol, host, port, authority, userInfo, path, query, ref)
    }

    override fun hashCode(u: URL) = super.hashCode(u)
    override fun toExternalForm(u: URL?) = super.toExternalForm(u)
    override fun equals(u1: URL?, u2: URL?) = super.equals(u1, u2)
    override fun sameFile(u1: URL?, u2: URL?) = super.sameFile(u1, u2)
    override fun getDefaultPort() = super.getDefaultPort()
    override fun getHostAddress(u: URL?) = super.getHostAddress(u)
    override fun hostsEqual(u1: URL?, u2: URL?) = super.hostsEqual(u1, u2)
}