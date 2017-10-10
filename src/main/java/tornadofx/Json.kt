package tornadofx

import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import tornadofx.FX.Companion.log
import java.io.InputStream
import java.io.OutputStream
import java.io.StringWriter
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URL
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.json.*
import javax.json.JsonValue.ValueType.NULL
import javax.json.stream.JsonGenerator
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

interface JsonModel {
    /**
     * Fetch JSON values and update the model properties
     * @param json The json to extract values from
     */
    fun updateModel(json: JsonObject) {

    }

    /**
     * Build a JSON representation of the model properties
     * @param json A builder that should be filled with the model properties
     */
    fun toJSON(json: JsonBuilder) {

    }

    /**
     * Build a JSON representation of the model directly to JsonObject
     */
    fun toJSON(): JsonObject {
        val builder = JsonBuilder()
        toJSON(builder)
        return builder.build()
    }

    /**
     * Copy all properties from this object to the given target object by converting to JSON and then updating the target.
     * @param target The target object to update with the properties of this model
     */
    fun copy(target: JsonModel) {
        val builder = JsonBuilder()
        toJSON(builder)
        target.updateModel(builder.build())
    }

    /**
     * Copy all properties from the given source object to this object by converting to JSON and then updating this object.
     * @param source The source object to extract properties from
     */
    fun update(source: JsonModel) {
        val builder = JsonBuilder()
        source.toJSON(builder)
        updateModel(builder.build())
    }

    /**
     * Duplicate this model object by creating a new object of the same type and copy over all the model properties.

     * @param <T> The type of object
     * *
     * @return A new object of type T with the model properties of this object
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : JsonModel> copy(): T {
        try {
            val clone = javaClass.newInstance() as T
            val builder = JsonBuilder()
            toJSON(builder)
            clone.updateModel(builder.build())
            return clone
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

}

object JsonConfig {
    var DefaultDateTimeMillis = false
}

/**
 * Nullable getters are lowercase, not null getters are prependend with get + camelcase
 * JsonObject already provide some not null getters like getString, getBoolean etc, the rest
 * are added here
 */

fun JsonObject.isNotNullOrNULL(key: String): Boolean = containsKey(key) && get(key)?.valueType != NULL

fun <T> JsonObject.firstNonNull(vararg keys: String, extractor: (key: String) -> T): T? = keys
        .firstOrNull { isNotNullOrNULL(it) }
        ?.let { extractor(it) }

fun JsonObject.string(vararg key: String): String? = firstNonNull(*key) { getString(it) }
fun JsonObject.getString(vararg key: String): String = string(*key)!!

fun JsonObject.double(vararg key: String): Double? = jsonNumber(*key)?.doubleValue()
fun JsonObject.getDouble(vararg key: String): Double = double(*key)!!

fun JsonObject.jsonNumber(vararg key: String): JsonNumber? = firstNonNull(*key) { getJsonNumber(it) }
fun JsonObject.getJsonNumber(vararg key: String): JsonNumber = jsonNumber(*key)!!

fun JsonObject.float(vararg key: String): Float? = firstNonNull(*key) { getFloat(it) }
fun JsonObject.getFloat(vararg key: String): Float = float(*key)!!

fun JsonObject.bigdecimal(vararg key: String): BigDecimal? = jsonNumber(*key)?.bigDecimalValue()
fun JsonObject.getBigDecimal(vararg key: String): BigDecimal = bigdecimal(*key)!!

fun JsonObject.long(vararg key: String): Long? = jsonNumber(*key)?.longValue()
fun JsonObject.getLong(vararg key: String) = long(*key)!!

fun JsonObject.bool(vararg key: String): Boolean? = firstNonNull(*key) { getBoolean(it) }
fun JsonObject.boolean(vararg key: String) = bool(*key) // Alias

fun JsonObject.date(vararg key: String): LocalDate? = string(*key)?.let { LocalDate.parse(it) }
fun JsonObject.getDate(vararg key: String): LocalDate = date(*key)!!

fun JsonNumber.datetime(millis: Boolean = JsonConfig.DefaultDateTimeMillis): LocalDateTime = LocalDateTime.ofEpochSecond(longValue() / (if (millis) 1000 else 1), 0, ZoneOffset.UTC)
fun JsonObject.datetime(vararg key: String, millis: Boolean = JsonConfig.DefaultDateTimeMillis): LocalDateTime? = jsonNumber(*key)?.datetime(millis)
fun JsonObject.getDateTime(vararg key: String, millis: Boolean = JsonConfig.DefaultDateTimeMillis): LocalDateTime = getJsonNumber(*key).datetime(millis)

fun JsonObject.uuid(vararg key: String): UUID? = string(*key)?.let { UUID.fromString(it) }
fun JsonObject.getUUID(vararg key: String) = uuid(*key)!!

fun JsonObject.int(vararg key: String): Int? = firstNonNull(*key) { getInt(it) }
fun JsonObject.getInt(vararg key: String): Int = int(*key)!!

fun JsonObject.jsonObject(vararg key: String): JsonObject? = firstNonNull(*key) { getJsonObject(it) }
inline fun <reified T : JsonModel> JsonObject.jsonModel(vararg key: String) = firstNonNull(*key) { T::class.java.newInstance().apply { updateModel(getJsonObject(it)) } }

fun JsonObject.jsonArray(vararg key: String): JsonArray? = firstNonNull(*key) { getJsonArray(it) }

class JsonBuilder {
    private val delegate: JsonObjectBuilder = Json.createObjectBuilder()

    fun <S, T : ObservableValue<S>> add(key: String, observable: T) {
        observable.value?.apply {
            when (this) {
                is Number -> add(key, this)
                is Boolean -> add(key, this)
                is UUID -> add(key, this)
                is LocalDate -> add(key, this)
                is LocalDateTime -> add(key, this)
                is String -> add(key, this)
                is JsonModel -> add(key, this)
            }
        }
    }

    fun add(key: String, value: Number?) = apply {
        when (value) {
            is Int -> delegate.add(key, value)
            is BigDecimal -> delegate.add(key, value)
            is BigInteger -> delegate.add(key, value)
            is Float -> delegate.add(key, value.toDouble())
            is Double -> delegate.add(key, value)
            is Long -> delegate.add(key, value)
        }
    }

    fun add(key: String, value: Boolean?) = apply {
        if (value != null)
            delegate.add(key, value)
    }

    fun add(key: String, value: UUID?) = apply {
        if (value != null)
            delegate.add(key, value.toString())
    }

    fun add(key: String, value: LocalDate?) = apply {
        if (value != null)
            delegate.add(key, value.toString())
    }

    fun add(key: String, value: LocalDateTime?, millis: Boolean = JsonConfig.DefaultDateTimeMillis) = apply {
        if (value != null)
            delegate.add(key, value.toEpochSecond(ZoneOffset.UTC) * (if (millis) 1000 else 1))
    }

    fun add(key: String, value: String?) = apply {
        if (value != null && value.isNotBlank())
            delegate.add(key, value)
    }

    fun add(key: String, value: JsonBuilder?) = apply {
        if (value != null)
            delegate.add(key, value.build())
    }

    fun add(key: String, value: JsonObjectBuilder?) = apply {
        if (value != null)
            delegate.add(key, value.build())
    }

    fun add(key: String, value: JsonObject?) = apply {
        if (value != null)
            delegate.add(key, value)
    }

    fun add(key: String, value: JsonModel?) = apply {
        if (value != null)
            add(key, value.toJSON())

    }

    fun add(key: String, value: JsonArrayBuilder?) = apply {
        if (value != null) {
            val built = value.build()
            if (built.isNotEmpty())
                delegate.add(key, built)
        }
    }

    fun add(key: String, value: JsonArray?) = apply {
        if (value != null && value.isNotEmpty())
            delegate.add(key, value)
    }

    fun add(key: String, value: Iterable<Any>?) = apply {
        if (value != null) {
            val builder = Json.createArrayBuilder()
            value.forEach {
                when (it) {
                    is Int -> builder.add(it)
                    is String -> builder.add(it)
                    is Float -> builder.add(it.toDouble())
                    is Long -> builder.add(it)
                    is BigDecimal -> builder.add(it)
                    is Boolean -> builder.add(it)
                    is JsonModel -> builder.add(it.toJSON())
                    is JsonArray -> builder.add(it)
                    is JsonArrayBuilder -> builder.add(it)
                    is JsonObject -> builder.add(it)
                    is JsonObjectBuilder -> builder.add(it)
                    is JsonValue -> builder.add(it)
                }
            }
            delegate.add(key, builder.build())
        }

        return this
    }

    fun build(): JsonObject {
        return delegate.build()
    }

}


/**
 * Requires kotlin-reflect on classpath
 */
private fun <T> KProperty<T>.generic(): Class<*> =
        (this.javaField?.genericType as ParameterizedType).actualTypeArguments[0] as Class<*>

/**
 * Requires kotlin-reflect on classpath
 */
@Suppress("UNCHECKED_CAST")
interface JsonModelAuto : JsonModel {
    val jsonProperties: Collection<KProperty1<JsonModelAuto, *>> get() {
        val props = javaClass.kotlin.memberProperties
        val propNames = props.map { it.name }
        return props.filterNot { it.name.endsWith("Property") && it.name.substringBefore("Property") in propNames }.filterNot { it.name == "jsonProperties" }
    }

    override fun updateModel(json: JsonObject) {
        jsonProperties.forEach {
            val pr = it.get(this)
            when (pr) {
                is BooleanProperty -> pr.value = json.bool(it.name)
                is LongProperty -> pr.value = json.long(it.name)
                is IntegerProperty -> pr.value = json.int(it.name)
                is DoubleProperty -> pr.value = json.double(it.name)
                is FloatProperty -> pr.value = json.double(it.name)?.toFloat()
                is StringProperty -> pr.value = json.string(it.name)
                is ObjectProperty<*> -> {
                    when (it.generic()) {
                        Boolean::class.java -> (pr as ObjectProperty<Boolean>).value = json.bool(it.name)
                        Long::class.java -> (pr as ObjectProperty<Long>).value = json.long(it.name)
                        Integer::class.java -> (pr as ObjectProperty<Int>).value = json.int(it.name)
                        Double::class.java -> (pr as ObjectProperty<Double>).value = json.double(it.name)
                        Float::class.java -> (pr as ObjectProperty<Float>).value = json.float(it.name)
                        String::class.java -> (pr as ObjectProperty<String>).value = json.string(it.name)
                        LocalDate::class.java -> (pr as ObjectProperty<LocalDate>).value = json.date(it.name)
                        LocalDateTime::class.java -> (pr as ObjectProperty<LocalDateTime>).value = json.datetime(it.name)
                    }
                }
                is MutableList<*> -> {
                    val list = pr as MutableList<Any>
                    list.clear()
                    json.getJsonArray(it.name)?.forEach { jsonObj ->
                        val entry = it.generic().newInstance()
                        if (entry is JsonModel) {
                            list.add(entry.apply { updateModel(jsonObj as JsonObject) })
                        }
                    }
                }
                else -> {
                    if (it is KMutableProperty1<*, *>) {
                        when (it.returnType.javaType) {
                            Boolean::class.javaPrimitiveType -> (it as KMutableProperty1<Any?, Boolean?>).set(this, json.bool(it.name))
                            Boolean::class.javaObjectType -> (it as KMutableProperty1<Any?, Boolean?>).set(this, json.bool(it.name))
                            Long::class.java -> (it as KMutableProperty1<Any?, Long?>).set(this, json.long(it.name))
                            Integer::class.java -> (it as KMutableProperty1<Any?, Int?>).set(this, json.int(it.name))
                            Double::class.java -> (it as KMutableProperty1<Any?, Double?>).set(this, json.double(it.name))
                            Float::class.java -> (it as KMutableProperty1<Any?, Float?>).set(this, json.float(it.name))
                            String::class.java -> (it as KMutableProperty1<Any?, String?>).set(this, json.string(it.name))
                            LocalDate::class.java -> (it as KMutableProperty1<Any?, LocalDate?>).set(this, json.date(it.name))
                            LocalDateTime::class.java -> (it as KMutableProperty1<Any?, LocalDateTime?>).set(this, json.datetime(it.name))
                            else -> {
                                log.warning("AutoModel doesn't know how to handle ${it.returnType}/${it.returnType.javaType}")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun toJSON(json: JsonBuilder) {
        with(json) {
            jsonProperties.forEach {
                val pr = it.get(this@JsonModelAuto)
                when (pr) {
                    is BooleanProperty -> add(it.name, pr.value)
                    is LongProperty -> add(it.name, pr.value)
                    is IntegerProperty -> add(it.name, pr.value)
                    is DoubleProperty -> add(it.name, pr.value)
                    is FloatProperty -> add(it.name, pr.value.toDouble())
                    is StringProperty -> add(it.name, pr.value)
                    is ObjectProperty<*> -> {
                        when (it.generic()) {
                            Boolean::class.java -> add(it.name, (pr as ObjectProperty<Boolean>).value)
                            Long::class.java -> add(it.name, (pr as ObjectProperty<Long>).value)
                            Integer::class.java -> add(it.name, (pr as ObjectProperty<Int>).value)
                            Double::class.java -> add(it.name, (pr as ObjectProperty<Double>).value)
                            Float::class.java -> add(it.name, (pr as ObjectProperty<Float>).value)
                            String::class.java -> add(it.name, (pr as ObjectProperty<String>).value)
                            LocalDate::class.java -> add(it.name, (pr as ObjectProperty<LocalDate>).value)
                            LocalDateTime::class.java -> add(it.name, (pr as ObjectProperty<LocalDateTime>).value)
                        }
                    }
                    is Boolean -> add(it.name, pr)
                    is Long -> add(it.name, pr)
                    is Int -> add(it.name, pr)
                    is Double -> add(it.name, pr)
                    is Float -> add(it.name, pr.toDouble())
                    is String -> add(it.name, pr)
                    is LocalDate -> add(it.name, pr)
                    is LocalDateTime -> add(it.name, pr)
                    is MutableList<*> -> {
                        val list = pr as? List<JsonModel>
                        val jsonArray = Json.createArrayBuilder()
                        list?.forEach { jsonArray.add(it.toJSON()) }
                        add(it.name, jsonArray.build())
                    }
                }
            }
        }
    }
}

fun JsonStructure.toPrettyString(): String {
    return toString(JsonGenerator.PRETTY_PRINTING)
}

fun JsonStructure.toString(vararg options: String): String {
    val stringWriter = StringWriter()
    val config = HashMap<String, Boolean>().apply { options.forEach { put(it, true) } }
    val writerFactory = Json.createWriterFactory(config)
    val jsonWriter = writerFactory.createWriter(stringWriter)
    jsonWriter.write(this)
    jsonWriter.close()
    return stringWriter.toString()
}

fun <T : JsonModel> Iterable<T>.toJSON() = Json.createArrayBuilder().apply { forEach { add(it.toJSON()) } }.build()

fun InputStream.toJSONArray(): JsonArray = Json.createReader(this).use { it.readArray() }
fun InputStream.toJSON(): JsonObject = Json.createReader(this).use { it.readObject() }

fun JsonObject?.contains(text: String?, ignoreCase: Boolean = true) =
        if (this == null || text == null) false else toString().toLowerCase().contains(text, ignoreCase)

fun JsonModel?.contains(text: String?, ignoreCase: Boolean = true) = this?.toJSON()?.contains(text, ignoreCase) ?: false

/**
 * Save this Json structure (JsonObject or JsonArray) to the given output stream and close it.
 */
fun JsonStructure.save(output: OutputStream) = Json.createWriter(output).use { it.write(this) }

/**
 * Save this Json structure (JsonObject or JsonArray) to the given output path.
 */
fun JsonStructure.save(output: Path, vararg options: OpenOption = arrayOf(CREATE, TRUNCATE_EXISTING)) = this.save(Files.newOutputStream(output, *options))

/**
 * Save this JsonModel to the given output stream and close it.
 */
fun JsonModel.save(output: OutputStream) = toJSON().save(output)

/**
 * Save this JsonModel to the given output path.
 */
fun JsonModel.save(output: Path, vararg options: OpenOption = arrayOf(CREATE, TRUNCATE_EXISTING)) = toJSON().save(output, *options)

/**
 * Load a JsonObject from the given URL
 */
fun loadJsonObject(url: URL) = loadJsonObject(url.openStream())

/**
 * Load a JsonObject from the given InputStream
 */
fun loadJsonObject(input: InputStream) = Json.createReader(input).use { it.readObject() }

/**
 * Load a JsonObject from the given path with the optional OpenOptions
 */
fun loadJsonObject(path: Path, vararg options: OpenOption = arrayOf(READ)) = Files.newInputStream(path, *options).use { loadJsonObject(it) }

/**
 * Load a JsonObject from the string source.
 */
fun loadJsonObject(source: String) = loadJsonObject(source.byteInputStream())

/**
 * Load a JsonArray from the given URL
 */
fun loadJsonArray(url: URL) = loadJsonArray(url.openStream())

/**
 * Load a JsonArray from the given string source
 */
fun loadJsonArray(source: String) = loadJsonArray(source.byteInputStream())

/**
 * Load a JsonArray from the given InputStream
 */
fun loadJsonArray(input: InputStream) = Json.createReader(input).use { it.readArray() }

/**
 * Load a JsonArray from the given path with the optional OpenOptions
 */
fun loadJsonArray(path: Path, vararg options: OpenOption = arrayOf(READ)) = Files.newInputStream(path, *options).use { loadJsonArray(it) }

/**
 * Load a JsonModel of the given type from the given URL
 */
inline fun <reified T : JsonModel> loadJsonModel(url: URL) = loadJsonObject(url).toModel<T>()

/**
 * Load a JsonModel of the given type from the given InputStream
 */
inline fun <reified T : JsonModel> loadJsonModel(input: InputStream) = loadJsonObject(input).toModel<T>()

/**
 * Load a JsonModel of the given type from the given path with the optional OpenOptions
 */
inline fun <reified T : JsonModel> loadJsonModel(path: Path, vararg options: OpenOption = arrayOf(READ)) = loadJsonObject(path, *options).toModel<T>()

/**
 * Load a JsonModel from the given String source
 */
inline fun <reified T : JsonModel> loadJsonModel(source: String) = loadJsonObject(source).toModel<T>()
