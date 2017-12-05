package tornadofx.tests

import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import kotlin.test.assertEquals

abstract class BaseInterceptor : ChildInterceptor {
	var intercepted: Boolean = false
}

class DummyPane : Pane()

fun EventTarget.dummyPane(op: DummyPane.() -> Unit = {}): DummyPane {
	return opcr(this, DummyPane(), op)
}

class FirstInterceptor : BaseInterceptor() {
	override fun invoke(parent: EventTarget, node: Node, index: Int?): Boolean = when (parent) {
		is DummyPane -> {
			intercepted = true
			true
		}
		else -> false
	}
}

class SecondInterceptor : BaseInterceptor() {
	override fun invoke(parent: EventTarget, node: Node, index: Int?): Boolean = when (parent) {
		is DummyPane -> {
			intercepted = true
			true
		}
		else -> false
	}
}

class MyTestView : View("TestView") {
	override val root = dummyPane {
		button {}
	}
}

class ChildInterceptorTest {

	companion object {
		val app = App(MyTestView::class)

		@JvmStatic
		@BeforeClass
		fun before() {
			val primaryStage: Stage = FxToolkit.registerPrimaryStage()
			FX.registerApplication(FxToolkit.setupApplication {
				app
			}, primaryStage)
		}

		@JvmStatic
		@AfterClass
		fun after() {
			FxToolkit.cleanupApplication(app)
		}
	}

	@Test
	fun interceptorsLoaded() {
		assertEquals(FX.childInterceptors.size, 2)
	}


	@Test
	fun onlyOneInterceptorShouldWork() {
		assertEquals(FX.childInterceptors.map { it as BaseInterceptor }.filter { it.intercepted }.size,
				1)
	}
}
