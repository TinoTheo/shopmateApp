package com.shopmate.ui.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.shopmate.R

/**
 * Drawing helpers shared by the shared image and the printed slip. Text is set in the bundled
 * Inter, so the brand type survives on any printer and any phone. Figures use tabular numerals
 * ([figures] = true); names don't, because Inter's tabular set also widens hyphens.
 *
 * Both receipts lay out in one pass that is run twice: once with no canvas to measure the height,
 * then on a bitmap of exactly that height. [canvas] is null during the measuring pass.
 */
internal class ReceiptPainter(context: Context, private val canvas: Canvas?) {

    private val regular = font(context, R.font.inter_regular)
    private val medium = font(context, R.font.inter_medium)
    private val semibold = font(context, R.font.inter_semibold)
    private val mark = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_shopmate_mark, null)!!

    enum class Weight { Regular, Medium, SemiBold }

    fun paint(
        sizePx: Float,
        weight: Weight,
        color: Int,
        tracking: Float = 0f,
        figures: Boolean = false,
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = when (weight) {
            Weight.Regular -> regular
            Weight.Medium -> medium
            Weight.SemiBold -> semibold
        }
        textSize = sizePx
        this.color = color
        letterSpacing = tracking
        if (figures) fontFeatureSettings = "tnum"
    }

    /** Height a line of [paint] occupies, ascent to descent. */
    fun lineHeight(paint: TextPaint): Float = paint.fontMetrics.let { it.descent - it.ascent }

    /** Draws [text] with its top at [top]; ellipsised to [maxWidth]. Returns the line height. */
    fun text(text: String, x: Float, top: Float, paint: TextPaint, maxWidth: Float = Float.MAX_VALUE): Float {
        val fitted = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        canvas?.drawText(fitted, x, top - paint.fontMetrics.ascent, paint)
        return lineHeight(paint)
    }

    /** As [text], right-aligned to [right]. */
    fun textEnd(text: String, right: Float, top: Float, paint: TextPaint): Float =
        text(text, right - paint.measureText(text), top, paint)

    fun width(text: String, paint: TextPaint): Float = paint.measureText(text)

    fun rect(left: Float, top: Float, right: Float, bottom: Float, color: Int, radius: Float = 0f) {
        canvas?.drawRoundRect(RectF(left, top, right, bottom), radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    fun line(left: Float, y: Float, right: Float, color: Int, strokePx: Float, dashPx: Float? = null) {
        val paint = Paint().apply {
            this.color = color
            strokeWidth = strokePx
            if (dashPx != null) pathEffect = android.graphics.DashPathEffect(floatArrayOf(dashPx, dashPx), 0f)
        }
        canvas?.drawLine(left, y, right, y, paint)
    }

    /** The looped M, fitted into the given box and tinted [color]. */
    fun mark(left: Float, top: Float, width: Float, height: Float, color: Int) {
        val c = canvas ?: return
        mark.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        mark.setBounds(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt())
        mark.draw(c)
    }

    companion object {
        /**
         * Runs [layout] once to measure, then again onto a [width]-wide bitmap of the measured
         * height. [layout] returns the height it used.
         */
        fun render(context: Context, width: Int, layout: (ReceiptPainter) -> Float): Bitmap {
            val height = layout(ReceiptPainter(context, null)).toInt().coerceAtLeast(1)
            val bitmap = createBitmap(width, height)
            layout(ReceiptPainter(context, Canvas(bitmap)))
            return bitmap
        }

        private fun font(context: Context, res: Int): Typeface =
            ResourcesCompat.getFont(context, res) ?: Typeface.DEFAULT
    }
}
