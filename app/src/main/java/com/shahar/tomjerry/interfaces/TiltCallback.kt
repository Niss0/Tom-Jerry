package com.shahar.tomjerry.interfaces // Or your chosen package

/**
 * Callback interface for receiving directional tilt events.
 */
interface TiltCallback {
    /**
     * Called when the device is significantly tilted to the (user's) left.
     */
    fun onTiltedLeft()

    /**
     * Called when the device is significantly tilted to the (user's) right.
     */
    fun onTiltedRight()

    /**
     * Called when a significant tilt along the Y-axis is detected.
     * (Retained from your original interface for potential future use, e.g., bonus speed boost)
     * @param yValue The raw Y-axis sensor value that triggered the tilt.
     */
    fun onTiltY(yValue: Float) // Passing yValue as discussed for consistency
}