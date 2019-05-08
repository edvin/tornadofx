package tornadofx.tests

import javafx.geometry.Point2D
import javafx.geometry.Point3D
import org.junit.Test
import tornadofx.*
import kotlin.math.sqrt
import kotlin.test.assertEquals

class VectorMathTest {
	@Test
	fun `Point2D + Point2D`() {
		val p1 = Point2D(3.0, 4.0)
		val p2 = Point2D(4.0, 3.0)
		val result = p1 + p2
		assertEquals(Point2D(7.0, 7.0), result)
	}

	@Test
	fun `Point2D + Double`() {
		val p1 = Point2D(3.0, 4.0)
		val result = p1 + 3.0
		assertEquals(Point2D(6.0, 7.0), result)
	}

	@Test
	fun `Double + Point2D`() {
		val p1 = Point2D(3.0, 4.0)
		val result = 3.0 + p1
		assertEquals(Point2D(6.0, 7.0), result)
	}

	@Test
	fun `Point2D - Point2D`() {
		val p1 = Point2D(3.0, 4.0)
		val p2 = Point2D(4.0, 3.0)
		val result = p1 - p2
		assertEquals(Point2D(-1.0, 1.0), result)
	}

	@Test
	fun `Point2D - Double`() {
		val p1 = Point2D(3.0, 4.0)
		val result = p1 - 3.0
		assertEquals(Point2D(0.0, 1.0), result)
	}

	@Test
	fun `Point2D * Double`() {
		val p1 = Point2D(3.0, 4.0)
		val result = p1 * 3.0
		assertEquals(Point2D(9.0, 12.0), result)
	}

	@Test
	fun `Double * Point2D`() {
		val p1 = Point2D(3.0, 4.0)
		val result = 3.0 * p1
		assertEquals(Point2D(9.0, 12.0), result)
	}

	@Test
	fun `Point2D div Double`() {
		val p1 = Point2D(3.0, 12.0)
		val result = p1 / 3.0
		assertEquals(Point2D(1.0, 4.0), result)
	}

	@Test
	fun `-Point2D`() {
		val p1 = Point2D(3.0, 12.0)
		val result = -p1
		assertEquals(Point2D(-3.0, -12.0), result)
	}

	@Test
	fun `Point2D dot Point2D`() {
		val p1 = Point2D(1.0, 2.0)
		val p2 = Point2D(3.0, 4.0)
		val result = p1 dot p2
		assertEquals(11.0, result)
	}

	@Test
	fun `Point2D cross Point2D`() {
		val p1 = Point2D(1.0, 2.0)
		val p2 = Point2D(3.0, 4.0)
		val result = p1 cross p2
		assertEquals(Point3D(0.0, 0.0, -2.0), result)
	}

	@Test
	fun `Point2D angle Point2D`() {
		val p1 = Point2D(1.0, 0.0)
		val p2 = Point2D(0.0, 1.0)
		val result = p1 angle p2
		assertEquals(90.0, result)
	}

	@Test
	fun `Point2D distance Point2D`() {
		val p1 = Point2D(1.0, 0.0)
		val p2 = Point2D(0.0, 1.0)
		val result = p1 distance  p2
		assertEquals(sqrt(2.0), result)
	}

	@Test
	fun `Point2D midPoint Point2D`() {
		val p1 = Point2D(1.0, 0.0)
		val p2 = Point2D(0.0, 1.0)
		val result = p1 midPoint  p2
		assertEquals(Point2D(0.5, 0.5), result)
	}

	@Test
	fun `Point2D squared magnitude`() {
		val p1 = Point2D(5.0, 5.0)
		val result = p1.magnitude2()
		assertEquals(50.0, result)
	}

	@Test
	fun `Point3D + Point3D`() {
		val p1 = Point3D(3.0, 4.0, 2.0)
		val p2 = Point3D(4.0, 3.0, 1.0)
		val result = p1 + p2
		assertEquals(Point3D(7.0, 7.0, 3.0), result)
	}

	@Test
	fun `Point3D + Double`() {
		val p1 = Point3D(3.0, 4.0, 2.0)
		val result = p1 + 3.0
		assertEquals(Point3D(6.0, 7.0, 5.0), result)
	}

	@Test
	fun `Double + Point3D`() {
		val p1 = Point3D(3.0, 4.0, 2.0)
		val result = 3.0 + p1
		assertEquals(Point3D(6.0, 7.0, 5.0), result)
	}

	@Test
	fun `Point3D - Point3D`() {
		val p1 = Point3D(3.0, 4.0, 2.0)
		val p2 = Point3D(4.0, 3.0, 1.0)
		val result = p1 - p2
		assertEquals(Point3D(-1.0, 1.0, 1.0), result)
	}

	@Test
	fun `Point3D - Double`() {
		val p1 = Point3D(3.0, 4.0, 2.0)
		val result = p1 - 3.0
		assertEquals(Point3D(0.0, 1.0, -1.0), result)
	}

	@Test
	fun `Point3D * Double`() {
		val p1 = Point3D(3.0, 4.0, 2.0)
		val result = p1 * 3.0
		assertEquals(Point3D(9.0, 12.0, 6.0), result)
	}

	@Test
	fun `Double * Point3D`() {
		val p1 = Point3D(3.0, 4.0, 2.0)
		val result = 3.0 * p1
		assertEquals(Point3D(9.0, 12.0, 6.0), result)
	}

	@Test
	fun `Point3D div Double`() {
		val p1 = Point3D(3.0, 12.0, 6.0)
		val result = p1 / 3.0
		assertEquals(Point3D(1.0, 4.0, 2.0), result)
	}

	@Test
	fun `-Point3D`() {
		val p1 = Point3D(3.0, 12.0, -2.0)
		val result = -p1
		assertEquals(Point3D(-3.0, -12.0, 2.0), result)
	}

	@Test
	fun `Point3D dot Point3D`() {
		val p1 = Point3D(1.0, 2.0, 3.0)
		val p2 = Point3D(4.0, 5.0, 6.0)
		val result = p1 dot p2
		assertEquals(32.0, result)
	}

	@Test
	fun `Point3D cross Point3D`() {
		val p1 = Point3D(1.0, 2.0, 3.0)
		val p2 = Point3D(4.0, 5.0, 6.0)
		val result = p1 cross p2
		assertEquals(Point3D(-3.0, 6.0, -3.0), result)
	}

	@Test
	fun `Point3D angle Point3D`() {
		val p1 = Point3D(1.0, 0.0, 0.0)
		val p2 = Point3D(0.0, 1.0, 0.0)
		val result = p1 angle p2
		assertEquals(90.0, result)
	}

	@Test
	fun `Point3D distance Point3D`() {
		val p1 = Point3D(1.0, 0.0, 3.0)
		val p2 = Point3D(0.0, 1.0, 3.0)
		val result = p1 distance  p2
		assertEquals(sqrt(2.0), result)
	}

	@Test
	fun `Point3D midPoint Point3D`() {
		val p1 = Point3D(1.0, 0.0, 0.5)
		val p2 = Point3D(0.0, 1.0, 0.5)
		val result = p1 midPoint  p2
		assertEquals(Point3D(0.5, 0.5, 0.5), result)
	}
	
	@Test
	fun `Point3D squared magnitude`() {
		val p1 = Point3D(5.0, 5.0, 5.0)
		val result = p1.magnitude2()
		assertEquals(75.0, result)
	}
}
