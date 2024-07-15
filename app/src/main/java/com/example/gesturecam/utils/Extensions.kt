package com.example.gesturecam.utils

import androidx.compose.ui.geometry.Offset
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

fun NormalizedLandmark.toOffset(
    width: Int,
    height: Int,
    scaleFactor: Float
): Offset {
    return Offset(this.x() * width, this.y() * height) * scaleFactor
}


