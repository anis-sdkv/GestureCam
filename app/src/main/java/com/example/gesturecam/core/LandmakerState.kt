package com.example.gesturecam.core

data class LandmakerState(
    var currentDelegate: Int = HandLandmarkerHelper.DELEGATE_CPU,
    var currentMinHandDetectionConfidence: Float = HandLandmarkerHelper.DEFAULT_HAND_DETECTION_CONFIDENCE,
    var currentMinHandTrackingConfidence: Float = HandLandmarkerHelper.DEFAULT_HAND_TRACKING_CONFIDENCE,
    var currentMinHandPresenceConfidence: Float = HandLandmarkerHelper.DEFAULT_HAND_PRESENCE_CONFIDENCE,
    var currentMaxHands: Int = HandLandmarkerHelper.DEFAULT_NUM_HANDS
    )