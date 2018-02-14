package tornadofx.tests

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.testfx.api.FxRobot
import org.testfx.api.FxToolkit
import tornadofx.*
import tornadofx.testapps.MultipleLifecycleAsyncApp
import tornadofx.testapps.MultipleLifecycleAsyncController
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(Parameterized::class)
class AsyncBugAppTest(val rounds: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Int>> {
            return listOf(arrayOf(1), arrayOf(1))
        }
    }

    lateinit var robot: FxRobot
    lateinit var app: App

    @RelaxedMockK
    lateinit var controller: MultipleLifecycleAsyncController

    @Rule
    @JvmField
    val timeout = Timeout(10, TimeUnit.SECONDS)

    @Before
    fun before() {
        MockKAnnotations.init(this)

        FxToolkit.registerPrimaryStage()
        app = MultipleLifecycleAsyncApp()
        app.scope.set(controller)
        FxToolkit.setupApplication { app }
        robot = FxRobot()

        println("rounds = $rounds")
    }

    @After
    fun after() {
        FxToolkit.cleanupStages()
        FxToolkit.cleanupApplication(app)
    }

    @Test(timeout = 20000)
    fun itShouldSurviveRunAsyncMultipleTimes() {
//        val latch = CountDownLatch(rounds)
//        every { controller.onAction(any()) }.answers { latch.countDown() }
//
//        var i = 0
//        while (i < rounds) {
//            robot.clickOn("#bug")
//            i++
//        }
//
//        latch.await()
//        verify(exactly = rounds) { controller.onAction(any()) }
    }
}