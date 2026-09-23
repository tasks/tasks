package org.tasks.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.createBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.tasks.compose.components.iconName
import tasks.kmp.generated.resources.Res

object MaterialSymbolsGlyphs {
    @Volatile
    private var typeface: Typeface? = null

    @OptIn(ExperimentalResourceApi::class)
    fun typeface(context: Context): Typeface = typeface ?: synchronized(this) {
        typeface ?: Typeface.createFromAsset(
            context.assets,
            Res.getUri("font/material_symbols_outlined.ttf").removePrefix("file:///android_asset/"),
        ).also { typeface = it }
    }

    fun draw(canvas: Canvas, context: Context, name: String, sizePx: Int, color: Int, left: Float = 0f, top: Float = 0f): Boolean {
        val glyph = MaterialSymbols.glyph(name.iconName) ?: return false
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = typeface(context)
            this.color = color
            textSize = sizePx.toFloat()
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(glyph, left + sizePx / 2f, top + sizePx, paint)
        return true
    }

    fun bitmap(context: Context, name: String, sizePx: Int, color: Int): Bitmap? {
        if (MaterialSymbols.glyph(name.iconName) == null) return null
        val bitmap = createBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1))
        if (!draw(Canvas(bitmap), context, name, sizePx, color)) {
            bitmap.recycle()
            return null
        }
        return bitmap
    }

    fun drawable(context: Context, name: String, sizeDp: Int, color: Int): Drawable? {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
        val key = "$name:$sizePx:$color"
        val bitmap = bitmaps[key]
            ?: bitmap(context, name, sizePx, color)?.also { bitmaps.put(key, it) }
            ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }

    private val bitmaps = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private const val MAX_CACHE_BYTES = 1024 * 1024
}
