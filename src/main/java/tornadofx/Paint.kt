package tornadofx

import javafx.geometry.Insets
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Paint

/**
 * Converts the given Paint to a Background
 */
fun Paint.asBackground(radii: CornerRadii = CornerRadii.EMPTY, insets: Insets = Insets.EMPTY) =
        Background(BackgroundFill(this, radii, insets))