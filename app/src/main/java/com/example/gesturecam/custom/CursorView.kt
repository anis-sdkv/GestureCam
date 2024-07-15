package com.example.gesturecam.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class CursorView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    constructor(context: Context?) : this(context, null)

    private var cursorPosition: PointF = PointF(0f, 0f)
    private var clicked = false
    private var radius: Float = 50f

    private val moveAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
        .apply { duration = 40 }
    private val clickAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
        .apply {
            duration = 500
            interpolator = DecelerateInterpolator(2f)
        }
    private val paint: Paint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 4f
        style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(
            cursorPosition.x - radius / 2,
            cursorPosition.y - radius / 2,
            radius,
            paint
        )
    }


    fun setTarget(targetPoint: PointF) {
        startAnimation(targetPoint)
    }

    fun startClickAnim(onAnimEnd: (x: Float, y: Float) -> Unit) {
        if (clicked) return
        if (clickAnimator.isRunning) clickAnimator.cancel()
        clicked = true

        clickAnimator.removeAllUpdateListeners()
        clickAnimator.addUpdateListener { animation ->
            Log.i("frac", animation.animatedFraction.toString())
            if (animation.animatedFraction == 1f) onAnimEnd.invoke(
                cursorPosition.x,
                cursorPosition.y
            )
            radius = 50f - 30f * animation.animatedFraction
            invalidate()
        }
        clickAnimator.start()
    }

    fun interruptClickAnim() {
        clicked = false
        if (clickAnimator.isRunning) clickAnimator.cancel()
        radius = 50f
    }

    private fun startAnimation(targetPoint: PointF) {
        if (moveAnimator.isRunning) {
            moveAnimator.cancel()
        }

        val diffX = targetPoint.x - cursorPosition.x
        val diffY = targetPoint.y - cursorPosition.y
        val initPoint = PointF(cursorPosition.x, cursorPosition.y)
        moveAnimator.removeAllUpdateListeners()
        moveAnimator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            cursorPosition = PointF(initPoint.x + diffX * fraction, initPoint.y + diffY * fraction)
            invalidate()
        }
        moveAnimator.start()
    }
}