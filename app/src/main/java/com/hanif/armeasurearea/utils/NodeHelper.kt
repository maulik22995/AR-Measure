package com.hanif.armeasurearea.utils

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.hanif.armeasurearea.utils.Constant.thresholdDistance
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.arcore.ARSession
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toFloat3
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan


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

fun updateVerticalMeasuring(
    cameraPose: Pose,
    startNode: AnchorNode,
    pointerNode: AnchorNode,
    session: ARSession
) {
    val cameraPos = cameraPose.position
    val nodePos = startNode.worldPosition.toVector3()

    val cameraToNodeHeightDifference = cameraPos.y - nodePos.y
    val cameraToNodeDistanceOnPlane =
        sqrt((cameraPos.x - nodePos.x).pow(2) + (cameraPos.z - nodePos.z).pow(2))
    val cameraAngle = cameraPose.pitch

    val normalizedCameraAngle = normalizeRadians(cameraAngle.toDouble(), -PI)
    val pi2 = PI / 2
    if (normalizedCameraAngle < -pi2 * 2 / 3 || normalizedCameraAngle > +pi2 * 1 / 2) {
        Log.e(">>", "Too steep of an angle, step away a bit")
        return
    }

    // don't allow negative heights (into the ground)
    val height =
        max(0f, cameraToNodeHeightDifference + cameraToNodeDistanceOnPlane * tan(cameraAngle))

    val pos = Vector3.add(nodePos, Vector3(0f, height, 0f))
    val newPos =
        Pose(pos.toFloat3().toFloatArray(), floatArrayOf(0f, 0f, 0f, 0f))
    val newAnchor = session.createAnchor(newPos)
    pointerNode.apply {
        anchor = newAnchor
    }
}

fun createLineNode(
    engine: Engine,
    anchor1: AnchorNode,
    anchor2: AnchorNode,
    materialLoader: MaterialLoader,
): Node {
    val point1 = anchor1.worldPosition
    val point2 = anchor2.worldPosition
    return drawLineOnPoints(engine, point1, point2, materialLoader)

}

fun updateLineNode(startNode: Node, endNode: Node, lineNode: CubeNode) {
    val point1 = startNode.worldPosition
    val point2 = endNode.worldPosition

    val point1Vector3 = point1.toVector3()
    val point2Vector3 = point2.toVector3()

    val difference = Vector3.subtract(point1Vector3, point2Vector3);


    lineNode.apply {
//        position = calculateMidPosition(point1Vector3, point2Vector3)
//        quaternion = calculateQuaternion(point1, point2)
//        scale = Scale(0.01f, 0.0001f, 0.01f)
        transform = Transform(
            position = calculateMidPosition(point1Vector3, point2Vector3),
            quaternion = calculateQuaternion(point1, point2),
            scale = Scale(0.005f, 0.001f, difference.length())
        )
    }
}

fun calculateQuaternion(
    point1: Position,
    point2: Position
): Quaternion {
    val point1Vector3 = point1.toVector3()
    val point2Vector3 = point2.toVector3()

    val difference = Vector3.subtract(point1Vector3, point2Vector3);
    val directionFromTopToBottom = difference.normalized();

    val rotationFromAToB = io.github.sceneview.collision.Quaternion.lookRotation(
        directionFromTopToBottom,
        Vector3.up()
    )
    return Quaternion(
        rotationFromAToB.x,
        rotationFromAToB.y,
        rotationFromAToB.z,
        rotationFromAToB.w
    )
}

fun calculateMidPosition(point1Vector3: Vector3, point2Vector3: Vector3): Position {
    return Vector3.add(point1Vector3, point2Vector3).scaled(.5f).toPosition()
}

fun drawLineOnPoints(
    engine: Engine,
    point1: Position,
    point2: Position,
    materialLoader: MaterialLoader
): Node {
    val point1Vector3 = point1.toVector3()
    val point2Vector3 = point2.toVector3()

    val difference = Vector3.subtract(point1Vector3, point2Vector3);

    val cube = CubeNode(
        engine,
        materialInstance = materialLoader.createColorInstance(Color.Yellow)
    ).apply {
        position = calculateMidPosition(point1Vector3, point2Vector3)
        quaternion = calculateQuaternion(point1, point2)
        scale = Scale(0.01f, 0.0001f, difference.length())
    }
    return cube
}

fun calculateDistance(x: Float, y: Float, z: Float): Float {
    return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}

fun changeUnit(distanceMeter: Float, unit: String): Float {
    return when (unit) {
        "cm" -> distanceMeter * 100
        "mm" -> distanceMeter * 1000
        else -> distanceMeter
    }
}

fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    modelInstances: MutableList<ModelInstance>,
    anchor: Anchor,
    kModelFile: String = "models/anim.glb"
): AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor)
    val sphereNode = CylinderNode(
        engine = engine,
        radius = 0.010f, // Sphere radius
        height = 0.005f, // Sphere height
        materialInstance = materialLoader.createColorInstance(Color.Blue)
    )

//    val modelNode = ModelNode(
//        modelInstance = modelInstances.apply {
//            if (isEmpty()) {
//                this += modelLoader.createInstancedModel(kModelFile, kMaxModelInstances)
//            }
//        }.removeLast(),
//        // Scale to fit in a 0.5 meters cube
//        scaleToUnits = 0.1f
//    ).apply {
//        // Model Node needs to be editable for independent rotation from the anchor rotation
//        isEditable = true
//    }
//    val boundingBoxNode = CubeNode(
//        engine,
//        size = modelNode.extents,
//        center = modelNode.center,
//        materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
//    ).apply {
//        isVisible = false
//    }
//    modelNode.addChildNode(boundingBoxNode)
//    anchorNode.addChildNode(modelNode)
//
//    listOf(modelNode, anchorNode).forEach {
//        it.onEditingChanged = { editingTransforms ->
//            boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
//        }
//    }
    anchorNode.addChildNode(sphereNode)
    return anchorNode
}

fun calculateAndReturnNearestNode(childNodes: List<Node>, anchorHit: Anchor): Node? {
    return childNodes
        .filter { it.name == Constant.MARKER }
        .find { existingNode ->
            val existingAnchorPose =
                (existingNode as AnchorNode).anchor.pose
            val newAnchorPose = anchorHit.pose
            if (existingAnchorPose != null && newAnchorPose != null) {
                // Calculate the distance between the two anchors
                val dx = existingAnchorPose.tx() - newAnchorPose.tx()
                val dy = existingAnchorPose.ty() - newAnchorPose.ty()
                val dz = existingAnchorPose.tz() - newAnchorPose.tz()
                val distance =
                    Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
                distance <= thresholdDistance // Check if the distance is within the threshold
            } else {
                false
            }
        }
}