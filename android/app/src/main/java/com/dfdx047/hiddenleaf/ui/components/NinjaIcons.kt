package com.dfdx047.hiddenleaf.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The launcher's own icons, drawn for it on a 24×24 grid: ninja things (a gate, a brush, a scroll, a shuriken, kunai)
 * instead of the generic Material ones. Single-colour, so [androidx.compose.material3.Icon] tints them.
 */
object NinjaIcons {
    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.fill(evenOdd: Boolean = false, d: PathBuilder.() -> Unit) =
        path(fill = SolidColor(Color.Black), pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero, pathBuilder = d)

    private fun ImageVector.Builder.line(width: Float = 2f, d: PathBuilder.() -> Unit) =
        path(stroke = SolidColor(Color.Black), strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = d)

    /** Home: a gate with its curved top beam. */
    val Gate: ImageVector = icon("Gate") {
        fill {
            moveTo(2f, 4.2f); quadTo(12f, 6.4f, 22f, 4.2f); lineTo(21.2f, 6.6f); quadTo(12f, 8.4f, 2.8f, 6.6f); close()
            moveTo(4.5f, 9f); lineTo(19.5f, 9f); lineTo(19.5f, 10.8f); lineTo(4.5f, 10.8f); close()
            moveTo(6f, 7f); lineTo(8f, 7.2f); lineTo(8f, 21f); lineTo(6f, 21f); close()
            moveTo(16f, 7.2f); lineTo(18f, 7f); lineTo(18f, 21f); lineTo(16f, 21f); close()
            moveTo(11f, 7.6f); lineTo(13f, 7.6f); lineTo(13f, 9f); lineTo(11f, 9f); close()
        }
    }

    /** Textures: a calligraphy brush. */
    val BrushPen: ImageVector = icon("BrushPen") {
        fill {
            moveTo(20.6f, 2.2f); lineTo(21.8f, 3.4f); lineTo(11.6f, 13.6f); lineTo(10.4f, 12.4f); close()
            moveTo(9.6f, 13.2f); lineTo(10.8f, 14.4f)
            quadTo(10.6f, 18.6f, 6.4f, 20.8f); quadTo(4f, 21.8f, 2.2f, 21.8f)
            quadTo(3.2f, 20.6f, 3.6f, 18.6f); quadTo(4.4f, 14.6f, 9.6f, 13.2f); close()
        }
    }

    /** Mods: an open scroll. */
    val Scroll: ImageVector = icon("Scroll") {
        fill(evenOdd = true) {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
            moveTo(6.5f, 8f); lineTo(17.5f, 8f); lineTo(17.5f, 9.4f); lineTo(6.5f, 9.4f); close()
            moveTo(6.5f, 11.3f); lineTo(15.5f, 11.3f); lineTo(15.5f, 12.7f); lineTo(6.5f, 12.7f); close()
            moveTo(6.5f, 14.6f); lineTo(13f, 14.6f); lineTo(13f, 16f); lineTo(6.5f, 16f); close()
        }
        fill {
            moveTo(1.5f, 3.6f); lineTo(5f, 3.6f); lineTo(5f, 20.4f); lineTo(1.5f, 20.4f); close()
            moveTo(19f, 3.6f); lineTo(22.5f, 3.6f); lineTo(22.5f, 20.4f); lineTo(19f, 20.4f); close()
        }
    }

    /** Controls: a four-bladed shuriken. */
    val Shuriken: ImageVector = icon("Shuriken") {
        fill(evenOdd = true) {
            moveTo(12f, 1.5f); quadTo(16f, 6f, 15.5f, 8.5f); lineTo(22.5f, 12f); quadTo(18f, 16f, 15.5f, 15.5f)
            lineTo(12f, 22.5f); quadTo(8f, 18f, 8.5f, 15.5f); lineTo(1.5f, 12f)
            quadTo(6f, 8f, 8.5f, 8.5f); close()
            moveTo(12f, 9.6f); arcTo(2.4f, 2.4f, 0f, true, true, 11.99f, 9.6f); close()
        }
    }

    /** A kunai, point up. */
    val Kunai: ImageVector = icon("Kunai") {
        fill(evenOdd = true) {
            moveTo(12f, 1.5f); lineTo(15f, 9.5f); lineTo(12.9f, 11f); lineTo(11.1f, 11f); lineTo(9f, 9.5f); close()
            moveTo(11f, 11f); lineTo(13f, 11f); lineTo(13f, 18.5f); lineTo(11f, 18.5f); close()
            moveTo(12f, 18.2f); arcTo(2.2f, 2.2f, 0f, true, true, 11.99f, 18.2f)
            moveTo(12f, 19.4f); arcTo(1.1f, 1.1f, 0f, true, false, 12.01f, 19.4f)
        }
    }

    /** Settings: two crossed kunai, the ninja's tools. */
    val Tools: ImageVector = icon("Tools") {
        for (flip in listOf(false, true)) {
            fill {
                fun x(v: Float) = if (flip) 24f - v else v
                // blade from the top corner to the middle, handle and ring at the other corner
                moveTo(x(2.5f), 2.5f); lineTo(x(10.2f), 6.4f); lineTo(x(9.5f), 8.8f); lineTo(x(8.8f), 9.5f); lineTo(x(6.4f), 10.2f); close()
                moveTo(x(8.6f), 10f); lineTo(x(10f), 8.6f); lineTo(x(16.6f), 15.2f); lineTo(x(15.2f), 16.6f); close()
                moveTo(x(18.4f), 16.4f); arcTo(2f, 2f, 0f, true, true, x(18.39f), 16.4f)
            }
        }
    }

    /** Drivers: a chakra bolt. */
    val Bolt: ImageVector = icon("Bolt") {
        fill {
            moveTo(13.5f, 1.5f); lineTo(5f, 13.5f); lineTo(11f, 13.5f); lineTo(9.5f, 22.5f); lineTo(19f, 9.5f); lineTo(12.8f, 9.5f); close()
        }
    }

    /** Debug: an eye, watching what the game does. */
    val Eye: ImageVector = icon("Eye") {
        fill(evenOdd = true) {
            moveTo(1.5f, 12f); quadTo(12f, 1.5f, 22.5f, 12f); quadTo(12f, 22.5f, 1.5f, 12f); close()
            moveTo(12f, 7.6f); arcTo(4.4f, 4.4f, 0f, true, false, 12.01f, 7.6f); close()
        }
        fill { moveTo(12f, 10f); arcTo(2f, 2f, 0f, true, true, 11.99f, 10f); close() }
    }

    /** A leaf with its vein. */
    val Leaf: ImageVector = icon("Leaf") {
        fill(evenOdd = true) {
            moveTo(3f, 21f); quadTo(2f, 6f, 21f, 3f); quadTo(19f, 20f, 3f, 21f); close()
            moveTo(5.2f, 19.6f); lineTo(16.6f, 7.4f); lineTo(17.4f, 8.2f); lineTo(6f, 20.4f); close()
        }
    }

    /** A disc for the game data. */
    val Disc: ImageVector = icon("Disc") {
        fill(evenOdd = true) {
            moveTo(12f, 2f); arcTo(10f, 10f, 0f, true, true, 11.99f, 2f); close()
            moveTo(12f, 9.5f); arcTo(2.5f, 2.5f, 0f, true, false, 12.01f, 9.5f); close()
        }
    }

    /** The engine: a hand seal's two hands, read here as a gear of fingers. */
    val Engine: ImageVector = icon("Engine") {
        line(2f) {
            moveTo(12f, 3f); lineTo(12f, 6f); moveTo(12f, 18f); lineTo(12f, 21f)
            moveTo(3f, 12f); lineTo(6f, 12f); moveTo(18f, 12f); lineTo(21f, 12f)
            moveTo(5.6f, 5.6f); lineTo(7.8f, 7.8f); moveTo(16.2f, 16.2f); lineTo(18.4f, 18.4f)
            moveTo(18.4f, 5.6f); lineTo(16.2f, 7.8f); moveTo(7.8f, 16.2f); lineTo(5.6f, 18.4f)
        }
        fill(evenOdd = true) {
            moveTo(12f, 6.5f); arcTo(5.5f, 5.5f, 0f, true, true, 11.99f, 6.5f); close()
            moveTo(12f, 10f); arcTo(2f, 2f, 0f, true, false, 12.01f, 10f); close()
        }
    }
}
