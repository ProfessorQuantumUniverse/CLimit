@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)

package io.github.professorquantumuniverse.climit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.professorquantumuniverse.climit.R

/**
 * Roboto Flex with explicit axis values. Variable fonts let the large numbers grow wider and
 * heavier than any static weight, one of the signature looks of Material 3 Expressive.
 */
fun flexFamily(weight: Int, width: Float = 100f): FontFamily = FontFamily(
    Font(
        R.font.roboto_flex,
        weight = FontWeight(weight.coerceIn(100, 1000)),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.coerceIn(100, 1000)),
            FontVariation.width(width.coerceIn(25f, 151f)),
        ),
    )
)

private val DisplayFlex = flexFamily(weight = 650, width = 112f)
private val HeadlineFlex = flexFamily(weight = 600, width = 105f)

private fun TextStyle.flex(family: FontFamily) = copy(fontFamily = family, fontWeight = null)

/** Material type scale with Roboto Flex for display and headline styles; body text stays system. */
private fun expressiveTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.flex(DisplayFlex),
        displayMedium = base.displayMedium.flex(DisplayFlex),
        displaySmall = base.displaySmall.flex(DisplayFlex),
        headlineLarge = base.headlineLarge.flex(HeadlineFlex),
        headlineMedium = base.headlineMedium.flex(HeadlineFlex),
        headlineSmall = base.headlineSmall.flex(HeadlineFlex),
        displayLargeEmphasized = base.displayLargeEmphasized.flex(flexFamily(800, 120f)),
        displayMediumEmphasized = base.displayMediumEmphasized.flex(flexFamily(800, 120f)),
        displaySmallEmphasized = base.displaySmallEmphasized.flex(flexFamily(800, 120f)),
        headlineLargeEmphasized = base.headlineLargeEmphasized.flex(flexFamily(750, 115f)),
        headlineMediumEmphasized = base.headlineMediumEmphasized.flex(flexFamily(750, 115f)),
        headlineSmallEmphasized = base.headlineSmallEmphasized.flex(flexFamily(700, 110f)),
    )
}

private val ExpressiveTypography = expressiveTypography()

/** Material 3 Expressive with the device's dynamic colours, expressive motion and Roboto Flex. */
@Composable
fun ClimitTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = ExpressiveTypography,
        content = content,
    )
}
