package com.hanif.armeasurearea.utils

import io.github.sceneview.collision.Vector3
import io.github.sceneview.node.Node
import java.math.RoundingMode
import java.text.DecimalFormat

fun calculateTotalDistance(anchorNodes: List<Node>): Float {
    if (anchorNodes.size < 2) {
        // Not enough nodes to calculate a distance
        return 0f
    }

    var totalDistance = 0f

    for (i in 0 until anchorNodes.size - 1) {
        val currentNode = anchorNodes[i]
        val nextNode = anchorNodes[i + 1]

        val currentPosition = currentNode.worldPosition.toVector3()
        val nextPosition = nextNode.worldPosition.toVector3()

        // Calculate the distance between the two nodes
        val distance = Vector3.subtract(currentPosition, nextPosition).length()
        totalDistance += distance
    }

    return totalDistance
}

fun changeUnit(distanceMeter: Double, unit: String): Double {
    return when (unit) {
        "cm" -> distanceMeter * 100
        "mm" -> distanceMeter * 1000
        "cm²" -> distanceMeter * 10000
        else -> distanceMeter
    }
}

fun calculateConvexHullArea(anchorNodes: List<Node>): Float {
    if (anchorNodes.size < 3) {
        // Not enough points to form a convex hull
        return 0f
    }

    // Project points onto a 2D plane (e.g., XZ plane)
    val points2D = anchorNodes.map { node ->
        Pair(node.worldPosition.x, node.worldPosition.z)
    }

    // Step 1: Compute the convex hull using Graham's scan or another algorithm
    val convexHull = computeConvexHull(points2D)

    // Step 2: Calculate the area of the convex hull using the Shoelace formula
    var area = 0f
    for (i in convexHull.indices) {
        val (x1, y1) = convexHull[i]
        val (x2, y2) = convexHull[(i + 1) % convexHull.size] // Wrap around to the first point
        area += (x1 * y2) - (y1 * x2)
    }

    return kotlin.math.abs(area) / 2
}

fun computeConvexHull(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
    // Sort points by x-coordinate, breaking ties by y-coordinate
    val sortedPoints = points.sortedWith(compareBy({ it.first }, { it.second }))

    val lowerHull = mutableListOf<Pair<Float, Float>>()
    for (point in sortedPoints) {
        while (lowerHull.size >= 2 && !isCounterClockwise(
                lowerHull[lowerHull.size - 2],
                lowerHull[lowerHull.size - 1],
                point
            )
        ) {
            lowerHull.removeAt(lowerHull.size - 1)
        }
        lowerHull.add(point)
    }

    val upperHull = mutableListOf<Pair<Float, Float>>()
    for (point in sortedPoints.asReversed()) {
        while (upperHull.size >= 2 && !isCounterClockwise(
                upperHull[upperHull.size - 2],
                upperHull[upperHull.size - 1],
                point
            )
        ) {
            upperHull.removeAt(upperHull.size - 1)
        }
        upperHull.add(point)
    }

    // Remove the last point of each half because it’s repeated at the beginning of the other half
    upperHull.removeAt(upperHull.size - 1)
    lowerHull.removeAt(lowerHull.size - 1)

    return lowerHull + upperHull
}

fun isCounterClockwise(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Boolean {
    return (b.first - a.first) * (c.second - a.second) - (b.second - a.second) * (c.first - a.first) > 0
}

fun Double.roundOffDecimal(): Double {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.CEILING
    return df.format(this).toDouble()
}