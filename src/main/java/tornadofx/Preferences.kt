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
fun preferences(nodename: String? = null, op: Preferences.() -> Unit) {
    val node = if (nodename != null) Preferences.userRoot().node(nodename) else Preferences.userNodeForPackage(FX.application.javaClass)
    op(node)
}
