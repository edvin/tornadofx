package tornadofx

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonObjectBuilder

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
    fun toJSON(json: JsonObjectBuilder) {

    }

    /**
     * Copy all properties from this object to the given target object by converting to JSON and then updating the target.
     * @param target The target object to update with the properties of this model
     */
    fun copy(target: JsonModel) {
        val builder = Json.createObjectBuilder()
        toJSON(builder)
        target.updateModel(builder.build())
    }

    /**
     * Copy all properties from the given source object to this object by converting to JSON and then updating this object.
     * @param source The source object to extract properties from
     */
    fun update(source: JsonModel) {
        val builder = Json.createObjectBuilder()
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
            val builder = Json.createObjectBuilder()
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

fun JsonObject.string(key: String) = if (containsKey(key)) getString(key) else null

fun JsonObject.double(key: String) = if (containsKey(key)) getJsonNumber(key).doubleValue() else null

fun JsonObject.bool(key: String): Boolean? = if (containsKey(key)) getBoolean(key) else null

fun JsonObject.date(key: String) = if (containsKey(key)) LocalDate.parse(getString(key)) else null

fun JsonObject.datetime(key: String) = if (containsKey(key))
    LocalDateTime.ofEpochSecond(getJsonNumber(key).longValue(), 0, ZoneOffset.UTC) else null

fun JsonObject.uuid(key: String) = if (containsKey(key)) UUID.fromString(getString(key)) else null

fun JsonObject.int(key: String) = if (containsKey(key)) getInt(key) else null

fun JsonObject.jsonObject(key: String) = if (containsKey(key)) getJsonObject(key) else null

fun JsonObject.jsonArray(key: String) = if (containsKey(key)) getJsonArray(key) else null