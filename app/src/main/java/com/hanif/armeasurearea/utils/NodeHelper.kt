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

fun updateVerticalCubeMeasuring(
    cameraPose: Pose,
    startNode: AnchorNode,
    pointerNode: AnchorNode,
    session: ARSession
) {
    val cameraPos = cameraPose.position
    val nodePos = startNode.worldPosition.toVector3()


    Log.e("updateVerticalCubeMeasuring >>",cameraPos.toString())

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
        max(0f, cameraToNodeHeightDifference +  tan(cameraAngle))

    val pos = Vector3.add(nodePos, Vector3(0f, height , 0f))
    val newPos =
        Pose(pos.toFloat3().toFloatArray(), pointerNode.quaternion.toFloatArray())
    val newAnchor = session.createAnchor(newPos)
    pointerNode.apply {
        anchor = newAnchor
    }
}

fun updateLineAnchor(
    startNode: Node,
    endNode: Node,
    lineAnchorNode: AnchorNode,
    arSession: ARSession
) {
    val point1 = startNode.worldPosition
    val point2 = endNode.worldPosition

    val point1Vector3 = point1.toVector3()
    val point2Vector3 = point2.toVector3()

    val difference = Vector3.subtract(point1Vector3, point2Vector3)
    val newPosition = calculateMidPosition(point1Vector3, point2Vector3)
    val newQuaternion = calculateQuaternion(point1, point2)
    val pose = Pose(newPosition.toFloatArray(), newQuaternion.toFloatArray())
    val newAnchor = arSession.createAnchor(pose)
    lineAnchorNode.apply {
        anchor = newAnchor
        (lineAnchorNode.childNodes.firstOrNull())?.apply {
            transform = Transform(
                scale = Scale(0.005f, 0.001f, difference.length())
            )
        }
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

fun createNewLineNode(
    engine: Engine,
    anchor: Anchor,
    linNode: AnchorNode,
    materialLoader: MaterialLoader
): AnchorNode {
    val newNode = AnchorNode(engine, anchor)
    newNode.apply {
        name = Constant.LINE
        val newLineNode = CubeNode(
            engine,
            materialInstance = materialLoader.createColorInstance(
                Color.Cyan
            )
        ).apply {
            transform = linNode.childNodes.first().transform
            name = Constant.LINE
        }
        addChildNode(newLineNode)
    }
    return newNode
}

fun createMarkerPointNode(
    engine: Engine,
    materialLoader: MaterialLoader,
    anchor: Anchor
): AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor)
    val sphereNode = CylinderNode(
        engine = engine,
        radius = 0.010f, // Sphere radius
        height = 0.005f, // Sphere height
        materialInstance = materialLoader.createColorInstance(Color.Blue)
    )
    anchorNode.addChildNode(sphereNode)
    anchorNode.name = Constant.MARKER
    return anchorNode
}

fun createReplicatedListForHeight(
    engine: Engine, materialLoader: MaterialLoader,
    nodes: List<Node>,
    session: ARSession
): List<AnchorNode> {
    val updatedList = ArrayList<AnchorNode>()
    nodes.forEachIndexed { index, anchorNode ->

        if (anchorNode.name == Constant.MARKER) {
            val newAnchor = createNewAnchor(
                session,
                Position(
                    anchorNode.worldPosition.x,
                    anchorNode.worldPosition.y + 0.05f,
                    anchorNode.worldPosition.z
                ), anchorNode.quaternion
            )
            val newPointNode =
                createMarkerPointNode(engine, materialLoader, newAnchor)
            updatedList.add(newPointNode)

            // create vertical line
            val newVerticalLineNode =
                createNewLineNode(engine, newAnchor, anchorNode as AnchorNode, materialLoader)
            updateLineAnchor(anchorNode, newPointNode, newVerticalLineNode, session)
            updatedList.add(newVerticalLineNode)

        } else if (anchorNode.name == Constant.LINE) {
            val newAnchor = createNewAnchor(
                session,
                Position(
                    anchorNode.worldPosition.x,
                    anchorNode.worldPosition.y + 0.05f,
                    anchorNode.worldPosition.z
                ), anchorNode.quaternion
            )
            val newLineNode =
                createNewLineNode(engine, newAnchor, anchorNode as AnchorNode, materialLoader)
            updatedList.add(newLineNode)
        }

    }
    return updatedList
}

fun createNewAnchor(
    arSession: ARSession,
    newPosition: Position,
    newQuaternion: Quaternion
): Anchor {
    val pose = Pose(newPosition.toFloatArray(), newQuaternion.toFloatArray())
    val newAnchor = arSession.createAnchor(pose)
    return newAnchor
}
