package com.shahar.tomjerry.utilities // Or your chosen package

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.shahar.tomjerry.interfaces.TiltCallback


class TiltDetector(context: Context, private var tiltCallback: TiltCallback?) {

    private val sensorManager: SensorManager = context
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? = sensorManager
        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private lateinit var sensorEventListener: SensorEventListener

    var tiltCounterXLeft: Int = 0
        private set
    var tiltCounterXRight: Int = 0
        private set
    var tiltCounterY: Int = 0
        private set

    private var lastProcessedTimestamp: Long = 0L
    // Time interval (milliseconds) between processing tilt events to avoid flooding.
    private val TILT_EVENT_INTERVAL_MS: Long = 300L
    // Thresholds for detecting significant tilt.
    // For X-axis:
    // Positive X value when device (top) is tilted to user's left.
    // Negative X value when device (top) is tilted to user's right.
    private val TILT_LEFT_THRESHOLD: Float = 2f  // e.g., If X > 2.0, consider it a left tilt
    private val TILT_RIGHT_THRESHOLD: Float = -2f // e.g., If X < -2.0, consider it a right tilt
    // For Y-axis (front/back tilt)
    private val Y_TILT_THRESHOLD: Float = 3.0f

    init {
        if (accelerometer == null) {
            Log.e("TiltDetector", "Accelerometer sensor not found on this device. Tilt controls will not function.")
        }
        initSensorEventListener()
    }

    private fun initSensorEventListener() {
        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { // Ensure event is not null
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val xAxisValue = it.values[0]
                        val yAxisValue = it.values[1]
                        // val zAxisValue = it.values[2] // Z-axis data, if needed
                        processTilt(xAxisValue, yAxisValue)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // This callback is invoked when sensor accuracy changes.
                // You might want to log this or notify the user if accuracy is low.
                // Log.i("TiltDetector", "Sensor accuracy changed: ${sensor?.name} to $accuracy")
            }
        }
    }

    /**
     * Processes the raw sensor data to detect directional tilt and invokes callbacks.
     * Rate-limited by TILT_EVENT_INTERVAL_MS.
     */
    private fun processTilt(x: Float, y: Float) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTimestamp >= TILT_EVENT_INTERVAL_MS) {
            lastProcessedTimestamp = currentTime

            var tiltDetectedX = false
            // Check for Left Tilt (Positive X beyond threshold)
            if (x > TILT_LEFT_THRESHOLD) {
                tiltCounterXLeft++
                tiltCallback?.onTiltedLeft()
                tiltDetectedX = true
            }
            // Check for Right Tilt (Negative X beyond threshold)
            else if (x < TILT_RIGHT_THRESHOLD) {
                tiltCounterXRight++
                tiltCallback?.onTiltedRight()
                tiltDetectedX = true
            }

            // Check for Y-axis tilt (for future features like bonus speed boost)
            // Using Math.abs() as the direction of Y tilt might be less critical for this specific callback.
            if (kotlin.math.abs(y) >= Y_TILT_THRESHOLD) {
                tiltCounterY++
                // Pass the actual y value for the callback to interpret if needed
                tiltCallback?.onTiltY(y)
            }
        }
    }

    /**
     * Starts listening for accelerometer sensor updates.
     * Call this when tilt controls should be active (e.g., in Activity's onResume or when game starts).
     */
    fun start() {
        accelerometer?.let { // Only register if the sensor is available
            sensorManager.registerListener(
                sensorEventListener,
                it, // Use the non-null accelerometer sensor
                SensorManager.SENSOR_DELAY_GAME // Use SENSOR_DELAY_GAME for better responsiveness in games
            )
            Log.i("TiltDetector", "Tilt detection started.")
        } ?: run {
            Log.w("TiltDetector", "Attempted to start tilt detection, but accelerometer is not available.")
        }
    }

    /**
     * Stops listening for accelerometer sensor updates.
     * Call this when tilt controls are no longer needed (e.g., in Activity's onPause or when game stops)
     * to conserve battery.
     */
    fun stop() {
        accelerometer?.let { // Only unregister if the sensor is available (and thus was likely registered)
            sensorManager.unregisterListener(sensorEventListener, it)
            Log.i("TiltDetector", "Tilt detection stopped.")
        }
    }

}