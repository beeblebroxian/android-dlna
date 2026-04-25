package com.cajor.dk.dlna

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.ControlPointFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DlnaService : Service() {
    private var controlPoint: ControlPoint? = null
    private val binder = LocalBinder()
    private var executor: ExecutorService? = null

    inner class LocalBinder : Binder() {
        fun getService(): DlnaService = this@DlnaService
    }

    fun getControlPoint(): ControlPoint? = controlPoint

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")

        executor = Executors.newSingleThreadExecutor()

        // Create ControlPoint with Android-friendly settings using builder
        controlPoint = ControlPointFactory.builder()
            .setCallbackHandler { runnable -> Handler(Looper.getMainLooper()).post(runnable) }
            .build()

        // Initialize and start the control point
        controlPoint?.initialize()
        controlPoint?.start()

        Log.d(TAG, "ControlPoint initialized and started")
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind called")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")

        controlPoint?.apply {
            stop()
            terminate()
        }
        controlPoint = null

        executor?.shutdown()

        Log.d(TAG, "ControlPoint stopped and terminated")
    }

    companion object {
        private const val TAG = "DlnaService"
    }
}
