package tornadofx

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressBar.INDETERMINATE_PROGRESS
import javafx.scene.control.Tooltip
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.*
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.InputStreamEntity
import org.apache.http.entity.StringEntity
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.*
import org.apache.http.util.EntityUtils
import tornadofx.Rest.Request.Method.*
import java.io.InputStream
import java.io.StringReader
import java.net.*
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonValue

open class Rest : Controller() {
    companion object {
        var engineProvider: (Rest) -> Engine = ::HttpURLEngine
        val ongoingRequests = FXCollections.observableArrayList<Request>()
        val atomicseq = AtomicLong()

        fun useApacheHttpClient() {
            engineProvider = ::HttpClientEngine
        }
    }

    var engine = engineProvider(this)
    var baseURI: String? = null
    var proxy: Proxy? = null

    fun setBasicAuth(username: String, password: String) = engine.setBasicAuth(username, password)
    fun reset() = engine.reset()

    fun get(path: String, data: JsonValue? = null, processor: ((Request) -> Unit)? = null) = execute(GET, path, data, processor)
    fun get(path: String, data: JsonModel, processor: ((Request) -> Unit)? = null) = get(path, JsonBuilder().apply { data.toJSON(this) }.build(), processor)

    fun put(path: String, data: JsonValue? = null, processor: ((Request) -> Unit)? = null) = execute(PUT, path, data, processor)
    fun put(path: String, data: JsonModel, processor: ((Request) -> Unit)? = null) = put(path, JsonBuilder().apply { data.toJSON(this) }.build(), processor)

    fun post(path: String, data: JsonValue? = null, processor: ((Request) -> Unit)? = null) = execute(POST, path, data, processor)
    fun post(path: String, data: JsonModel, processor: ((Request) -> Unit)? = null) = post(path, JsonBuilder().apply { data.toJSON(this) }.build(), processor)
    fun post(path: String, data: InputStream, processor: ((Request) -> Unit)? = null) = execute(POST, path, data, processor)

    fun delete(path: String, data: JsonValue? = null, processor: ((Request) -> Unit)? = null) = execute(DELETE, path, data, processor)
    fun delete(path: String, data: JsonModel, processor: ((Request) -> Unit)? = null) = delete(path, JsonBuilder().apply { data.toJSON(this) }.build(), processor)

    fun getURI(path: String): URI {
        try {
            val uri = StringBuilder()

            if (baseURI != null)
                uri.append(baseURI)

            if (uri.toString().endsWith("/") && path.startsWith("/"))
                uri.append(path.substring(1))
            else if (!uri.toString().endsWith("/") && !path.startsWith("/"))
                uri.append("/").append(path)
            else
                uri.append(path)

            return URI(uri.toString().replace(" ", "%20"))
        } catch (ex: URISyntaxException) {
            throw RuntimeException(ex)
        }

    }

    fun execute(method: Request.Method, target: String, data: Any? = null, processor: ((Request) -> Unit)? = null): Response {
        val request = engine.request(atomicseq.addAndGet(1), method, getURI(target), data)

        if (processor != null)
            processor(request)

        Platform.runLater { ongoingRequests.add(request) }
        return request.execute()
    }

    abstract class Engine {
        var authInterceptor: ((Request) -> Unit)? = null
        var responseInterceptor: ((Response) -> Unit)? = null
        abstract fun request(seq: Long, method: Request.Method, uri: URI, entity: Any? = null): Request
        abstract fun setBasicAuth(username: String, password: String)
        abstract fun reset()
    }

    interface Request {
        enum class Method { GET, PUT, POST, DELETE }

        val seq: Long
        val method: Method
        val uri: URI
        val entity: Any?
        fun addHeader(name: String, value: String)
        fun execute(): Response
    }

    interface Response {
        val request: Request
        val statusCode: Int
        val reason: String
        fun text(): String?
        fun consume(): Response
        fun list(): JsonArray {
            try {
                val content = text()

                if (content == null || content.isEmpty())
                    return Json.createArrayBuilder().build()

                val json = Json.createReader(StringReader(content)).use { it.read() }

                return when (json) {
                    is JsonArray -> json
                    is JsonObject -> Json.createArrayBuilder().add(json).build()
                    else -> throw IllegalArgumentException("Unknown json result value")
                }
            } finally {
                consume()
            }
        }

        fun one(): JsonObject {
            try {
                val content = text()

                if (content == null || content.isEmpty())
                    return Json.createObjectBuilder().build()

                val json = Json.createReader(StringReader(content)).use { it.read() }

                return when (json) {
                    is JsonArray -> {
                        if (json.isEmpty())
                            return Json.createObjectBuilder().build()
                        else
                            return json.getJsonObject(0)
                    }
                    is JsonObject -> json
                    else -> throw IllegalArgumentException("Unknown json result value")
                }
            } finally {
                consume()
            }
        }

        fun content(): InputStream
        fun bytes(): ByteArray
        fun ok() = statusCode == 200
    }
}

class HttpURLEngine(val rest: Rest) : Rest.Engine() {
    override fun setBasicAuth(username: String, password: String) {
        authInterceptor = { engine ->
            val b64 = Base64.getEncoder().encodeToString("$username:$password".toByteArray(UTF_8))
            engine.addHeader("Authorization", "Basic $b64")
        }
    }

    override fun reset() {
        authInterceptor = null
    }

    override fun request(seq: Long, method: Rest.Request.Method, uri: URI, entity: Any?) =
            HttpURLRequest(this, seq, method, uri, entity)
}

class HttpURLRequest(val engine: HttpURLEngine, override val seq: Long, override val method: Rest.Request.Method, override val uri: URI, override val entity: Any?) : Rest.Request {
    val connection: HttpURLConnection
    val headers = mutableMapOf<String, String>()

    init {
        val url = uri.toURL()
        connection = (if (engine.rest.proxy != null) url.openConnection(engine.rest.proxy) else url.openConnection()) as HttpURLConnection
        headers += "Accept-Encoding" to "gzip, deflate"
        headers += "Content-Type" to "application/json"
        headers += "Accept" to "application/json"
    }

    override fun execute(): Rest.Response {
        engine.authInterceptor?.invoke(this)

        for ((key, value) in headers)
            connection.addRequestProperty(key, value)

        connection.requestMethod = method.toString()

        if (entity != null) {
            if (headers["Content-Type"] == null)
                connection.addRequestProperty("Content-Type", "application/json")

            connection.doOutput = true

            connection.connect()

            when (entity) {
                is JsonModel -> connection.outputStream.write(entity.toJSON().toString().toByteArray(UTF_8))
                is JsonValue -> connection.outputStream.write(entity.toString().toByteArray(UTF_8))
                is InputStream -> connection.outputStream.write(entity.readBytes())
                else -> throw IllegalArgumentException("Don't know how to handle entity of type ${entity.javaClass}")
            }
        } else {
            connection.connect()
        }

        val response = HttpURLResponse(this)
        engine.responseInterceptor?.invoke(response)
        return response
    }

    override fun addHeader(name: String, value: String) {
        headers[name] = value
    }
}

class HttpURLResponse(override val request: HttpURLRequest) : Rest.Response {
    override val statusCode: Int get() = request.connection.responseCode

    override fun consume(): Rest.Response {
        request.connection.disconnect()
        Platform.runLater { Rest.ongoingRequests.remove(request) }
        return this
    }

    override val reason: String get() = request.connection.responseMessage

    override fun text() = bytes().toString(UTF_8)

    override fun content() = request.connection.inputStream

    override fun bytes(): ByteArray {
        try {
            val connection = request.connection
            val unwrapped = when (connection.contentEncoding) {
                "gzip" -> GZIPInputStream(connection.inputStream)
                "deflate" -> DeflaterInputStream(connection.inputStream)
                else -> connection.inputStream
            }
            return unwrapped.readBytes()
        } finally {
            consume()
        }
    }
}

class HttpClientEngine(val rest: Rest) : Rest.Engine() {
    lateinit var client: CloseableHttpClient
    lateinit var context: HttpClientContext

    init {
        reset()
    }

    override fun request(seq: Long, method: Rest.Request.Method, uri: URI, entity: Any?) =
            HttpClientRequest(this, client, seq, method, uri, entity)

    override fun setBasicAuth(username: String, password: String) {
        if (rest.baseURI == null) throw IllegalArgumentException("You must configure the baseURI first.")

        val uri = URI.create(rest.baseURI)

        val scheme = if (uri.scheme == null) "http" else uri.scheme
        val port = if (uri.port > -1) uri.port else if (scheme == "http") 80 else 443
        val host = HttpHost(uri.host, port, scheme)

        val credsProvider = BasicCredentialsProvider().apply {
            setCredentials(AuthScope(host), UsernamePasswordCredentials(username, password))
        }

        context.authCache = BasicAuthCache()
        context.authCache.put(host, BasicScheme())

        client = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build()
    }

    override fun reset() {
        client = HttpClientBuilder.create().build()
        context = HttpClientContext.create()
    }
}

class HttpClientRequest(val engine: HttpClientEngine, val client: CloseableHttpClient, override val seq: Long, override val method: Rest.Request.Method, override val uri: URI, override val entity: Any?) : Rest.Request {
    val request: HttpRequestBase

    init {
        when (method) {
            GET -> request = HttpGet(uri)
            PUT -> request = HttpPut(uri)
            POST -> request = HttpPost(uri)
            DELETE -> request = HttpDelete(uri)
        }
        addHeader("Accept-Encoding", "gzip, deflate")
        addHeader("Content-Type", "application/json")
        addHeader("Accept", "application/json")
    }

    override fun execute(): Rest.Response {
        if (engine.rest.proxy != null) {
            val hp = engine.rest.proxy as Proxy
            val sa = hp.address() as? InetSocketAddress
            if (sa != null) {
                val scheme = if (engine.rest.baseURI?.startsWith("https") ?: false) "https" else "http"
                val proxy = HttpHost(sa.address, sa.port,  scheme)
                request.config = RequestConfig.custom().setProxy(proxy).build()
            }
        }
        engine.authInterceptor?.invoke(this)

        if (entity != null && request is HttpEntityEnclosingRequestBase) {

            when (entity) {
                is JsonModel -> request.entity = StringEntity(entity.toJSON().toString(), UTF_8)
                is JsonValue -> request.entity = StringEntity(entity.toString(), UTF_8)
                is InputStream -> request.entity = InputStreamEntity(entity)
                else -> throw IllegalArgumentException("Don't know how to handle entity of type ${entity.javaClass}")
            }
        }

        val httpResponse = client.execute(request, engine.context)

        val response = HttpClientResponse(this, httpResponse)
        engine.responseInterceptor?.invoke(response)
        return response
    }

    override fun addHeader(name: String, value: String) = request.addHeader(name, value)
}

class HttpClientResponse(override val request: HttpClientRequest, val response: CloseableHttpResponse) : Rest.Response {
    override val statusCode: Int get() = response.statusLine.statusCode

    override val reason: String get() = response.statusLine.reasonPhrase

    override fun text(): String {
        try {
            return EntityUtils.toString(response.entity, UTF_8)
        } finally {
            consume()
        }
    }


    override fun consume(): Rest.Response {
        EntityUtils.consumeQuietly(response.entity)
        try {
            if (response is CloseableHttpResponse) response.close()
            return this
        } finally {
            Platform.runLater { Rest.ongoingRequests.remove(request) }
        }
    }

    override fun content() = response.entity.content

    override fun bytes(): ByteArray {
        try {
            return EntityUtils.toByteArray(response.entity)
        } finally {
            consume()
        }
    }
}

inline fun <reified T : JsonModel> JsonObject.toModel(): T {
    val model = T::class.java.newInstance()
    model.updateModel(this)
    return model
}

inline fun <reified T : JsonModel> JsonArray.toModel(): ObservableList<T> {
    return FXCollections.observableArrayList(map { (it as JsonObject).toModel<T>() })
}

class RestProgressBar : Fragment() {
    override val root = ProgressBar().apply {
        prefWidth = 100.0
        isVisible = false
    }

    private val api: Rest by inject()

    init {
        Rest.ongoingRequests.addListener(ListChangeListener<Rest.Request> { c ->
            val size = c.list.size

            Platform.runLater {
                val tooltip = c.list.map { r -> "%s %s".format(r.method, r.uri) }.joinToString("\n")

                root.tooltip = Tooltip(tooltip)
                root.isVisible = size > 0

                if (size == 0) {
                    root.progress = 100.0
                } else if (size == 1) {
                    root.progress = INDETERMINATE_PROGRESS
                } else {
                    val pct = 1.0 / size.toDouble()
                    root.progress = pct
                }
            }
        })
    }

}