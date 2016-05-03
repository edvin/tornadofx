package tornadofx

import java.util.prefs.Preferences

/**
 * Store and retrieve preferences.
 *
 * Preferences are stored automatically in a OS specific way.
 * <ul>
 *     <li>Windows stores it in the registry at HKEY_CURRENT_USER/Software/JavaSoft/....</li>
 *     <li>Mac OS stores it at ~/Library/Preferences/com.apple.java.util.prefs.plist</li>
 *     <li>Linux stores it at ~/.java</li>
 * </ul>
 */
private var _nodename: String? = null
private val _preferences: Preferences by lazy {
    Preferences.userRoot().node(_nodename)
}

fun preferences(nodename: String? = null, op: Preferences.() -> Unit) {
    if (_nodename == null) _nodename = nodename ?: FX.application.javaClass.canonicalName
    _preferences.op()
}