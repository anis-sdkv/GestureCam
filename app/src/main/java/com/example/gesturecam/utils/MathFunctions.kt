package com.example.gesturecam.utils

import android.graphics.PointF
import kotlin.math.acos
import kotlin.math.sqrt

object MathFunctions {
    fun calculateAngle(vector1: PointF, vector2: PointF): Double {
        val dotProduct = vector1.x * vector2.x + vector1.y * vector2.y
        val magnitude1 = sqrt(vector1.x * vector1.x + vector1.y * vector1.y)
        val magnitude2 = sqrt(vector2.x * vector2.x + vector2.y * vector2.y)

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angle = acos(cosTheta)

        return Math.toDegrees(angle.toDouble())
    }

    fun calculateCrossProduct(vector1: PointF, vector2: PointF): Float {
        return vector1.x * vector2.y - vector1.y * vector2.x
    }
}