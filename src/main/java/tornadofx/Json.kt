package tornadofx

import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import java.io.StringWriter
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.json.*
import javax.json.JsonValue.ValueType.NULL
import javax.json.stream.JsonGenerator
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

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

/**
 * Nullable getters are lowercase, not null getters are prependend with get + camelcase
 * JsonObject already provide some not null getters like getString, getBoolean etc, the rest
 * are added here
 */

fun JsonObject.isNotNullOrNULL(key: String) = containsKey(key) && get(key)?.valueType != NULL

fun JsonObject.string(key: String) = if (isNotNullOrNULL(key)) getString(key) else null

fun JsonObject.double(key: String) = if (isNotNullOrNULL(key)) getDouble(key) else null
fun JsonObject.getDouble(key: String): Double = getJsonNumber(key).doubleValue()

fun JsonObject.bigdecimal(key: String) = if (isNotNullOrNULL(key)) getBigDecimal(key) else null
fun JsonObject.getBigDecimal(key: String) : BigDecimal = getJsonNumber(key).bigDecimalValue()

fun JsonObject.long(key: String) = if (isNotNullOrNULL(key)) getLong(key) else null
fun JsonObject.getLong(key: String) = getJsonNumber(key).longValue()

fun JsonObject.bool(key: String): Boolean? = if (isNotNullOrNULL(key)) getBoolean(key) else null
fun JsonObject.boolean(key: String) = bool(key) // Alias

fun JsonObject.date(key: String) = if (isNotNullOrNULL(key)) getDate(key) else null
fun JsonObject.getDate(key: String) : LocalDate = LocalDate.parse(getString(key))

fun JsonNumber.datetime() = LocalDateTime.ofEpochSecond(longValue(), 0, ZoneOffset.UTC)
fun JsonObject.getDateTime(key: String): LocalDateTime = getJsonNumber(key).datetime()
fun JsonObject.datetime(key: String) = if (isNotNullOrNULL(key)) getDateTime(key) else null

fun JsonObject.uuid(key: String) = if (isNotNullOrNULL(key)) getUUID(key) else null
fun JsonObject.getUUID(key: String) = UUID.fromString(getString(key))

fun JsonObject.int(key: String) = if (isNotNullOrNULL(key)) getInt(key) else null

fun JsonObject.jsonObject(key: String) = if (isNotNullOrNULL(key)) getJsonObject(key) else null
inline fun <reified T : JsonModel> JsonObject.jsonModel(key: String) = if (isNotNullOrNULL(key)) T::class.java.newInstance().apply { updateModel(getJsonObject(key)) }  else null

fun JsonObject.jsonArray(key: String) = if (isNotNullOrNULL(key)) getJsonArray(key) else null

class JsonBuilder {
    private val delegate: JsonObjectBuilder = Json.createObjectBuilder()

    fun <S, T : ObservableValue<S>> add(key: String, observable: T) {
        observable.value?.apply {
            when (this) {
                is Int -> add(key, this)
                is Double -> add(key, this)
                is Boolean -> add(key, this)
                is UUID -> add(key, this)
                is Long -> add(key, this)
                is BigDecimal -> add(key, this)
                is LocalDate -> add(key, this)
                is LocalDateTime -> add(key, this)
                is String -> add(key, this)
                is JsonModel -> add(key, this)
            }
        }
    }

    fun add(key: String, value: Double?): JsonBuilder {
        if (value != null)
            delegate.add(key, value)

        return this
    }

    fun add(key: String, value: Int?): JsonBuilder {
        if (value != null)
            delegate.add(key, value)

        return this
    }

    fun add(key: String, value: Boolean?): JsonBuilder {
        if (value != null)
            delegate.add(key, value)

        return this
    }

    fun add(key: String, value: UUID?): JsonBuilder {
        if (value != null)
            delegate.add(key, value.toString())

        return this
    }

    fun add(key: String, value: Long?): JsonBuilder {
        if (value != null)
            delegate.add(key, value)

        return this
    }

    fun add(key: String, value: BigDecimal?): JsonBuilder {
        if (value != null)
            delegate.add(key, value)

        return this
    }

    fun add(key: String, value: LocalDate?): JsonBuilder {
        if (value != null)
            delegate.add(key, value.toString())

        return this
    }

    fun add(key: String, value: LocalDateTime?): JsonBuilder {
        if (value != null)
            delegate.add(key, value.toEpochSecond(ZoneOffset.UTC))

        return this
    }

    fun add(key: String, value: String?): JsonBuilder {
        if (value != null && value.isNotBlank())
            delegate.add(key, value)

        return this
    }

    fun add(key: String, value: JsonBuilder?): JsonBuilder {
        if (value != null)
            delegate.add(key, value.build())

        return this
    }

    fun add(key: String, value: JsonObjectBuilder?): JsonBuilder {
        if (value != null)
            delegate.add(key, value.build())

        return this
    }

    fun add(key: String, value: JsonObject?): JsonBuilder {
        if (value != null)
            delegate.add(key, value)

        return this
    }

    fun add(key: String, value: JsonModel?): JsonBuilder {
        if (value != null)
            add(key, value.toJSON())

        return this
    }

    fun add(key: String, value: JsonArrayBuilder?): JsonBuilder {
        if (value != null) {
            val built = value.build()
            if (built.isNotEmpty())
                delegate.add(key, built)
        }

        return this
    }

    fun add(key: String, value: JsonArray?): JsonBuilder {
        if (value != null && value.isNotEmpty())
            delegate.add(key, value)

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
    override fun updateModel(json: JsonObject) {
        val props = this.javaClass.kotlin.memberProperties
        props.forEach {
            val pr = it.get(this)
            when (pr) {
                is BooleanProperty -> pr.value = json.bool(it.name)
                is ObjectProperty<*> -> {
                    when (it.generic()) {
                        LocalDate::class.java -> (pr as ObjectProperty<LocalDate>).value = json.date(it.name)
                    }
                }
                is LongProperty -> pr.value = json.long(it.name)
                is IntegerProperty -> pr.value = json.int(it.name)
                is DoubleProperty -> pr.value = json.double(it.name)
                is FloatProperty -> pr.value = json.double(it.name)?.toFloat()
                is StringProperty -> pr.value = json.string(it.name)
                is ObservableList<*> -> {
                    val Array = pr as ObservableList<Any>

                    val arrayObject = json.getJsonArray(it.name)
                    arrayObject?.forEach { jsonObj ->
                        val New: JsonModelAuto = it.generic().newInstance() as JsonModelAuto
                        Array.add(New.apply { updateModel(jsonObj as JsonObject) })
                    }
                }
            }
        }
    }

    override fun toJSON(json: JsonBuilder) {
        with(json) {
            val props = this@JsonModelAuto.javaClass.kotlin.memberProperties//.filter { it.isAccessible }
            props.forEach {
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
                            LocalDate::class.java -> add(it.name, (pr as ObjectProperty<LocalDate>).value)
                        }
                    }
                    is Int -> add(it.name, pr)
                    is Long -> add(it.name, pr)
                    is Double -> add(it.name, pr)
                    is Float -> add(it.name, pr.toDouble())
                    is Boolean -> add(it.name, pr)
                    is ObservableList<*> -> {
                        val Array = pr as ObservableList<JsonModel>
                        val jsonArray = Json.createArrayBuilder()
                        Array.forEach { jsonArray.add(it.toJSON()) }
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
