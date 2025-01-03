package com.hanif.armeasurearea.utils

import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.Position
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

val Pose.pitch: Float get() {
    val (x, y, z, w) = rotationQuaternion
    return if (0.5 - abs(x * y + z * w) < 0.001) 0f
    else atan2(2 * (x * w - y * z), -x * x + y * y - z * z + w * w)
}

fun Position.toVector3(): Vector3 = Vector3(x, y, z)
fun Vector3.toPosition(): Position = Float3(x, y, z)


/** returns a number between [startAt] - [startAt]+2PI */
fun normalizeRadians(value: Double, startAt: Double = 0.0): Double {
    val pi2 = PI * 2
    var result = value % pi2 // is now -2PI..2PI
    result = (result + pi2) % pi2 // is now 0..2PI
    if (result > startAt + pi2) result -= pi2
    return result
}