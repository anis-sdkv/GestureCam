package com.example.gesturecam.core;

import android.content.Context
import android.util.Log
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class HandLandmarkManager(private val context: Context) : HandLandmarkerHelper.LandmarkerListener {
    private lateinit var landmarkerHelper: HandLandmarkerHelper
    private var state: LandmakerState = LandmakerState(currentMinHandDetectionConfidence = 0.8f)
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val _resultFlow: MutableStateFlow<HandLandmarkerHelper.ResultBundle?> =
        MutableStateFlow(null)
    val resultFlow = _resultFlow.asStateFlow()

    private val _errorsFlow: MutableStateFlow<Pair<String, Int>?> = MutableStateFlow(null)
    val errorsFlow = _errorsFlow.asStateFlow()

    suspend fun initManager(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null
    ) {
        if (cameraProvider == null)
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        initLandMarker()
        bindCameraUseCases(lifecycleOwner, previewView)
    }

    fun unbind() {
        landmarkerHelper.clearHandLandmarker()
        cameraProvider?.unbindAll()
    }

    private suspend fun initLandMarker() = withContext(Dispatchers.Default) {
        if (!::landmarkerHelper.isInitialized)
            landmarkerHelper = HandLandmarkerHelper(
                context = context,
                runningMode = RunningMode.LIVE_STREAM,
                state = state,
                handLandmarkerHelperListener = this@HandLandmarkManager
            )
        else if (landmarkerHelper.isClose())
            landmarkerHelper.setupHandLandmarker()
    }

    private suspend fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null
    ) {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView?.display?.rotation ?: Surface.ROTATION_0)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { image -> detectHand(image) }
            }

        try {
            withContext(Dispatchers.Main) {
                if (previewView != null) {
                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(previewView.display.rotation)
                        .build()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } else
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalyzer)
            }

        } catch (exc: Exception) {
            Log.e("LandMarkerManager", "Use case binding failed", exc)
        }
    }

    private fun detectHand(imageProxy: ImageProxy) {
        landmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = true
        )
    }

    override fun onError(error: String, errorCode: Int) {
        _errorsFlow.tryEmit(Pair(error, errorCode))
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        _resultFlow.tryEmit(resultBundle)
    }
}