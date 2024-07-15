package com.example.gesturecam.services

import android.app.Notification
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.gesturecam.R
import com.example.gesturecam.core.HandLandmarkerHelper
import com.example.gesturecam.custom.CursorView

import com.example.gesturecam.utils.MathFunctions
import com.example.gesturecam.core.HandLandmarkManager
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max


class OverlayService : LifecycleService() {

    var state = State.Stopped
        private set

    private lateinit var serviceNotification: Notification
    private lateinit var landmarkManager: HandLandmarkManager

    private var windowSize = IntSize(0, 0)
    private var windowManager: WindowManager? = null
    private var rootView: ViewGroup? = null
    private var cursorView: CursorView? = null

    private var gesturesServices: GesturesServices? = null
    private var lastTapTime = System.currentTimeMillis()
    private var statusBarOffset: Int = 0

    override fun onCreate() {
        super.onCreate()

        serviceNotification = NotificationCompat.Builder(this, "OverLayChannel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("The service is running...")
            .setContentText("Go back to the app to stop.")
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_ACTION -> start()
            STOP_ACTION -> stop()
            else -> throw Exception()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        if (state != State.Stopped) return
        state = State.Starting
        gesturesServices = GesturesServices.sharedInstance
        startForeground(1, serviceNotification)
        showView()
        lifecycleScope.launch(Dispatchers.Default) {
            if (!::landmarkManager.isInitialized)
                landmarkManager = HandLandmarkManager(this@OverlayService)
            landmarkManager.initManager(this@OverlayService, this@OverlayService, null)
            state = State.Started
            landmarkManager.resultFlow.collect { if (it != null) onResults(it) }
        }
    }


    private fun stop() {
        if (state != State.Started) return
        state = State.Stopping
        clear()
        stopSelf()
        state = State.Stopped
    }

    private fun clear() {
        lifecycleScope.cancel()
        landmarkManager.unbind()
        windowManager?.removeViewImmediate(rootView)
        rootView?.removeAllViews()
        windowManager = null
        rootView = null
        cursorView = null
    }

    private fun showView() {
        cursorView = CursorView(this).apply { FrameLayout.LayoutParams.MATCH_PARENT }
        rootView = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            fitsSystemWindows = true
            addView(cursorView)
        }

        windowManager = (getSystemService(WINDOW_SERVICE) as WindowManager).apply {
            val displayMetrics = DisplayMetrics()
            defaultDisplay.getMetrics(displayMetrics)
            windowSize = IntSize(displayMetrics.widthPixels, displayMetrics.heightPixels)

            val viewParams = WindowManager.LayoutParams(
                windowSize.width,
                windowSize.height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            addView(rootView, viewParams)
        }.also {
            val outPoint = Point()
            it.defaultDisplay.getRealSize(outPoint)
            statusBarOffset = outPoint.y - windowSize.height
        }
    }

    private fun scalePoint(landmark: NormalizedLandmark, scaleFactor: Float): Offset =
        Offset(landmark.x() - 0.5f, landmark.y() - 0.5f) * scaleFactor + Offset(0.5f, 0.5f)

    private suspend fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        if (resultBundle.results.isEmpty() || resultBundle.results.first().landmarks()
                .isEmpty()
        ) return

        val landmark = resultBundle.results.first().landmarks().first()

        val imageWidth = resultBundle.inputImageWidth
        val imageHeight = resultBundle.inputImageHeight
        val scaleFactor = max(
            windowSize.width * 1f / imageWidth,
            windowSize.height * 1f / imageHeight
        )

        val p1 = scalePoint(landmark[4], 1.6f)
        val p2 = scalePoint(landmark[8], 1.6f)
        val midPoint = Offset(
            (p1.x + p2.x) / 2 * imageWidth * scaleFactor,
            (p1.y + p2.y) / 2 * imageHeight * scaleFactor,
        )

        val vector1 =
            PointF(landmark[4].x() - landmark[3].x(), landmark[4].y() - landmark[3].y())
        val vector2 =
            PointF(landmark[8].x() - landmark[7].x(), landmark[8].y() - landmark[7].y())
        val angle = MathFunctions.calculateAngle(vector1, vector2)
        val crossProduct = MathFunctions.calculateCrossProduct(vector1, vector2)

        withContext(Dispatchers.Main) {
            cursorView?.setTarget(PointF(midPoint.x, midPoint.y))
            if ((angle > 35) and (crossProduct < 0)) {
                if (System.currentTimeMillis() - lastTapTime > 1000) {
                    cursorView?.startClickAnim { x, y -> dispatch(x, y + statusBarOffset) }
                    lastTapTime = System.currentTimeMillis()
                } else {
                }
            } else cursorView?.interruptClickAnim()
        }
    }

    private fun dispatch(x: Float, y: Float): Boolean {
        if (x < 0 || y < 0 || x > windowSize.width || y > windowSize.height) return false
        return gesturesServices?.dispatchClickGesture(x, y) ?: false
    }

    companion object {
        const val START_ACTION = "OverlayService.action.start"
        const val STOP_ACTION = "OverlayService.action.stop"
    }

    enum class State {
        Started, Starting, Stopped, Stopping
    }
}