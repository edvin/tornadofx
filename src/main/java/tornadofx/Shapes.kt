package tornadofx

import javafx.scene.Parent
import javafx.scene.shape.*

fun Parent.arc(
    centerX: Number = 0.0, centerY: Number = 0.0,
    radiusX: Number = 0.0, radiusY: Number = 0.0,
    startAngle: Number = 0.0,
    length: Number = 0.0,
    op: Arc.() -> Unit = {}
): Arc = Arc(
    centerX.toDouble(), centerY.toDouble(),
    radiusX.toDouble(), radiusY.toDouble(),
    startAngle.toDouble(),
    length.toDouble()
).attachTo(this, op)

fun Parent.circle(
    centerX: Number = 0.0, centerY: Number = 0.0,
    radius: Number = 0.0,
    op: Circle.() -> Unit = {}
): Circle = Circle(
    centerX.toDouble(), centerY.toDouble(),
    radius.toDouble()
).attachTo(this, op)

fun Parent.cubiccurve(
    startX: Number = 0.0, startY: Number = 0.0,
    controlX1: Number = 0.0, controlY1: Number = 0.0,
    controlX2: Number = 0.0, controlY2: Number = 0.0,
    endX: Number = 0.0, endY: Number = 0.0,
    op: CubicCurve.() -> Unit = {}
): CubicCurve = CubicCurve(
    startX.toDouble(), startY.toDouble(),
    controlX1.toDouble(), controlY1.toDouble(),
    controlX2.toDouble(), controlY2.toDouble(),
    endX.toDouble(), endY.toDouble()
).attachTo(this, op)

fun Parent.ellipse(
    centerX: Number = 0.0, centerY: Number = 0.0,
    radiusX: Number = 0.0, radiusY: Number = 0.0,
    op: Ellipse.() -> Unit = {}
): Ellipse = Ellipse(
    centerX.toDouble(), centerY.toDouble(),
    radiusX.toDouble(), radiusY.toDouble()
).attachTo(this, op)

fun Parent.line(
    startX: Number = 0.0, startY: Number = 0.0,
    endX: Number = 0.0, endY: Number = 0.0,
    op: Line.() -> Unit = {}
): Line = Line(
    startX.toDouble(), startY.toDouble(),
    endX.toDouble(), endY.toDouble()
).attachTo(this, op)

fun Parent.path(vararg elements: PathElement, op: Path.() -> Unit = {}): Path = Path(*elements).attachTo(this, op)

fun Path.moveTo(x: Number = 0.0, y: Number = 0.0): Path = apply { elements.add(MoveTo(x.toDouble(), y.toDouble())) }

fun Path.hlineTo(x: Number): Path = apply { elements.add(HLineTo(x.toDouble())) }
fun Path.vlineTo(y: Number): Path = apply { elements.add(VLineTo(y.toDouble())) }

fun Path.quadqurveTo(
    controlX: Number = 0.0, controlY: Number = 0.0,
    x: Number = 0.0, y: Number = 0.0,
    op: QuadCurveTo.() -> Unit = {}
): Path = apply { elements.add(QuadCurveTo(controlX.toDouble(), controlY.toDouble(), x.toDouble(), y.toDouble()).also(op)) }

fun Path.lineTo(x: Number = 0.0, y: Number = 0.0): Path = apply { elements.add(LineTo(x.toDouble(), y.toDouble())) }

fun Path.arcTo(
    radiusX: Number = 0.0, radiusY: Number = 0.0,
    xAxisRotation: Number = 0.0,
    x: Number = 0.0, y: Number = 0.0,
    largeArcFlag: Boolean = false,
    sweepFlag: Boolean = false,
    op: ArcTo.() -> Unit = {}
): Path = apply {
    elements.add(ArcTo(radiusX.toDouble(), radiusY.toDouble(), xAxisRotation.toDouble(), x.toDouble(), y.toDouble(), largeArcFlag, sweepFlag).also(op))
}

fun Path.closepath(): Path = apply { elements.add(ClosePath()) }

fun Parent.polygon(vararg points: Number, op: Polygon.() -> Unit = {}): Polygon = Polygon(*points.map(Number::toDouble).toDoubleArray()).attachTo(this, op)
fun Parent.polyline(vararg points: Number, op: Polyline.() -> Unit = {}): Polyline = Polyline(*points.map(Number::toDouble).toDoubleArray()).attachTo(this, op)

fun Parent.quadcurve(
    startX: Number = 0.0, startY: Number = 0.0,
    controlX: Number = 0.0, controlY: Number = 0.0,
    endX: Number = 0.0, endY: Number = 0.0,
    op: QuadCurve.() -> Unit = {}
): QuadCurve = QuadCurve(
    startX.toDouble(), startY.toDouble(),
    controlX.toDouble(), controlY.toDouble(),
    endX.toDouble(), endY.toDouble()
).attachTo(this, op)

fun Parent.rectangle(
    x: Number = 0.0, y: Number = 0.0,
    width: Number = 0.0, height: Number = 0.0,
    op: Rectangle.() -> Unit = {}
): Rectangle = Rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble()).attachTo(this, op)

fun Parent.svgpath(content: String? = null, fillRule: FillRule? = null, op: SVGPath.() -> Unit = {}): SVGPath = SVGPath().attachTo(this, op) {
    if (content != null) it.content = content
    if (fillRule != null) it.fillRule = fillRule
}
