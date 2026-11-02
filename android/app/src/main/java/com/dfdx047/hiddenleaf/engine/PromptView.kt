package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import com.dfdx047.hiddenleaf.touch.Ps2

/**
 * The button the game asks for, big and near the right thumb, over the controller; a sequence goes in a row, its last one
 * under the thumb. Touches on the left half go on to the controller; nothing is taken when the game asks for nothing.
 * Codes as in hl_native's updatePrompt: 1 circle 2 triangle 3 square 4 cross 5 up 6 right 7 down 8 left.
 */
class PromptView(context: Context, private val onMask: (Int) -> Unit) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var codes = IntArray(0)
    private var value = 0
    private val pressed = HashMap<Int, Int>() // pointer -> code

    fun show(packed: Int) {
        if (packed == value) return
        value = packed
        val count = packed and 15
        codes = IntArray(count) { (packed ushr (4 + it * 4)) and 15 }
        if (count == 0) {
            pressed.clear()
            onMask(0)
        }
        invalidate()
    }

    private fun radius() = height * if (codes.size <= 1) 0.2f else 0.13f

    private fun cx(i: Int): Float = width * 0.84f - (codes.size - 1 - i) * radius() * 2.2f

    private fun cy() = height * 0.68f

    override fun onDraw(canvas: Canvas) {
        val r = radius()
        codes.forEachIndexed { i, code ->
            val x = cx(i)
            val y = cy()
            val down = pressed.containsValue(code)
            fill.color = if (down) 0xF0FFFFFF.toInt() else 0xD0181820.toInt()
            canvas.drawCircle(x, y, r, fill)
            line.color = 0xFFFF8A1E.toInt()
            line.strokeWidth = r * 0.08f
            canvas.drawCircle(x, y, r, line)
            glyph(canvas, code, x, y, r * 0.5f, down)
        }
    }

    private fun glyph(c: Canvas, code: Int, x: Float, y: Float, s: Float, down: Boolean) {
        line.strokeWidth = s * 0.26f
        line.color = when (code) {
            1 -> 0xFFFF5A4A.toInt()
            2 -> 0xFF35D6A0.toInt()
            3 -> 0xFFE27AD6.toInt()
            4 -> 0xFF6EA8FF.toInt()
            else -> if (down) Color.BLACK else Color.WHITE
        }
        when (code) {
            1 -> c.drawCircle(x, y, s, line)
            2 -> c.drawPath(poly(x, y - s, x + s, y + s * 0.8f, x - s, y + s * 0.8f), line)
            3 -> c.drawRect(x - s * 0.85f, y - s * 0.85f, x + s * 0.85f, y + s * 0.85f, line)
            4 -> {
                c.drawLine(x - s, y - s, x + s, y + s, line)
                c.drawLine(x - s, y + s, x + s, y - s, line)
            }
            5 -> c.drawPath(poly(x, y - s, x + s, y + s * 0.6f, x - s, y + s * 0.6f), line)
            6 -> c.drawPath(poly(x + s, y, x - s * 0.6f, y + s, x - s * 0.6f, y - s), line)
            7 -> c.drawPath(poly(x, y + s, x + s, y - s * 0.6f, x - s, y - s * 0.6f), line)
            8 -> c.drawPath(poly(x - s, y, x + s * 0.6f, y + s, x + s * 0.6f, y - s), line)
        }
    }

    private fun poly(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) = Path().apply {
        moveTo(x0, y0)
        lineTo(x1, y1)
        lineTo(x2, y2)
        close()
    }

    private fun mask(code: Int) = when (code) {
        1 -> Ps2.CIRCLE
        2 -> Ps2.TRIANGLE
        3 -> Ps2.SQUARE
        4 -> Ps2.CROSS
        5 -> Ps2.UP
        6 -> Ps2.RIGHT
        7 -> Ps2.DOWN
        8 -> Ps2.LEFT
        else -> 0
    }

    private fun send() {
        var m = 0
        pressed.values.forEach { m = m or mask(it) }
        onMask(m)
        invalidate()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (codes.isEmpty()) return false
        val index = ev.actionIndex
        val id = ev.getPointerId(index)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // (anywhere on the right half is the last one: a thumb in a hurry does not aim)
                val x = ev.getX(index)
                val y = ev.getY(index)
                val r = radius() * 1.3f
                var hit = codes.indices.firstOrNull { kotlin.math.hypot(x - cx(it), y - cy()) <= r }
                if (hit == null && x > width * 0.5f) hit = codes.lastIndex
                if (hit == null) return ev.actionMasked != MotionEvent.ACTION_DOWN
                pressed[id] = codes[hit]
                send()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                pressed.remove(id)
                send()
            }
        }
        return true
    }
}
