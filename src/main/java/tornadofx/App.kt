package tornadofx

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import tornadofx.FX.Companion.inheritParamHolder
import tornadofx.FX.Companion.inheritScopeHolder
import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.BUTTON1
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

// TODO Simplify constructors
open class App(
    open val primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class,
    vararg stylesheet: KClass<out Stylesheet>,
    open var scope: Scope = FX.defaultScope
) : Application(), Configurable {

    val workspace: Workspace get() = scope.workspace

    /**
     * Path to app/global configuration settings. Defaults to app.properties inside
     * the configured configBasePath (By default conf in the current directory).
     */
    override val config: ConfigProperties by lazy { loadConfig() }

    /**
     * Default path for configuration. All components will by default
     * store their configuration in a file below this path.
     */
    open val configBasePath: Path get() = Paths.get("conf")

    /**
     * Path for the application wide config element. It is `app.properties` by default,
     * in the folder provided by #configBasePath
     */
    override val configPath: Path get() = configBasePath.resolve("app.properties")

    val resources: ResourceLookup by lazy { ResourceLookup(this) }

    private val trayIcons = mutableListOf<TrayIcon>()

    init {
        Stylesheet.importServiceLoadedStylesheets()
        stylesheet.forEach { importStylesheet(it) }
    }

    constructor(
        icon: Image,
        primaryView: KClass<out UIComponent> = NoPrimaryViewSpecified::class,
        vararg stylesheet: KClass<out Stylesheet>,
        scope: Scope = FX.defaultScope
    ) : this(primaryView, *stylesheet, scope = scope) {
        addStageIcon(icon, scope)
    }

    fun <T : FXEvent> fire(event: T): Unit = FX.eventbus.fire(event)
    fun <T : Any> k(javaClass: Class<T>): KClass<T> = javaClass.kotlin

    override fun start(stage: Stage) {
        FX.registerApplication(scope, this, stage)
        detectDiContainerArgument()

        try {
            val primaryViewType = determinePrimaryView()
            val view = find(primaryViewType, scope)

            @Suppress("UNCHECKED_CAST")
            (view as? Workspace)?.let { FX.defaultWorkspace = primaryViewType as KClass<Workspace> }

            stage.apply {
                stage.aboutToBeShown = true
                view.muteDocking = true
                scene = createPrimaryScene(view)
                view.properties["tornadofx.scene"] = scene
                FX.applyStylesheetsTo(scene)
                titleProperty().bind(view.titleProperty)
                hookGlobalShortcuts()
                view.onBeforeShow()
                onBeforeShow(view)
                view.muteDocking = false
                if (view !is NoPrimaryViewSpecified && shouldShowPrimaryStage()) show()
                view.callOnDock()
                stage.aboutToBeShown = false
            }
            FX.initialized.value = true
        } catch (ex: Exception) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex)
        }
    }

    override fun stop() {
        scope.deregister()
        shutdownThreadPools()
        inheritParamHolder.remove()
        inheritScopeHolder.remove()
        trayIcons.forEach { SwingUtilities.invokeLater { SystemTray.getSystemTray().remove(it) } }
    }

    /**
     * Detect if --di-container= argument was given on the command line and assign it to FX.dicontainer
     *
     * Another implementation can still be assigned to FX.dicontainer programmatically
     */
    private fun detectDiContainerArgument() {
        parameters?.named?.get("di-container")?.let { diContainerClassName ->
            val dic = try {
                Class.forName(diContainerClassName).newInstance()
            } catch (ex: Exception) {
                log.warning("Unable to instantiate --di-container=$diContainerClassName: ${ex.message}")
                null
            }
            if (dic is DIContainer)
                FX.dicontainer = dic
            else
                log.warning("--di-container=$diContainerClassName did not resolve to an instance of tornadofx.DIContainer, ignoring assignment")
        }
    }

    open fun onBeforeShow(view: UIComponent) {}
    open fun shouldShowPrimaryStage(): Boolean = true
    open fun createPrimaryScene(view: UIComponent): Scene = Scene(view.getRootWrapper())

    @Suppress("UNCHECKED_CAST")
    private fun determinePrimaryView(): KClass<out UIComponent> {
        if (primaryView == NoPrimaryViewSpecified::class) {
            val viewClassName = parameters.named?.get("view-class") ?: return NoPrimaryViewSpecified::class
            val viewClass = Class.forName(viewClassName)

            require(UIComponent::class.java.isAssignableFrom(viewClass)) { "Class specified by --class-name is not a subclass of tornadofx.View" }
            return viewClass.kotlin as KClass<out UIComponent>
        } else {
            return primaryView
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> inject(scope: Scope = FX.defaultScope): ReadOnlyProperty<App, T> where T : Component, T : ScopedInstance =
        object : ReadOnlyProperty<App, T> {
            override fun getValue(thisRef: App, property: KProperty<*>) = find<T>(scope)
        }

    fun trayicon(
        image: BufferedImage,
        tooltip: String? = null,
        implicitExit: Boolean = false,
        autoSize: Boolean = false,
        op: TrayIcon.() -> Unit
    ) {
        Platform.setImplicitExit(implicitExit)
        SwingUtilities.invokeLater {
            Toolkit.getDefaultToolkit()
            val trayIcon = TrayIcon(image, tooltip)
            trayIcon.isImageAutoSize = autoSize
            op(trayIcon)
            SystemTray.getSystemTray().add(trayIcon)
            trayIcons.add(trayIcon)
        }
    }

    fun trayicon(
        icon: InputStream,
        tooltip: String? = null,
        implicitExit: Boolean = false,
        autoSize: Boolean = false,
        op: TrayIcon.() -> Unit
    ): Unit = trayicon(ImageIO.read(icon), tooltip, implicitExit, autoSize, op)

    fun TrayIcon.menu(label: String, op: PopupMenu.() -> Unit) {
        popupMenu = PopupMenu(label)
        op(popupMenu)
    }

    fun TrayIcon.setOnMouseClicked(fxThread: Boolean = false, button: Int = BUTTON1, clickCount: Int = 1, op: () -> Unit) {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == button && e.clickCount == clickCount) {
                    if (fxThread) runLater(op) else op()
                }
            }
        })
    }

    fun PopupMenu.item(label: String, shortcut: MenuShortcut? = null, op: MenuItem.() -> Unit): MenuItem {
        val item = MenuItem(label, shortcut)
        op(item)
        add(item)
        return item
    }

    fun PopupMenu.checkboxItem(label: String, state: Boolean = false, op: CheckboxMenuItem.() -> Unit): CheckboxMenuItem {
        val item = CheckboxMenuItem(label, state)
        op(item)
        add(item)
        return item
    }

    fun MenuItem.setOnAction(fxThread: Boolean = false, action: () -> Unit): Unit =
        addActionListener { if (fxThread) runLater(action) else action() }

    fun CheckboxMenuItem.setOnItem(fxThread: Boolean = false, action: (state: Boolean) -> Unit) {
        addItemListener {
            val state = it.stateChange == ItemEvent.SELECTED
            if (fxThread) runLater { action(state) } else action(state)
        }
    }
}

/**
 * This is the default primary view parameter. It is used to signal that there
 * is no primary view configured for the application and so the App start process
 * should not show the primary stage upon startup, unless the --view-class parameter
 * was passed on the command line.
 *
 * If no primary view is shown, the developer must use either the start() hook or some
 * other means of determining that the application has started. This would be good
 * for applications where the default view depends upon some state, or where the app
 * simply starts with a tray icon.
 */
class NoPrimaryViewSpecified : View() {
    override val root: Parent = stackpane()
}

inline fun <reified T : Application> launch(vararg args: String): Unit = Application.launch(T::class.java, *args)

@JvmName("launchWithArrayArgs")
inline fun <reified T : Application> launch(args: Array<String>): Unit = Application.launch(T::class.java, *args)
