package tornadofx

import java.io.IOException
import java.io.InputStream
import java.security.AccessController
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.util.*

class FXResourceBundleControl private constructor(): ResourceBundle.Control() {
    companion object {
        val INSTANCE = FXResourceBundleControl()
    }

    override fun newBundle(baseName: String, locale: Locale, format: String,
                  loader: ClassLoader, reload: Boolean): ResourceBundle {
        val bundleName = toBundleName(baseName, locale)
        var bundle: ResourceBundle? = null
        if (format == "java.class") {
            try {
                @Suppress("UNCHECKED_CAST")
                val bundleClass = loader.loadClass(bundleName) as Class<out ResourceBundle>

                // If the class isn't a ResourceBundle subclass, throw a
                // ClassCastException.
                if (ResourceBundle::class.java.isAssignableFrom(bundleClass)) {
                    bundle = bundleClass.newInstance()
                } else {
                    throw ClassCastException(bundleClass.name + " cannot be cast to ResourceBundle")
                }
            } catch (e: ClassNotFoundException) {
            }

        } else if (format == "java.properties") {
            val resourceName = toResourceName(bundleName, "properties") ?: return bundle!!
            val classLoader = loader
            val reloadFlag = reload
            val stream: InputStream?
            try {
                stream = AccessController.doPrivileged(
                        PrivilegedExceptionAction<java.io.InputStream> {
                            var `is`: InputStream? = null
                            if (reloadFlag) {
                                val url = classLoader.getResource(resourceName)
                                if (url != null) {
                                    val connection = url.openConnection()
                                    if (connection != null) {
                                        // Disable caches to get fresh filtredItems for
                                        // reloading.
                                        connection.useCaches = false
                                        `is` = connection.inputStream
                                    }
                                }
                            } else {
                                `is` = classLoader.getResourceAsStream(resourceName)
                            }
                            `is`
                        })
            } catch (e: PrivilegedActionException) {
                throw e.exception as IOException
            }

            if (stream != null) {
                try {
                    bundle = FXPropertyResourceBundle(stream)
                } finally {
                    stream.close()
                }
            }
        } else {
            throw IllegalArgumentException("unknown format: " + format)
        }
        return bundle!!
    }

}

/**
 * Convenience function to support lookup via messages["key"]
 */
operator fun ResourceBundle.get(key: String) = getString(key)

class FXPropertyResourceBundle(input: InputStream): PropertyResourceBundle(input) {
    fun inheritFromGlobal() {
        setParent(FX.messages)
    }

    /**
     * Lookup resource in this bundle. If no value, lookup in parent bundle if defined.
     * If we still have no value, return "[key]" instead of null.
     */
    override fun handleGetObject(key: String?): Any {
        var value = super.handleGetObject(key)
        if (value == null && parent != null)
            value = parent.getObject(key)

        return value ?: "[$key]"
    }

    /**
     * Always return true, since we want to supply a default text message instead of throwing exception
     */
    override fun containsKey(key: String) = true
}

internal class EmptyResourceBundle private constructor() : ResourceBundle() {
    override fun getKeys(): Enumeration<String> = Collections.emptyEnumeration()
    override fun handleGetObject(key: String) = "[$key]"
    override fun containsKey(p0: String) = true

    companion object {
        val INSTANCE = EmptyResourceBundle()
    }
}
