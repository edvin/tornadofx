package tornadofx.tests

import org.junit.Test
import tornadofx.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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


    @Test
    fun testMoveAllValidIndex() {
        val list = mutableListOf(1, 2, 3, 4, 5)

        list.moveAll(2, { it < 3 })

        assertEquals(mutableListOf(3, 4, 1, 2, 5), list)
    }

    @Test
    fun testMoveAllInvalidIndex() {
        val list = mutableListOf(1, 2, 3, 4, 5)

        try {
            list.moveAll(101, { it < 3 })
        } catch (e: Exception) {
            assert(e is IllegalStateException)
        }
    }

    @Test
    fun testMoveUpAtValidIndex() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        list.moveUpAt(2)

        assertEquals(listOf(0, 2, 1, 3, 4), list)
    }

    @Test
    fun testMoveUpAtInvalidIndex() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        try {
            list.moveUpAt(10)
        } catch (e: Exception) {
            assert(e is IllegalStateException)
        }
    }

    @Test
    fun testMoveDownAtValidIndex() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        list.moveDownAt(2)

        assertEquals(listOf(0, 1, 3, 2, 4), list)
    }

    @Test
    fun testMoveDownAtInvalidIndex() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        try {
            list.moveDownAt(10)
        } catch (e: Exception) {
            assert(e is IllegalStateException)
        }
    }

    @Test
    fun testMoveUpWithValidItem() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        val result = list.moveUp(2)

        assertTrue(result)
        assertEquals(listOf(0, 2, 1, 3, 4), list)
    }

    @Test
    fun testMoveUpWithInvalidItem() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        val result = list.moveUp(10)

        assertFalse(result)
        assertEquals(listOf(0, 1, 2, 3, 4), list)
    }

    @Test
    fun testMoveDownWithValidItem() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        val result = list.moveDown(2)

        assertTrue(result)
        assertEquals(listOf(0, 1, 3, 2, 4), list)
    }

    @Test
    fun testMoveDownWithInvalidItem() {
        val list = mutableListOf(0, 1, 2, 3, 4)

        val result = list.moveDown(10)

        assertFalse(result)
        assertEquals(listOf(0, 1, 2, 3, 4), list)
    }
}