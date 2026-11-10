@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.professorquantumuniverse.climit.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.core.content.res.ResourcesCompat
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.toPath
import io.github.professorquantumuniverse.climit.R
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws everything Glance has no building blocks for into bitmaps: gauges, Material shapes,
 * the card outline and numbers in the variable font.
 *
 * The gauges follow the Material 3 Expressive progress indicators: round caps, a gap between the
 * active part and the track, a stop dot at the end of a linear track, and optional waves on the
 * active part only.
 */
object WidgetPainter {

    /** Wave settings, both 0..1. [amplitude] 0 means a plain indicator. */
    class Wave(val amplitude: Float, val length: Float) {
        val isFlat get() = amplitude <= 0f

        companion object {
            val NONE = Wave(0f, 0f)
        }
    }

    /** Everything about a ring that does not change between the limits of one widget. */
    class RingSpec(
        val thickness: Float,
        val wave: Wave = Wave.NONE,
        /** Length of the ring in degrees: 360 for a closed ring, less for an open one. */
        val total: Float = 360f,
        /** Canvas angle (0 = right, clockwise) where the ring starts. */
        val canvasStart: Float = -90f,
        val clockwise: Boolean = true,
    ) {
        val open: Boolean get() = total < 360f

        companion object {
            fun of(config: WidgetConfig) = RingSpec(
                thickness = config.thickness,
                wave = if (config.style.isWavy) Wave(config.waveAmplitude, config.waveLength) else Wave.NONE,
                total = if (config.ringOpen) 270f else 360f,
                canvasStart = if (config.ringOpen) 135f else -90f + config.ringStart,
                clockwise = config.clockwise,
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // Rings
    // -----------------------------------------------------------------------------------------

    fun ringStroke(sizePx: Int, thickness: Float): Float = sizePx * (0.05f + 0.11f * thickness.coerceIn(0f, 1f))

    fun ring(
        sizePx: Int,
        fraction: Float,
        color: Int,
        track: Int,
        spec: RingSpec,
        timeFraction: Float? = null,
        timeColor: Int = 0,
    ): Bitmap {
        val size = sizePx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = ringStroke(size, spec.thickness)
        val amplitude = stroke * 0.42f * spec.wave.amplitude.coerceIn(0f, 1f)
        val center = size / 2f
        val radius = center - stroke / 2f - amplitude - 1f
        if (!spec.clockwise) canvas.scale(-1f, 1f, center, center)

        drawRingTrackAndProgress(canvas, center, center, radius, stroke, amplitude, fraction, color, track, spec)

        if (timeFraction != null && timeColor != 0) {
            val timeStroke = stroke * 0.3f
            val timeRadius = radius - stroke * 0.95f
            if (timeRadius > timeStroke) {
                val paint = strokePaint(timeStroke, timeColor)
                val sweep = spec.total * timeFraction.coerceIn(0f, 1f)
                val bounds = RectF(center - timeRadius, center - timeRadius, center + timeRadius, center + timeRadius)
                canvas.drawArc(bounds, spec.canvasStart, sweep.coerceAtLeast(0.1f), false, paint)
            }
        }
        return bitmap
    }

    /** Diameter of the free space inside a ring of [sizePx], for fitting the number. */
    fun ringInner(sizePx: Int, spec: RingSpec): Float {
        val stroke = ringStroke(sizePx, spec.thickness)
        val amplitude = stroke * 0.42f * spec.wave.amplitude.coerceIn(0f, 1f)
        return (sizePx - 2f * (stroke + 2f * amplitude) - 2f).coerceAtLeast(1f)
    }

    /** All limits as nested rings in one bitmap, outermost first. */
    fun concentric(sizePx: Int, fractions: List<Float>, colors: List<Int>, track: Int, spec: RingSpec): Bitmap {
        val size = sizePx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val n = fractions.size.coerceAtLeast(1)
        val stroke = size * (0.03f + 0.05f * spec.thickness.coerceIn(0f, 1f)) * (if (n == 1) 1.6f else 1f)
        val amplitude = stroke * 0.42f * spec.wave.amplitude.coerceIn(0f, 1f)
        val gap = stroke * 0.5f + amplitude * 2f
        val center = size / 2f
        if (!spec.clockwise) canvas.scale(-1f, 1f, center, center)
        fractions.forEachIndexed { i, fraction ->
            val radius = center - stroke / 2f - amplitude - 1f - i * (stroke + gap)
            if (radius > stroke) {
                drawRingTrackAndProgress(canvas, center, center, radius, stroke, amplitude, fraction, colors[i], track, spec)
            }
        }
        return bitmap
    }

    /** Inner radius left free by [concentric], for placing the number in the middle. */
    fun concentricHole(sizePx: Int, count: Int, spec: RingSpec): Float {
        val n = count.coerceAtLeast(1)
        val stroke = sizePx * (0.03f + 0.05f * spec.thickness.coerceIn(0f, 1f)) * (if (n == 1) 1.6f else 1f)
        val amplitude = stroke * 0.42f * spec.wave.amplitude.coerceIn(0f, 1f)
        val gap = stroke * 0.5f + amplitude * 2f
        return (sizePx / 2f - n * (stroke + gap)).coerceAtLeast(0f)
    }

    private fun drawRingTrackAndProgress(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        stroke: Float,
        amplitude: Float,
        fraction: Float,
        color: Int,
        track: Int,
        spec: RingSpec,
    ) {
        val bounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val paint = strokePaint(stroke, track)
        val total = spec.total
        val progress = total * fraction.coerceIn(0f, 1f)
        // Round caps stick out by half the stroke width, so the visible gap needs that much extra.
        val capDegrees = Math.toDegrees((stroke / 2f / radius).toDouble()).toFloat()
        val gap = capDegrees * 2f + 5f
        val start = spec.canvasStart

        when {
            progress <= 0f -> canvas.drawArc(bounds, start, total, false, paint)
            progress < total -> {
                val trackStart = progress + gap
                val trackEnd = if (spec.open) total else total - gap
                if (trackEnd > trackStart) canvas.drawArc(bounds, start + trackStart, trackEnd - trackStart, false, paint)
            }
        }
        if (progress <= 0f) return

        paint.color = color
        if (amplitude <= 0f) {
            // A tiny sweep still draws a visible dot thanks to the round caps.
            canvas.drawArc(bounds, start, progress.coerceAtLeast(0.1f), false, paint)
        } else {
            canvas.drawPath(wavyArc(cx, cy, radius, start, progress, amplitude, stroke, total), paint)
        }
    }

    private fun wavyArc(
        cx: Float,
        cy: Float,
        radius: Float,
        start: Float,
        sweep: Float,
        amplitude: Float,
        stroke: Float,
        total: Float,
    ): Path {
        // A whole number of waves over the full ring, so a full ring closes cleanly.
        val waveLength = stroke * 2.4f
        val waves = ((2f * PI.toFloat() * radius * total / 360f) / waveLength).roundToInt().coerceAtLeast(3)
        val path = Path()
        val steps = (sweep * 2f).roundToInt().coerceAtLeast(2)
        for (i in 0..steps) {
            val degrees = sweep * i / steps
            val angle = Math.toRadians((start + degrees).toDouble())
            val r = radius + amplitude * sin(Math.toRadians((degrees / total * 360f * waves).toDouble())).toFloat()
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        return path
    }

    // -----------------------------------------------------------------------------------------
    // Half-circle gauge
    // -----------------------------------------------------------------------------------------

    fun gaugeHeight(widthPx: Int, thickness: Float): Int {
        val stroke = ringStroke(widthPx, thickness) * 0.8f
        return (widthPx / 2f + stroke / 2f + 2f).roundToInt()
    }

    fun gauge(widthPx: Int, fraction: Float, color: Int, track: Int, thickness: Float, timeFraction: Float?, timeColor: Int): Bitmap {
        val width = widthPx.coerceAtLeast(16)
        val stroke = ringStroke(width, thickness) * 0.8f
        val height = gaugeHeight(width, thickness)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = width / 2f - stroke / 2f - 1f
        val cx = width / 2f
        val cy = stroke / 2f + 1f + radius
        val half = RingSpec(thickness, total = 180f, canvasStart = 180f)
        drawRingTrackAndProgress(canvas, cx, cy, radius, stroke, 0f, fraction, color, track, half)
        if (timeFraction != null && timeColor != 0) {
            val angle = Math.toRadians((180.0 + 180.0 * timeFraction.coerceIn(0f, 1f)))
            val r = radius - stroke * 0.95f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = timeColor }
            canvas.drawCircle(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat(), stroke * 0.22f, paint)
        }
        return bitmap
    }

    // -----------------------------------------------------------------------------------------
    // Linear gauges
    // -----------------------------------------------------------------------------------------

    fun barStroke(density: Float, thickness: Float): Float = (4f + 12f * thickness.coerceIn(0f, 1f)) * density

    private fun barAmplitude(stroke: Float, wave: Wave): Float =
        if (wave.isFlat) 0f else (stroke * 0.3f + 2f) * (0.4f + wave.amplitude.coerceIn(0f, 1f))

    /** Bitmap height a bar needs: the stroke, room for waves, and room for the time marker. */
    fun barHeight(density: Float, thickness: Float, wave: Wave, timeMarker: Boolean): Int {
        val stroke = barStroke(density, thickness)
        val base = stroke + 2f * barAmplitude(stroke, wave)
        val marker = if (timeMarker) stroke + 8f * density else 0f
        return max(base, marker).roundToInt().coerceAtLeast(4)
    }

    fun bar(
        widthPx: Int,
        density: Float,
        fraction: Float,
        color: Int,
        track: Int,
        thickness: Float,
        wave: Wave,
        timeFraction: Float?,
        timeColor: Int,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(8)
        val stroke = barStroke(density, thickness)
        val amplitude = barAmplitude(stroke, wave)
        val showTime = timeFraction != null && timeColor != 0
        val height = barHeight(density, thickness, wave, showTime)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val middle = height / 2f
        val start = stroke / 2f
        val end = width - stroke / 2f
        val paint = strokePaint(stroke, track)

        val progress = fraction.coerceIn(0f, 1f)
        val activeEnd = start + (end - start) * progress
        val gap = stroke + 4f * density
        val trackStart = if (progress > 0f) activeEnd + gap else start
        if (trackStart < end) {
            canvas.drawLine(trackStart, middle, end, middle, paint)
            // Stop indicator: a dot in the active colour marking where 100 % is.
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawCircle(end, middle, (stroke * 0.22f).coerceAtLeast(1.5f * density), dot)
        }
        if (progress > 0f) {
            paint.color = color
            if (amplitude <= 0f || activeEnd - start < stroke) {
                canvas.drawLine(start, middle, activeEnd.coerceAtLeast(start + 0.1f), middle, paint)
            } else {
                val waveLength = stroke * (2.4f + 3.6f * wave.length.coerceIn(0f, 1f))
                val path = Path()
                var x = start
                path.moveTo(x, middle)
                while (x < activeEnd) {
                    x = (x + 2f).coerceAtMost(activeEnd)
                    path.lineTo(x, middle + amplitude * sin((x - start) / waveLength * 2f * PI.toFloat()))
                }
                canvas.drawPath(path, paint)
            }
        }
        if (showTime) drawTimeMarker(canvas, start, end, height, timeFraction!!, timeColor, density)
        return bitmap
    }

    fun segments(
        widthPx: Int,
        density: Float,
        fraction: Float,
        color: Int,
        track: Int,
        thickness: Float,
        count: Int,
        timeFraction: Float?,
        timeColor: Int,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(8)
        val n = count.coerceIn(MIN_COUNT, MAX_COUNT)
        val stroke = barStroke(density, thickness) * 1.15f
        val showTime = timeFraction != null && timeColor != 0
        val height = max(stroke, if (showTime) stroke + 8f * density else 0f).roundToInt().coerceAtLeast(4)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val gap = max(3f * density, stroke * 0.35f)
        val segment = (width - gap * (n - 1)) / n
        val top = (height - stroke) / 2f
        val radius = min(stroke / 2f, segment / 2f)
        val filled = fraction.coerceIn(0f, 1f) * n
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until n) {
            val left = i * (segment + gap)
            val rect = RectF(left, top, left + segment, top + stroke)
            paint.color = track
            when {
                i + 1 <= floor(filled) -> paint.color = color
                i < filled -> {
                    canvas.drawRoundRect(rect, radius, radius, paint)
                    canvas.save()
                    canvas.clipRect(left, 0f, left + segment * (filled - i), height.toFloat())
                    paint.color = color
                }
            }
            canvas.drawRoundRect(rect, radius, radius, paint)
            if (i < filled && i + 1 > floor(filled)) canvas.restore()
        }
        if (showTime) drawTimeMarker(canvas, 0f, width.toFloat(), height, timeFraction!!, timeColor, density)
        return bitmap
    }

    private fun drawTimeMarker(canvas: Canvas, start: Float, end: Float, height: Int, fraction: Float, color: Int, density: Float) {
        val x = start + (end - start) * fraction.coerceIn(0f, 1f)
        val w = 2.5f * density
        val paint = strokePaint(w, color)
        canvas.drawLine(x, w / 2f, x, height - w / 2f, paint)
    }

    /** Height of a row of [count] dots across [widthPx]. */
    fun dotSize(widthPx: Int, count: Int): Int {
        val n = count.coerceIn(MIN_COUNT, MAX_COUNT)
        return (widthPx / (n + (n - 1) * 0.3f)).roundToInt().coerceAtLeast(4)
    }

    fun dots(widthPx: Int, fraction: Float, color: Int, track: Int, count: Int, shape: GaugeShape): Bitmap {
        val width = widthPx.coerceAtLeast(8)
        val n = count.coerceIn(MIN_COUNT, MAX_COUNT)
        val d = dotSize(width, n)
        val bitmap = Bitmap.createBitmap(width, d, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val gap = if (n > 1) (width - d * n) / (n - 1f) else 0f
        val outline = scaledPath(gaugePolygon(shape), RectF(0f, 0f, d.toFloat(), d.toFloat()))
        val filled = fraction.coerceIn(0f, 1f) * n
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until n) {
            val left = i * (d + gap)
            canvas.save()
            canvas.translate(left, 0f)
            when {
                i + 1 <= floor(filled) -> {
                    paint.color = color
                    canvas.drawPath(outline, paint)
                }
                i < filled -> {
                    paint.color = track
                    canvas.drawPath(outline, paint)
                    canvas.clipRect(0f, 0f, d * (filled - i), d.toFloat())
                    paint.color = color
                    canvas.drawPath(outline, paint)
                }
                else -> {
                    paint.color = track
                    canvas.drawCircle(d / 2f, d / 2f, d * 0.26f, paint)
                }
            }
            canvas.restore()
        }
        return bitmap
    }

    // -----------------------------------------------------------------------------------------
    // Shapes and tiles
    // -----------------------------------------------------------------------------------------

    /** A Material shape that fills from the bottom like a level gauge. */
    fun shape(sizePx: Int, fraction: Float, shape: GaugeShape, color: Int, track: Int): Bitmap {
        val size = sizePx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val outline = scaledPath(gaugePolygon(shape), RectF(1f, 1f, size - 1f, size - 1f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = track
        canvas.drawPath(outline, paint)
        val progress = fraction.coerceIn(0f, 1f)
        if (progress > 0f) {
            canvas.save()
            canvas.clipPath(outline)
            paint.color = color
            canvas.drawRect(0f, size * (1f - progress), size.toFloat(), size.toFloat(), paint)
            canvas.restore()
        }
        return bitmap
    }

    /** A rounded tile that fills from the bottom, with a crisp line at the level. */
    fun fillTile(widthPx: Int, heightPx: Int, cornerPx: Float, fraction: Float, color: Int, density: Float): Bitmap {
        val width = widthPx.coerceAtLeast(8)
        val height = heightPx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val clip = Path().apply { addRoundRect(rect, cornerPx, cornerPx, Path.Direction.CW) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = withAlpha(color, 0.16f)
        canvas.drawPath(clip, paint)
        val progress = fraction.coerceIn(0f, 1f)
        if (progress > 0f) {
            canvas.save()
            canvas.clipPath(clip)
            val level = height * (1f - progress)
            paint.color = withAlpha(color, 0.42f)
            canvas.drawRect(0f, level, width.toFloat(), height.toFloat(), paint)
            paint.color = color
            canvas.drawRect(0f, level, width.toFloat(), level + 3f * density, paint)
            canvas.restore()
        }
        return bitmap
    }

    /** The card behind the widget: a rounded rectangle or a Material shape, with optional outline. */
    fun card(widthPx: Int, heightPx: Int, shape: CardShape, cornerPx: Float, fill: Int, outline: Int, outlineWidth: Float): Bitmap {
        val width = widthPx.coerceAtLeast(8)
        val height = heightPx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = if (outline != 0) outlineWidth / 2f else 0f
        val path = cardPath(shape, RectF(inset, inset, width - inset, height - inset), cornerPx - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (fill != 0) {
            paint.color = fill
            canvas.drawPath(path, paint)
        }
        if (outline != 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = outlineWidth
            paint.color = outline
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    fun cardPath(shape: CardShape, bounds: RectF, cornerPx: Float): Path = when (shape) {
        CardShape.ROUNDED -> Path().apply {
            val r = cornerPx.coerceIn(0f, min(bounds.width(), bounds.height()) / 2f)
            addRoundRect(bounds, r, r, Path.Direction.CW)
        }
        // A true horizontal stadium; Material's Pill is drawn diagonally and would cut into the content.
        CardShape.PILL -> Path().apply {
            val r = min(bounds.width(), bounds.height()) / 2f
            addRoundRect(bounds, r, r, Path.Direction.CW)
        }
        else -> scaledPath(cardPolygon(shape), bounds)
    }

    /**
     * How far content must stay away from each edge of a card shape (left, top, right, bottom).
     * Shapes are stretched to the widget, so their dents scale with width and height separately:
     * left/right are fractions of the width, top/bottom of the height. The pill is the exception,
     * its round ends depend on the height only.
     */
    fun cardInsets(shape: CardShape, width: Float, height: Float): FloatArray {
        fun rel(l: Float, t: Float, r: Float, b: Float) = floatArrayOf(l * width, t * height, r * width, b * height)
        return when (shape) {
            CardShape.ROUNDED -> floatArrayOf(0f, 0f, 0f, 0f)
            CardShape.PILL -> floatArrayOf(0.22f * height, 0.03f * height, 0.22f * height, 0.03f * height)
            CardShape.ARCH -> rel(0.07f, 0.13f, 0.07f, 0.03f)
            CardShape.GHOSTISH -> rel(0.07f, 0.12f, 0.07f, 0.09f)
            CardShape.SLANTED -> rel(0.09f, 0.05f, 0.09f, 0.05f)
            CardShape.COOKIE9, CardShape.SUNNY -> rel(0.1f, 0.1f, 0.1f, 0.1f)
            CardShape.COOKIE6 -> rel(0.11f, 0.11f, 0.11f, 0.11f)
            CardShape.COOKIE4, CardShape.CLOVER8, CardShape.PUFFY -> rel(0.12f, 0.12f, 0.12f, 0.12f)
            CardShape.CLOVER4 -> rel(0.15f, 0.15f, 0.15f, 0.15f)
        }
    }

    private fun scaledPath(polygon: RoundedPolygon, target: RectF): Path {
        val path = polygon.toPath(Path())
        // The exact outline, not Path.computeBounds: that includes curve control points, which
        // made every shape smaller than its box and shifted content off-centre.
        val b = polygon.calculateBounds(FloatArray(4), false)
        val bounds = RectF(b[0], b[1], b[2], b[3])
        path.transform(Matrix().apply { setRectToRect(bounds, target, Matrix.ScaleToFit.FILL) })
        return path
    }

    fun gaugePolygon(shape: GaugeShape): RoundedPolygon = when (shape) {
        GaugeShape.COOKIE -> MaterialShapes.Cookie9Sided
        GaugeShape.CLOVER -> MaterialShapes.Clover4Leaf
        GaugeShape.SUNNY -> MaterialShapes.Sunny
        GaugeShape.FLOWER -> MaterialShapes.Flower
        GaugeShape.BURST -> MaterialShapes.SoftBurst
        GaugeShape.PUFFY -> MaterialShapes.Puffy
        GaugeShape.GEM -> MaterialShapes.Gem
        GaugeShape.PILL -> MaterialShapes.Pill
        GaugeShape.HEART -> MaterialShapes.Heart
        GaugeShape.CIRCLE -> MaterialShapes.Circle
    }

    fun cardPolygon(shape: CardShape): RoundedPolygon = when (shape) {
        CardShape.ROUNDED -> MaterialShapes.Square
        CardShape.COOKIE4 -> MaterialShapes.Cookie4Sided
        CardShape.COOKIE6 -> MaterialShapes.Cookie6Sided
        CardShape.COOKIE9 -> MaterialShapes.Cookie9Sided
        CardShape.CLOVER4 -> MaterialShapes.Clover4Leaf
        CardShape.CLOVER8 -> MaterialShapes.Clover8Leaf
        CardShape.SUNNY -> MaterialShapes.Sunny
        CardShape.PUFFY -> MaterialShapes.Puffy
        CardShape.PILL -> RoundedPolygon.pill(2f, 1f)
        CardShape.ARCH -> MaterialShapes.Arch
        CardShape.SLANTED -> MaterialShapes.Slanted
        CardShape.GHOSTISH -> MaterialShapes.Ghostish
    }

    // -----------------------------------------------------------------------------------------
    // Text
    // -----------------------------------------------------------------------------------------

    @Volatile
    private var flex: Typeface? = null

    fun flexTypeface(context: Context): Typeface? =
        flex ?: runCatching { ResourcesCompat.getFont(context, R.font.roboto_flex) }.getOrNull()?.also { flex = it }

    /** Roboto Flex axis settings for the weight and width sliders (both 0..1). */
    fun flexVariation(weight: Float, width: Float, sizeDp: Float): String {
        val wght = 300 + 700 * weight.coerceIn(0f, 1f)
        val wdth = 60 + 91 * width.coerceIn(0f, 1f)
        val opsz = sizeDp.coerceIn(8f, 144f)
        return "'wght' ${wght.roundToInt()}, 'wdth' ${wdth.roundToInt()}, 'opsz' ${opsz.roundToInt()}"
    }

    /**
     * [text] as a tightly cropped bitmap in Roboto Flex. Returns null if the font is unavailable,
     * in which case the caller falls back to a system font text view.
     */
    fun number(context: Context, text: String, sizePx: Float, color: Int, weight: Float, width: Float, maxWidthPx: Float = Float.MAX_VALUE): Bitmap? {
        val typeface = flexTypeface(context) ?: return null
        val density = context.resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = sizePx
            this.color = color
            fontVariationSettings = flexVariation(weight, width, sizePx / density)
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        // Wide, heavy settings can outgrow the space; shrink the type until it fits.
        if (bounds.width() > maxWidthPx && bounds.width() > 0) {
            paint.textSize = sizePx * maxWidthPx / bounds.width()
            paint.fontVariationSettings = flexVariation(weight, width, paint.textSize / density)
            paint.getTextBounds(text, 0, text.length, bounds)
        }
        val pad = ceil(sizePx * 0.04f).toInt() + 1
        val w = bounds.width() + pad * 2
        val h = bounds.height() + pad * 2
        if (w <= 0 || h <= 0) return null
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, (pad - bounds.left).toFloat(), (pad - bounds.top).toFloat(), paint)
        return bitmap
    }

    // -----------------------------------------------------------------------------------------

    private fun strokePaint(width: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or (((color ushr 24) * alpha.coerceIn(0f, 1f)).roundToInt() shl 24)
}
