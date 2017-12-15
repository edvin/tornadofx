package tornadofx

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.AccessController
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.util.*

private fun <EXCEPTION: Throwable, RETURN> doPrivileged(privilegedAction: ()->RETURN) : RETURN = try {
    AccessController.doPrivileged(
            PrivilegedExceptionAction<RETURN> {  privilegedAction() }
    )
} catch (e: PrivilegedActionException) {
    @Suppress("UNCHECKED_CAST")
    throw e.exception as EXCEPTION
}


object FXResourceBundleControl : ResourceBundle.Control() {

    override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle? {
        val bundleName = toBundleName(baseName, locale)
        return when (format) {

            "java.class" -> try {
                @Suppress("UNCHECKED_CAST")
                val bundleClass = loader.loadClass(bundleName) as Class<out ResourceBundle>

                // If the class isn't a ResourceBundle subclass, throw a ClassCastException.
                if (ResourceBundle::class.java.isAssignableFrom(bundleClass)) bundleClass.newInstance()
                else throw ClassCastException(bundleClass.name + " cannot be cast to ResourceBundle")

            } catch (e: ClassNotFoundException) { null}
            "java.properties" -> {
                val resourceName = toResourceName(bundleName, "properties")!!
                doPrivileged<IOException, InputStream?> {
                    if (!reload) loader.getResourceAsStream(resourceName)
                    else loader.getResource(resourceName)?.openConnection()?.apply {
                        useCaches = false // Disable caches to get fresh data for reloading.
                    } ?.inputStream
                }?.use { FXPropertyResourceBundle(InputStreamReader(it, StandardCharsets.UTF_8)) }
            }
            else -> throw IllegalArgumentException("unknown format: $format")
        }!!
    }
}

/**
 * Convenience function to support lookup via messages["key"]
 */
operator fun ResourceBundle.get(key: String) = getString(key)

class FXPropertyResourceBundle(input: InputStreamReader): PropertyResourceBundle(input) {
    fun inheritFromGlobal() {
        parent = FX.messages
    }

    /**
     * Lookup resource in this bundle. If no value, lookup in parent bundle if defined.
     * If we still have no value, return "[key]" instead of null.
     */
    override fun handleGetObject(key: String?) = super.handleGetObject(key)
            ?: parent?.getObject(key)
            ?: "[$key]"

    /**
     * Always return true, since we want to supply a default text message instead of throwing exception
     */
    override fun containsKey(key: String) = true
}

internal object EmptyResourceBundle : ResourceBundle() {
    override fun getKeys(): Enumeration<String> = Collections.emptyEnumeration()
    override fun handleGetObject(key: String) = "[$key]"
    override fun containsKey(p0: String) = true
}
