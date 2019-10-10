package tornadofx

import javafx.geometry.Point2D
import javafx.geometry.Point3D

typealias Vector2D = Point2D

fun Point2D(value: Double): Point2D = Point2D(value, value)
fun Vector2D(value: Double): Vector2D = Vector2D(value, value)

operator fun Point2D.plus(other: Point2D): Point2D = this.add(other)
operator fun Point2D.plus(value: Double): Point2D = this.add(value, value)
operator fun Double.plus(point: Point2D): Point2D = point.add(this, this)

operator fun Point2D.minus(other: Point2D): Point2D = this.subtract(other)
operator fun Point2D.minus(value: Double): Point2D = this.subtract(value, value)

operator fun Point2D.times(factor: Double): Point2D = this.multiply(factor)
operator fun Double.times(point: Point2D): Point2D = point.multiply(this)

operator fun Point2D.div(divisor: Double): Point2D = this.multiply(1.0 / divisor)

operator fun Point2D.unaryMinus(): Point2D = this.multiply(-1.0)

infix fun Point2D.dot(other: Point2D): Double = this.dotProduct(other)
infix fun Point2D.cross(other: Point2D): Point3D = this.crossProduct(other)
infix fun Point2D.angle(other: Point2D): Double = this.angle(other)
infix fun Point2D.distance(other: Point2D): Double = this.distance(other)
infix fun Point2D.midPoint(other: Point2D): Point2D = this.midpoint(other)

/**
 * Returns the squared length of the vector/point.
 */
fun Point2D.magnitude2(): Double = this.x * this.x + this.y * this.y


typealias Vector3D = Point3D

fun Point3D(value: Double): Point3D = Point3D(value, value, value)
fun Point3D(point: Point2D, z: Double): Point3D = Point3D(point.x, point.y, z)
fun Point3D(x: Double, point: Point2D): Point3D = Point3D(x, point.x, point.y)

fun Vector3D(value: Double): Vector3D = Vector3D(value, value, value)
fun Vector3D(point: Point2D, z: Double): Vector3D = Vector3D(point.x, point.y, z)
fun Vector3D(x: Double, point: Point2D): Vector3D = Vector3D(x, point.x, point.y)

operator fun Point3D.plus(other: Point3D): Point3D = this.add(other)
operator fun Point3D.plus(value: Double): Point3D = this.add(value, value, value)
operator fun Double.plus(point: Point3D): Point3D = point.add(this, this, this)

operator fun Point3D.minus(other: Point3D): Point3D = this.subtract(other)
operator fun Point3D.minus(value: Double): Point3D = this.subtract(value, value, value)

operator fun Point3D.times(factor: Double): Point3D = this.multiply(factor)
operator fun Double.times(point: Point3D): Point3D = point.multiply(this)

operator fun Point3D.div(divisor: Double): Point3D = this.multiply(1.0 / divisor)

operator fun Point3D.unaryMinus(): Point3D = this.multiply(-1.0)

infix fun Point3D.dot(other: Point3D): Double = this.dotProduct(other)
infix fun Point3D.cross(other: Point3D): Point3D = this.crossProduct(other)
infix fun Point3D.angle(other: Point3D): Double = this.angle(other)
infix fun Point3D.distance(other: Point3D): Double = this.distance(other)
infix fun Point3D.midPoint(other: Point3D): Point3D = this.midpoint(other)

/**
 * Returns the squared length of the vector/point.
 */
fun Point3D.magnitude2(): Double = this.x * this.x + this.y * this.y + this.z * this.z


// All them swizzles... :P

val Point2D.xx: Point2D
	get() = Point2D(this.x, this.x)

val Point2D.xy: Point2D
	get() = Point2D(this.x, this.y)

val Point2D.yx: Point2D
	get() = Point2D(this.y, this.x)

val Point2D.yy: Point2D
	get() = Point2D(this.y, this.y)

val Point2D.xxx: Point3D
	get() = Point3D(this.x, this.x, this.x)

val Point2D.xxy: Point3D
	get() = Point3D(this.x, this.x, this.y)

val Point2D.xyx: Point3D
	get() = Point3D(this.x, this.y, this.x)

val Point2D.xyy: Point3D
	get() = Point3D(this.x, this.y, this.y)

val Point2D.yxx: Point3D
	get() = Point3D(this.y, this.x, this.x)

val Point2D.yxy: Point3D
	get() = Point3D(this.y, this.x, this.y)

val Point2D.yyx: Point3D
	get() = Point3D(this.y, this.y, this.x)

val Point2D.yyy: Point3D
	get() = Point3D(this.y, this.y, this.y)


val Point3D.xx: Point2D
	get() = Point2D(this.x, this.x)

val Point3D.yx: Point2D
	get() = Point2D(this.y, this.x)

val Point3D.zx: Point2D
	get() = Point2D(this.z, this.x)

val Point3D.xy: Point2D
	get() = Point2D(this.x, this.y)

val Point3D.yy: Point2D
	get() = Point2D(this.y, this.y)

val Point3D.zy: Point2D
	get() = Point2D(this.z, this.y)

val Point3D.xz: Point2D
	get() = Point2D(this.x, this.z)

val Point3D.yz: Point2D
	get() = Point2D(this.y, this.z)

val Point3D.zz: Point2D
	get() = Point2D(this.z, this.z)


val Point3D.xxx: Point3D
	get() = Point3D(this.x, this.x, this.x)

val Point3D.yxx: Point3D
	get() = Point3D(this.y, this.x, this.x)

val Point3D.zxx: Point3D
	get() = Point3D(this.z, this.x, this.x)

val Point3D.xyx: Point3D
	get() = Point3D(this.x, this.y, this.x)

val Point3D.yyx: Point3D
	get() = Point3D(this.y, this.y, this.x)

val Point3D.zyx: Point3D
	get() = Point3D(this.z, this.y, this.x)
val Point3D.xzx: Point3D
	get() = Point3D(this.x, this.z, this.x)

val Point3D.yzx: Point3D
	get() = Point3D(this.y, this.z, this.x)

val Point3D.zzx: Point3D
	get() = Point3D(this.z, this.z, this.x)

val Point3D.xxy: Point3D
	get() = Point3D(this.x, this.x, this.y)

val Point3D.yxy: Point3D
	get() = Point3D(this.y, this.x, this.y)

val Point3D.zxy: Point3D
	get() = Point3D(this.z, this.x, this.y)

val Point3D.xyy: Point3D
	get() = Point3D(this.x, this.y, this.y)

val Point3D.yyy: Point3D
	get() = Point3D(this.y, this.y, this.y)

val Point3D.zyy: Point3D
	get() = Point3D(this.z, this.y, this.y)

val Point3D.xzy: Point3D
	get() = Point3D(this.x, this.z, this.y)

val Point3D.yzy: Point3D
	get() = Point3D(this.y, this.z, this.y)

val Point3D.zzy: Point3D
	get() = Point3D(this.z, this.z, this.y)

val Point3D.xxz: Point3D
	get() = Point3D(this.x, this.x, this.z)

val Point3D.yxz: Point3D
	get() = Point3D(this.y, this.x, this.z)

val Point3D.zxz: Point3D
	get() = Point3D(this.z, this.x, this.z)

val Point3D.xyz: Point3D
	get() = Point3D(this.x, this.y, this.z)

val Point3D.yyz: Point3D
	get() = Point3D(this.y, this.y, this.z)

val Point3D.zyz: Point3D
	get() = Point3D(this.z, this.y, this.z)

val Point3D.xzz: Point3D
	get() = Point3D(this.x, this.z, this.z)

val Point3D.yzz: Point3D
	get() = Point3D(this.y, this.z, this.z)

val Point3D.zzz: Point3D
	get() = Point3D(this.z, this.z, this.z)
