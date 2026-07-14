package com.danicosta.mkbot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "mkbot_capture_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TARGET_FRAME_INTERVAL_MS = 66L

        @Volatile
        var latestFrame: Bitmap? = null
            private set

        var isCapturing: Boolean = false
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var lastFrameTime = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handlerThread = HandlerThread("MKBotCapture").also { it.start() }
        backgroundHandler = Handler(handlerThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val resultCode = MainActivity.projectionResultCode
        val resultData = MainActivity.projectionResultData
        if (resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, resultData)
        mediaProjection?.registerCallback(projectionCallback, backgroundHandler)
        startCapture()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isCapturing = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        handlerThread?.quitSafely()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        super.onDestroy()
    }

    private fun startCapture() {
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        var width = metrics.widthPixels
        var height = metrics.heightPixels
        val density = metrics.densityDpi

        // Em Service, getRealMetrics às vezes devolve o tamanho "natural" (retrato) mesmo com a
        // tela girada pra paisagem. Corrige comparando com a rotação atual de verdade.
        val isLandscapeRotation = display.rotation == Surface.ROTATION_90 || display.rotation == Surface.ROTATION_270
        if (isLandscapeRotation && height > width) {
            val temp = width
            width = height
            height = temp
        }

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener({ imgReader ->
            val image = imgReader.acquireLatestImage()
            if (image == null) return@setOnImageAvailableListener

            val now = System.currentTimeMillis()
            if (now - lastFrameTime < TARGET_FRAME_INTERVAL_MS) {
                image.close()
                return@setOnImageAvailableListener
            }
            lastFrameTime = now

            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                latestFrame = if (rowPadding == 0) {
                    bitmap
                } else {
                    Bitmap.createBitmap(bitmap, 0, 0, width, height)
                }
            } catch (e: Exception) {
                // Perder um quadro não é grave — o próximo já chega.
            } finally {
                image.close()
            }
        }, backgroundHandler)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "MKBotCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            backgroundHandler
        )
        isCapturing = true
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MK Bot - Captura de tela",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MK Bot")
            .setContentText("Lendo a tela em tempo real")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
