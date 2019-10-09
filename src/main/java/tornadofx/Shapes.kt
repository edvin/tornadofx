package tornadofx

import javafx.scene.Parent
import javafx.scene.shape.*

fun Parent.arc(centerX: Number = 0.0, centerY: Number = 0.0, radiusX: Number = 0.0, radiusY: Number = 0.0, startAngle: Number = 0.0, length: Number = 0.0, op: Arc.() -> Unit = {}) =
        Arc(centerX.toDouble(), centerY.toDouble(), radiusX.toDouble(), radiusY.toDouble(), startAngle.toDouble(), length.toDouble()).attachTo(this, op)

fun Parent.circle(centerX: Number = 0.0, centerY: Number = 0.0, radius: Number = 0.0, op: Circle.() -> Unit = {}) =
        Circle(centerX.toDouble(), centerY.toDouble(), radius.toDouble()).attachTo(this, op)

fun Parent.cubiccurve(startX: Number = 0.0, startY: Number = 0.0, controlX1: Number = 0.0, controlY1: Number = 0.0, controlX2: Number = 0.0, controlY2: Number = 0.0, endX: Number = 0.0, endY: Number = 0.0, op: CubicCurve.() -> Unit = {}) =
        CubicCurve(startX.toDouble(), startY.toDouble(), controlX1.toDouble(), controlY1.toDouble(), controlX2.toDouble(), controlY2.toDouble(), endX.toDouble(), endY.toDouble()).attachTo(this, op)

fun Parent.ellipse(centerX: Number = 0.0, centerY: Number = 0.0, radiusX: Number = 0.0, radiusY: Number = 0.0, op: Ellipse.() -> Unit = {}) =
        Ellipse(centerX.toDouble(), centerY.toDouble(), radiusX.toDouble(), radiusY.toDouble()).attachTo(this, op)

fun Parent.line(startX: Number = 0.0, startY: Number = 0.0, endX: Number = 0.0, endY: Number = 0.0, op: Line.() -> Unit = {}) =
        Line(startX.toDouble(), startY.toDouble(), endX.toDouble(), endY.toDouble()).attachTo(this, op)

fun Parent.path(vararg elements: PathElement, op: Path.() -> Unit = {}) =  Path(*elements).attachTo(this, op)

fun Path.moveTo(x: Number = 0.0, y: Number = 0.0) = apply {
    elements.add(MoveTo(x.toDouble(), y.toDouble()))
}

fun Path.hlineTo(x: Number) = apply { elements.add(HLineTo(x.toDouble())) }

fun Path.vlineTo(y: Number) = apply { elements.add(VLineTo(y.toDouble())) }

fun Path.quadcurveTo(controlX: Number = 0.0, controlY: Number = 0.0, x: Number = 0.0, y: Number = 0.0, op: QuadCurveTo.() -> Unit = {}) = apply {
    elements.add(QuadCurveTo(controlX.toDouble(), controlY.toDouble(), x.toDouble(), y.toDouble()).also(op))
}

fun Path.cubiccurveTo(controlX1: Number = 0.0, controlY1: Number = 0.0, controlX2: Number = 0.0, controlY2: Number = 0.0, x: Number = 0.0, y: Number = 0.0, op: CubicCurveTo.() -> Unit = {}) = apply {
    elements.add(CubicCurveTo(controlX1.toDouble(), controlY1.toDouble(), controlX2.toDouble(), controlY2.toDouble(), x.toDouble(), y.toDouble()).also(op))
}

fun Path.lineTo(x: Number = 0.0, y: Number = 0.0) = apply {
    elements.add(LineTo(x.toDouble(), y.toDouble()))
}

fun Path.arcTo(
        radiusX: Number = 0.0, radiusY: Number = 0.0,
        xAxisRotation: Number = 0.0, x: Number = 0.0,
        y: Number = 0.0, largeArcFlag: Boolean = false,
        sweepFlag: Boolean = false, op: ArcTo.() -> Unit = {}) = apply{
    elements.add(ArcTo(radiusX.toDouble(), radiusY.toDouble(), xAxisRotation.toDouble(), x.toDouble(), y.toDouble(), largeArcFlag, sweepFlag).also(op))
}

fun Path.closepath() = apply { elements.add(ClosePath()) }

fun Parent.polygon(vararg points: Number, op: Polygon.() -> Unit = {}) =
        Polygon(*points.map(Number::toDouble).toDoubleArray()).attachTo(this, op)

fun Parent.polyline(vararg points: Number, op: Polyline.() -> Unit = {}) =
        Polyline(*points.map(Number::toDouble).toDoubleArray()).attachTo(this, op)

fun Parent.quadcurve(startX: Number = 0.0, startY: Number = 0.0, controlX: Number = 0.0, controlY: Number = 0.0, endX: Number = 0.0, endY: Number = 0.0, op: QuadCurve.() -> Unit = {}) =
        QuadCurve(startX.toDouble(), startY.toDouble(), controlX.toDouble(), controlY.toDouble(), endX.toDouble(), endY.toDouble()).attachTo(this, op)

fun Parent.rectangle(x: Number = 0.0, y: Number = 0.0, width: Number = 0.0, height: Number = 0.0, op: Rectangle.() -> Unit = {}) =
        Rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble()).attachTo(this, op)

fun Parent.svgpath(content: String? = null, fillRule: FillRule? = null, op: SVGPath.() -> Unit = {})= SVGPath().attachTo(this, op){
    if (content != null) it.content = content
    if (fillRule != null) it.fillRule = fillRule
}