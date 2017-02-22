package tornadofx

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.BUTTON1
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.*
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class App(primaryView: KClass<out UIComponent>? = null, vararg stylesheet: KClass<out Stylesheet>) : Application() {
    var scope: Scope = DefaultScope
    val workspace: Workspace get() = scope.workspace
    private val trayIcons = ArrayList<TrayIcon>()
    val resources: ResourceLookup by lazy {
        ResourceLookup(this)
    }

    fun <T : FXEvent> fire(event: T) {
        FX.eventbus.fire(event)
    }

    constructor(primaryView: KClass<out UIComponent>? = null, stylesheet: KClass<out Stylesheet>, scope: Scope = DefaultScope) : this(primaryView, *arrayOf(stylesheet)) {
        this.scope = scope
    }

    constructor(icon: Image, primaryView: KClass<out UIComponent>? = null, vararg stylesheet: KClass<out Stylesheet>) : this(primaryView, *stylesheet) {
        addStageIcon(icon, scope)
    }

    constructor() : this(null)

    open val primaryView: KClass<out UIComponent> = primaryView ?: DeterminedByParameter::class

    init {
        Stylesheet.importServiceLoadedStylesheets()
        stylesheet.forEach { importStylesheet(it) }
    }

    override fun start(stage: Stage) {
        FX.registerApplication(scope, this, stage)

        try {
            val primaryViewType = determinePrimaryView()
            val view = find(primaryViewType, scope)

            @Suppress("UNCHECKED_CAST")
            if (view is Workspace) FX.defaultWorkspace = primaryViewType as KClass<Workspace>

            stage.apply {
                scene = createPrimaryScene(view)
                view.properties["tornadofx.scene"] = scene
                FX.applyStylesheetsTo(scene)
                titleProperty().bind(view.titleProperty)
                hookGlobalShortcuts()
                onBeforeShow(view)
                if (shouldShowPrimaryStage()) show()
            }
            FX.initialized.value = true
        } catch (ex: Exception) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), ex)
        }
    }

    open fun onBeforeShow(view: UIComponent) {

    }

    override fun stop() {
        scope.deregister()
        trayIcons.forEach {
            SwingUtilities.invokeLater { SystemTray.getSystemTray().remove(it) }
        }
    }

    open fun shouldShowPrimaryStage() = true

    open fun createPrimaryScene(view: UIComponent) = Scene(view.root)

    @Suppress("UNCHECKED_CAST")
    private fun determinePrimaryView(): KClass<out UIComponent> {
        if (primaryView == DeterminedByParameter::class) {
            val viewClassName = parameters.named?.get("view-class") ?: throw IllegalArgumentException("No provided --view-class parameter and primaryView was not overridden. Choose one strategy to specify the primary View")
            val viewClass = Class.forName(viewClassName)
            if (UIComponent::class.java.isAssignableFrom(viewClass)) return viewClass.kotlin as KClass<out UIComponent>
            throw IllegalArgumentException("Class specified by --class-name is not a subclass of tornadofx.View")
         } else {
            return primaryView
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> inject(scope: Scope = DefaultScope): ReadOnlyProperty<App, T> where T : Component, T: Injectable = object : ReadOnlyProperty<App, T> {
        override fun getValue(thisRef: App, property: KProperty<*>) = find(T::class, scope)
    }

    class DeterminedByParameter : View() {
        override val root = Pane()
    }

    fun trayicon(image: BufferedImage, tooltip: String?, implicitExit: Boolean = false, resizeIcon: Boolean = false, op: TrayIcon.() -> Unit){
        Platform.setImplicitExit(implicitExit)
        SwingUtilities.invokeLater {
            Toolkit.getDefaultToolkit()
            val trayIcon = TrayIcon(image, tooltip)
            trayIcon.isImageAutoSize=resizeIcon
            op(trayIcon)
            SystemTray.getSystemTray().add(trayIcon)
            trayIcons.add(trayIcon)
        }
    }
    fun trayicon(icon: InputStream, tooltip: String? = null, implicitExit: Boolean = false, resizeIcon: Boolean = false,  op: TrayIcon.() -> Unit) {
        trayicon(ImageIO.read(icon), tooltip, implicitExit,resizeIcon, op)
    }

    fun TrayIcon.menu(label: String, op: PopupMenu.() -> Unit) {
        popupMenu = PopupMenu(label)
        op(popupMenu)
    }

    fun TrayIcon.setOnMouseClicked(fxThread: Boolean = false, button: Int = BUTTON1, clickCount: Int = 1, op: () -> Unit) {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == button && e.clickCount == clickCount) {
                    if (fxThread) {
                        Platform.runLater {
                            op()
                        }
                    } else {
                        op()
                    }
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

    fun MenuItem.setOnAction(fxThread: Boolean = false, action: () -> Unit) {
        addActionListener {
            if (fxThread) {
                Platform.runLater {
                    action()
                }
            } else {
                action()
            }
        }
    }

}