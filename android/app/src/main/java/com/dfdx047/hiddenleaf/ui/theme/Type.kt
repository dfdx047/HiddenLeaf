package com.dfdx047.hiddenleaf.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfdx047.hiddenleaf.R

/** Shojumaru (Astigmatic, SIL OFL): brush-cut capitals for titles. */
val BrushFont = FontFamily(Font(R.font.shojumaru_regular, FontWeight.Normal))

/** Kanit (Cadson Demak, SIL OFL): the text. */
val KanitFont = FontFamily(
    Font(R.font.kanit_regular, FontWeight.Normal),
    Font(R.font.kanit_medium, FontWeight.Medium),
    Font(R.font.kanit_semibold, FontWeight.SemiBold),
    Font(R.font.kanit_bold, FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.kanit(weight: FontWeight = FontWeight.Normal) = copy(fontFamily = KanitFont, fontWeight = weight)
private fun TextStyle.brush(size: Int, spacing: Float = 1f) =
    copy(fontFamily = BrushFont, fontWeight = FontWeight.Normal, fontSize = size.sp, lineHeight = (size * 1.15f).sp, letterSpacing = spacing.sp)

val LeafTypography = Typography(
    displayLarge = base.displayLarge.brush(52),
    displayMedium = base.displayMedium.brush(42),
    displaySmall = base.displaySmall.brush(34),
    headlineLarge = base.headlineLarge.brush(28),
    headlineMedium = base.headlineMedium.brush(24),
    headlineSmall = base.headlineSmall.brush(20),
    titleLarge = base.titleLarge.kanit(FontWeight.SemiBold),
    titleMedium = base.titleMedium.kanit(FontWeight.SemiBold),
    titleSmall = base.titleSmall.kanit(FontWeight.Medium),
    bodyLarge = base.bodyLarge.kanit(),
    bodyMedium = base.bodyMedium.kanit(),
    bodySmall = base.bodySmall.kanit(),
    labelLarge = base.labelLarge.kanit(FontWeight.SemiBold).copy(letterSpacing = 0.8.sp),
    labelMedium = base.labelMedium.kanit(FontWeight.Medium),
    labelSmall = base.labelSmall.kanit(FontWeight.Medium),
)

val TitleStyle = TextStyle(fontFamily = BrushFont, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = 2.sp)

/** Cut corners, like a forehead protector's plate or a scroll's tag, instead of the round cards of Material. */
val LeafShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(6.dp),
    medium = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
    large = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp),
    extraLarge = CutCornerShape(topStart = 24.dp, bottomEnd = 24.dp),
)
