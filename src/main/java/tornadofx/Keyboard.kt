package tornadofx

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.event.EventTarget
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.SkinBase
import javafx.scene.paint.Color
import javax.json.Json
import javax.json.JsonObject

fun EventTarget.keyboard(op: KeyboardLayout.() -> Unit) = opcr(this, KeyboardLayout(), op)

class KeyboardStyles : Stylesheet() {
    init {
        keyboard {
            keyboardKey {
                borderWidth += box(1.px)
                borderColor += box(Color.BLACK)
                borderRadius += box(3.px)
                padding = box(3.px)
                backgroundInsets += box(5.px)
                backgroundRadius += box(5.px)
                backgroundColor += Color.WHITE

                and(armed) {
                    borderRadius += box(5.px)
                    backgroundInsets += box(7.px)
                    backgroundRadius += box(7.px)
                }
            }
        }
    }
}

class KeyboardLayout : Control() {
    val rows = FXCollections.observableArrayList<KeyboardRow>()

    val unitSizeProperty = SimpleDoubleProperty(50.0)
    var unitSize by unitSizeProperty

    init {
        addClass(Stylesheet.keyboard)
    }

    override fun getUserAgentStylesheet() = KeyboardStyles().base64URL.toExternalForm()

    override fun createDefaultSkin() = KeyboardSkin(this)

    fun row(op: KeyboardRow.() -> Unit) = KeyboardRow(this).apply {
        rows.add(this)
        op(this)
    }

    fun load(json: JsonObject) {
        rows.addAll(json.getJsonArray("rows").map {
            KeyboardRow.fromJSON(this, it as JsonObject)
        })
    }

    fun toJSON() = JsonBuilder()
            .add("rows", Json.createArrayBuilder().let { jsonRows ->
                rows.forEach { jsonRows.add(it.toJSON()) }
                jsonRows.build()
            })
            .build()

    fun toKeyboardLayoutEditorFormat(): String {
        val output = StringBuilder()
        rows.forEachIndexed { rowIndex, row ->
            output.append("[")
            row.keys.forEachIndexed { colIndex, key ->
                if (colIndex > 0) output.append(",")
                if (key is SpacerKeyboardKey) {
                    output.append("{x:${key.keyWidth}}")
                } else {
                    if (key.keyWidth != 1.0 || key.keyHeight != 1.0) {
                        output.append("{")
                        if (key.keyWidth != 1.0) output.append("w:${key.keyWidth}")
                        if (key.keyHeight != 1.0) output.append("h:${key.keyHeight}")
                        output.append("},")
                    }
                    output.append("\"${key.text?.replace("\\", "\\\\") ?: ""}\"")
                }
            }
            output.append("]")
            if (rowIndex < rows.size - 1) output.append(",")
            output.append("\n")
        }
        return output.toString()
    }

    internal fun addKeys(added: List<KeyboardKey>) = children.addAll(added)
    internal fun removeKeys(removed: List<KeyboardKey>) = children.removeAll(removed)
}

class KeyboardSkin(control: KeyboardLayout) : SkinBase<KeyboardLayout>(control) {
    val keyboard: KeyboardLayout = control

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        var currentX: Double
        var currentY = contentY

        keyboard.rows.forEach { row ->
            currentX = contentX
            row.keys.forEach { key ->
                if (key !is SpacerKeyboardKey) key.resizeRelocate(currentX, currentY, key.prefWidth, key.prefHeight)
                currentX += key.prefWidth
            }
            if (!row.keys.isEmpty()) {
                currentY += row.keys.map { it.prefHeight(-1.0) }.min() ?: 0.0
            }
        }
    }

    override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) = keyboard.rows.sumByDouble { row ->
        if (row.keys.isEmpty()) 0.0 else row.keys.map { it.prefHeight(width) }.min() ?: 0.0
    } + topInset + bottomInset

    override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) = (keyboard.rows.map { row ->
        if (row.keys.isEmpty()) 0.0 else row.keys.sumByDouble { it.prefWidth(height) }
    }.max() ?: 0.0) + leftInset + rightInset

    override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) = computePrefWidth(height, topInset, rightInset, bottomInset, leftInset)
    override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double) = computePrefHeight(width, topInset, rightInset, bottomInset, leftInset)
}

class KeyboardRow(val keyboard: KeyboardLayout) {
    val keys = FXCollections.observableArrayList<KeyboardKey>()

    fun spacer(width: Number = 1.0, height: Number = 1.0) = SpacerKeyboardKey(keyboard, width, height).apply {
        keys.add(this)
    }

    fun key(text: String? = null, svg: String? = null, code: Int? = null, width: Number = 1.0, height: Number = 1.0, op: KeyboardKey.() -> Unit = {}): KeyboardKey {
        val key = KeyboardKey(keyboard, text, svg, code, width, height)
        op(key)
        keys.add(key)
        return key
    }

    init {
        keys.addListener(ListChangeListener {
            while (it.next()) {
                if (it.wasAdded()) keyboard.addKeys(it.addedSubList)
                if (it.wasRemoved()) keyboard.removeKeys(it.removed)
            }
        })
    }

    companion object {
        fun fromJSON(keyboard: KeyboardLayout, json: JsonObject) = KeyboardRow(keyboard).apply {
            json.getJsonArray("keys").mapTo(keys) {
                KeyboardKey.fromJSON(keyboard, it as JsonObject)
            }
        }
    }

    fun toJSON() = JsonBuilder()
            .add("keys", Json.createArrayBuilder().let { jsonKeys ->
                keys.forEach { jsonKeys.add(it.toJSON()) }
                jsonKeys.build()
            })
            .build()
}

class SpacerKeyboardKey(keyboard: KeyboardLayout, width: Number, height: Number) : KeyboardKey(keyboard, null, null, null, width, height) {
    init {
        addClass(Stylesheet.keyboardSpacerKey)
    }

    constructor(keyboard: KeyboardLayout, json: JsonObject) : this(keyboard, json.getDouble("width"), json.getDouble("height"))
}

open class KeyboardKey(keyboard: KeyboardLayout, text: String?, svg: String?, code: Int? = null, width: Number, height: Number) : Button(text) {
    val svgProperty = SimpleStringProperty(svg)
    var svg by svgProperty

    val keyWidthProperty = SimpleDoubleProperty(width.toDouble())
    var keyWidth by keyWidthProperty

    val keyHeightProperty = SimpleDoubleProperty(height.toDouble())
    var keyHeight by keyHeightProperty

    val codeProperty = SimpleObjectProperty<Int>(code)
    var code by codeProperty

    init {
        addClass(Stylesheet.keyboardKey)
        prefWidthProperty().bind(keyWidthProperty * keyboard.unitSizeProperty)
        prefHeightProperty().bind(keyHeightProperty * keyboard.unitSizeProperty)
        svgProperty.onChange { updateGraphic() }
        updateGraphic()
    }

    private fun updateGraphic() {
        graphic = if (svg != null) svgpath(svg) else null
    }

    fun toJSON() = JsonBuilder()
            .add("type", if (this is SpacerKeyboardKey) "spacer" else null)
            .add("text", text)
            .add("svg", svg)
            .add("code", code)
            .add("width", keyWidth)
            .add("height", keyHeight)
            .build()

    constructor(keyboard: KeyboardLayout, json: JsonObject) : this(keyboard, json.string("text"), json.string("svg"), json.int("code"), json.getDouble("width"), json.getDouble("height"))

    companion object {
        fun fromJSON(keyboard: KeyboardLayout, json: JsonObject): KeyboardKey =
                if (json.getString("type", null) == "spacer") SpacerKeyboardKey(keyboard, json) else KeyboardKey(keyboard, json)
    }
}