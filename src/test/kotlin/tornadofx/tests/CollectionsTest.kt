package tornadofx.tests

import org.junit.Test
import tornadofx.*
import kotlin.test.assertEquals

class CollectionsTest {

    @Test
    fun testMoveWithValidIndex() {
        val list = mutableListOf(1, 2, 3, 4)

        list.move(2, 3)

        assertEquals(mutableListOf(1, 3, 4, 2), list)
    }

    @Test
    fun testMoveWithInvalidIndex() {
        val list = mutableListOf(1, 2, 3, 4)

        try {
            list.move(2, 102)
        } catch (e: Exception) {
            assert(e is IllegalStateException)
        }
    }

    @Test
    fun testMoveWithInvalidItem() {
        val list = mutableListOf(1, 2, 3, 4)

        list.move(10, 1)

        assertEquals(mutableListOf(1, 2, 3, 4), list)
    }

    @Test
    fun testMoveAtForward() {
        val list = mutableListOf(1, 2, 3, 4)
        val newIndex = 2

        list.moveAt(0, newIndex)

        assertEquals(1, list[newIndex])
    }

    @Test
    fun testMoveAtBackward() {
        val list = mutableListOf(1, 2, 3, 4)
        val newIndex = 0

        list.moveAt(2, newIndex)

        assertEquals(3, list[newIndex])
    }

    @Test
    fun testMoveAtWithInvalidNewIndex() {
        val list = mutableListOf(1, 2, 3, 4)
        val newIndex = 10

        try {
            list.moveAt(2, newIndex)
        } catch (e: Exception) {
            assert(e is IllegalStateException)
        }
    }
}