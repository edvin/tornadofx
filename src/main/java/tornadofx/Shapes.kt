package tornadofx

import javafx.scene.Parent
import javafx.scene.shape.*

fun Parent.arc(centerX: Double = 0.0, centerY: Double = 0.0, radiusX: Double = 0.0, radiusY: Double = 0.0, startAngle: Double = 0.0, length: Double = 0.0, op: (Arc.() -> Unit)? = null) =
        opcr(this, Arc(centerX, centerY, radiusX, radiusY, startAngle, length), op)

fun Parent.circle(centerX: Double = 0.0, centerY: Double = 0.0, radius: Double = 0.0, op: (Circle.() -> Unit)? = null) =
        opcr(this, Circle(centerX, centerY, radius), op)

fun Parent.cubiccurve(startX: Double = 0.0, startY: Double = 0.0, controlX1: Double = 0.0, controlY1: Double = 0.0, controlX2: Double = 0.0, controlY2: Double = 0.0, endX: Double = 0.0, endY: Double = 0.0, op: (CubicCurve.() -> Unit)? = null) =
        opcr(this, CubicCurve(startX, startY, controlX1, controlY1, controlX2, controlY2, endX, endY), op)

fun Parent.ellipse(centerX: Double = 0.0, centerY: Double = 0.0, radiusX: Double = 0.0, radiusY: Double = 0.0, op: (Ellipse.() -> Unit)? = null) =
        opcr(this, Ellipse(centerX, centerY, radiusX, radiusY), op)

fun Parent.line(startX: Double = 0.0, startY: Double = 0.0, endX: Double = 0.0, endY: Double = 0.0, op: (Line.() -> Unit)? = null) =
        opcr(this, Line(startX, startY, endX, endY), op)

fun Parent.path(vararg elements: PathElement, op: (Path.() -> Unit)? = null) =
        opcr(this, Path(*elements), op)

fun Path.moveTo(x: Double = 0.0, y: Double = 0.0): Path {
    elements.add(MoveTo(x, y)); return this
}

fun Path.hlineTo(x: Double): Path {
    elements.add(HLineTo(x)); return this
}

fun Path.vlineTo(y: Double): Path {
    elements.add(VLineTo(y)); return this
}

fun Path.quadqurveTo(controlX: Double = 0.0, controlY: Double = 0.0, x: Double = 0.0, y: Double = 0.0): Path {
    elements.add(QuadCurveTo(controlX, controlY, x, y)); return this
}

fun Path.lineTo(x: Double = 0.0, y: Double = 0.0): Path {
    elements.add(LineTo(x, y)); return this
}

fun Path.arcTo(radiusX: Double = 0.0, radiusY: Double = 0.0, xAxisRotation: Double = 0.0, x: Double = 0.0, y: Double = 0.0, largeArcFlag: Boolean = false, sweepFlag: Boolean = false): Path {
    elements.add(ArcTo(radiusX, radiusY, xAxisRotation, x, y, largeArcFlag, sweepFlag)); return this
}

fun Path.closepath(): Path {
    elements.add(ClosePath()); return this
}

fun Parent.polygon(vararg points: Double, op: (Polygon.() -> Unit)? = null) =
        opcr(this, Polygon(*points), op)

fun Parent.polyline(vararg points: Double, op: (Polyline.() -> Unit)? = null) =
        opcr(this, Polyline(*points), op)

fun Parent.quadcurve(startX: Double = 0.0, startY: Double = 0.0, controlX: Double = 0.0, controlY: Double = 0.0, endX: Double = 0.0, endY: Double = 0.0, op: (QuadCurve.() -> Unit)? = null) =
        opcr(this, QuadCurve(startX, startY, controlX, controlY, endX, endY), op)

fun Parent.rectangle(x: Double = 0.0, y: Double = 0.0, width: Double = 0.0, height: Double = 0.0, op: (Rectangle.() -> Unit)? = null) =
        opcr(this, Rectangle(x, y, width, height), op)

fun Parent.svgpath(content: String? = null, fillRule: FillRule? = null, op: (SVGPath.() -> Unit)? = null): SVGPath {
    val p = SVGPath()
    if (content != null) p.content = content
    if (fillRule != null) p.fillRule = fillRule
    return opcr(this, p, op)
}