package tornadofx

import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject

/**
 * This File contains all config releated classes and functions for tornadofx.
 *
 * @author nimakro
 */

/**
 * Defines the context in which the settings should be saved.
 */
enum class Context {
    /**
     * In this context all settings will be stored for a specific component.
     */
    COMPONENT,

    /**
     * This context stores all settings in a global space, which can be accessed from all components.
     */
    GLOBAL
}

/**
 * Defines a configuration object, which allows to save and restore key value pairs.
 * @param configTarget The fully qualified path to the storage loaction of the config file.
 * @param fileName The filename without a file extension.
 */
class Config(fileName: String, configTarget: Path): Properties() {

    private val path = lazy {
        if (!Files.exists(configTarget))
            Files.createDirectories(configTarget)
        configTarget.resolve(fileName + ".properties")
    }

    /**
     * Stores for the given key the associated value in the config.
     * If the config contains already the given key with a value, the value
     * will be overriden.
     */
    fun set(pair: Pair<String, Any>) = set(pair.first, pair.second.toString());

    /*
     * Tries to read the String associated with the given key.
     * If no matching key could be found <b>null</b> will be returned.
     *
     * To use a default value, you can use orElse.
     * @sample string("title").orElse("New Title")
     */
    fun string(key: String): String? = getProperty(key)

    /*
     * Tries to read the boolean associated with the given key.
     * If no matching key could be found <b>null</b> will be returned.
     *
     * To use a default value, you can use orElse.
     * @sample boolean("showTitle).orElse(false)
     */
    fun boolean(key: String): Boolean? = getProperty(key)?.toBoolean()

    /*
     * Tries to read the long associated with the given key.
     * If no matching key could be found <b>null</b> will be returned.
     *
     * To use a default value, you can use oreElse.
     * @sample long("items").orElse(0)
     */
    fun long(key: String): Long? = getProperty(key)?.toLong()

    /*
     * Tries to read the double associated with the given key.
     * If no matching key could be found <b>null</b> will be returned.
     *
     * To use a default value, you can use orElse.
     * @sample string("title").orElse("New Title")
     */
    fun double(key: String): Double? = getProperty(key)?.toDouble()

    /*
     * Tries to read the JsonObject associated with the given key.
     * If no matching key could be found <b>null</b> will be returned.
     *
     * To use a default value, you can use orElse.
     * @sample jsonObject("user.info").orElse(otherJsonObject)
     */
    fun jsonObject(key: String): JsonObject? = getProperty(key)?.let { Json.createReader(StringReader(it)).readObject() }

    /*
     * Tries to read the JsonArray associated with the given key.
     * If no matching key could be found <b>null</b> will be returned.
     *
     * To use a default value, you can use orElse.
     * @sample jsonArray("user.info").orElse(otherJsonArray)
     */
    fun jsonArray(key: String): JsonArray? = getProperty(key)?.let { Json.createReader(StringReader(it)).readArray() }

    /**
     * Saves the given config object.
     * @param description A description of the config-file.
     */

    fun save(description: String = "") = Files.newOutputStream(path.value).use {output -> store(output, description)}

    /**
     * Loads the given config object.
     * If no config file can be found, this function is a no op.
     */
    fun load() {
        if (Files.exists(path.value))
            Files.newInputStream(path.value).use { load(it) }
    }
}

//-- Helper Functions

/**
 * Load the config file if it exists and do something with it.
 */
fun Component.config(context: Context = Context.COMPONENT, op: Config.() -> Unit) {
    if (context == Context.COMPONENT)
        op(config)
    else if (context == Context.GLOBAL)
        op((FX.getApplication(scope)!! as App).config)
    else throw IllegalStateException("State: $context not known!")
}

/**
 * Loads the system config file if it exists and do something with it.
 */
fun App.config(op: Config.() -> Unit) {
    op(config)
}

//-- Default value helper
/**
 * Returns this Boolean if not null, otherwise return [other].
 */
infix fun Boolean?.orElse(other: Boolean): Boolean = orElse(other)

/**
 * Returns this String if not null, otherwise return [other].
 */
infix fun String?.orElse(other: String): String = if (this != null) this else other

/**
 * Returns this Double if not null, otherwise return [other].
 */
infix fun Double?.orElse(other: Double): Double = if (this != null) this else other

/**
 * Returns this Long if not null, otherwise return [other].
 */
infix fun Long?.orElse(other: Long): Long = if (this != null) this else other

/**
 * Returns this JsonObject if not null, otherwise return [other].
 */
infix fun JsonObject?.orElse(other: JsonObject): JsonObject = if (this != null) this else other

/**
 * Returns this JsonObject if not null, otherwise return [other].
 */
infix fun JsonArray?.orElse(other: JsonArray): JsonArray = if (this != null) this else other

