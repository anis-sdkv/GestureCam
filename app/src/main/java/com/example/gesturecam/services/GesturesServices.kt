package com.example.gesturecam.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent


class GesturesServices : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        sharedInstance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        sharedInstance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun dispatchClickGesture(x: Float, y: Float): Boolean = dispatchGesture(
        buildClick(x, y), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i("GesturesServices", "Gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.i("GesturesServices", "Gesture cancelled")
            }
        },
        null
    )

    private fun buildClick(x: Float, y: Float): GestureDescription {
        val clickPath = Path()
        clickPath.moveTo(x, y)
        val clickStroke = GestureDescription.StrokeDescription(clickPath, 0, 20)
        val clickBuilder = GestureDescription.Builder()
        clickBuilder.addStroke(clickStroke)
        return clickBuilder.build()
    }

    companion object {
        var sharedInstance: GesturesServices? = null
    }
}
