package com.hanif.armeasurearea

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.math.toFloat3
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlin.math.pow
import kotlin.math.sqrt


@Composable
fun DistanceScreen() {
    var arSceneView by remember { mutableStateOf<ARSceneView?>(null) }
    var calculatedDistance by remember { mutableStateOf<String?>(null) }

    Box(contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            val context = LocalContext.current
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val materialLoader = rememberMaterialLoader(engine)
            val cameraNode = rememberARCameraNode(engine)
            val childNodes = rememberNodes()
            val view = rememberView(engine)
            val collisionSystem = rememberCollisionSystem(view)
            var totalDistance by remember { mutableStateOf(0.0) }
            val planeRenderer by remember { mutableStateOf(true) }
            val modelInstances = remember { mutableListOf<ModelInstance>() }
            var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
            var frame by remember { mutableStateOf<Frame?>(null) }
            ARScene(
                modifier = Modifier.fillMaxSize(),
                childNodes = childNodes,
                engine = engine,
                view = view,
                modelLoader = modelLoader,
                collisionSystem = collisionSystem,
                onViewCreated = { arSceneView = this },
                onViewUpdated = { arSceneView = this },
                sessionConfiguration = { session, config ->
                    config.depthMode =
                        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                            Config.DepthMode.AUTOMATIC
                        } else {
                            Config.DepthMode.DISABLED
                        }
                    config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                },
                cameraNode = cameraNode,
                planeRenderer = planeRenderer,
                onTrackingFailureChanged = { trackingFailureReason = it },
                onSessionUpdated = { _, updatedFrame ->
                    frame = updatedFrame
                },
                onGestureListener = rememberOnGestureListener(onSingleTapConfirmed = { motionEvent, node ->
                    if (node == null) {
                        val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                        val hitResult =
                            hitResults?.firstOrNull { it.isValid(depthPoint = true, point = true) }

                        hitResult?.createAnchorOrNull()?.let { anchor ->
                            // Add the new marker
                            val newNode = createAnchorNode(
                                engine = engine,
                                modelLoader = modelLoader,
                                materialLoader = materialLoader,
                                modelInstances = modelInstances,
                                anchor = anchor,
                                kModelFile = "models/pin.glb"
                            )

                            childNodes += newNode

                            // Draw a line between the last two markers
                            if (childNodes.size > 1) {
                                val previousMarker =
                                    childNodes[childNodes.size - if (childNodes.size == 2) 2 else 3]
                                val currentMarker = childNodes.last()

                                // Create a line between the two markers
                                childNodes += createLineNode(
                                    engine = engine,
                                    anchor1 = previousMarker as AnchorNode,
                                    anchor2 = currentMarker as AnchorNode,
                                    materialLoader = materialLoader,
                                )
                            }
                        }
                    } else {
                        Toast.makeText(context, "Node tapped", Toast.LENGTH_LONG).show()
                    }
                })
            )

            // Update the distance dynamically
            LaunchedEffect(childNodes.size) {
                val allAnchorNodes = childNodes.filterIsInstance<AnchorNode>()
                var totalDistanceLocal = 0.0f
                if (allAnchorNodes.size > 1) {
                    allAnchorNodes.forEachIndexed { index, node ->
                        if(index != allAnchorNodes.size -1){
                            val position1 = node.worldPosition
                            val position2 = childNodes[index + 1].worldPosition
                            val distance = calculateDistance(
                                position1.x - position2.x,
                                position1.y - position2.y,
                                position1.z - position2.z
                            )
                            totalDistanceLocal += distance
                        }

                    }
                    calculatedDistance = "Total Distance: ${
                        changeUnit(
                            totalDistanceLocal.toFloat(),
                            "cm"
                        )
                    } cm"
                } else {
                    calculatedDistance = null
                }
            }

            Text(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                color = Color.White,
                text = trackingFailureReason?.getDescription(LocalContext.current)
                    ?: if (childNodes.isEmpty()) {
                        stringResource(R.string.point_your_phone_down)
                    } else {
                        stringResource(R.string.tap_anywhere_to_add_model)
                    }
            )


            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                calculatedDistance?.let {
                    Text(
                        modifier = Modifier
                            .padding(16.dp),
                        text = it,
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    if (childNodes.size > 0) {
                        Button(onClick = {
                            if (childNodes.last() is CubeNode) {
                                childNodes.removeLastOrNull()
                            }
                            childNodes.removeLastOrNull()
                        }, modifier = Modifier.padding(bottom = 10.dp)) {
                            Text(text = "Undo")
                        }
                    }
                }
            }


        }
    }
}

fun Position.toVector3(): Vector3 = Vector3(x, y, z)
fun Vector3.toPosition(): Position = Float3(x, y, z)


fun createLineNode(
    engine: Engine,
    anchor1: AnchorNode,
    anchor2: AnchorNode,
    materialLoader: MaterialLoader,
): Node {
    val point1 = anchor1.worldPosition
    val point2 = anchor2.worldPosition

    val point1Vector3 = point1.toVector3()
    val point2Vector3 = point2.toVector3()

    val difference = Vector3.subtract(point1Vector3, point2Vector3);
    val directionFromTopToBottom = difference.normalized();

    // Calculate the direction and length of the line
    val direction = Vector3(point2.x - point1.x, point2.y - point1.y, point2.z - point1.z)
    val length = direction.length()

    val rotationFromAToB = io.github.sceneview.collision.Quaternion.lookRotation(
        directionFromTopToBottom,
        Vector3.up()
    )

    val cube = CubeNode(
        engine,
        size = Size(0.01f, 0.01f, difference.length()), // Thin cube (small X/Z, stretched Y)
        center = Vector3.zero().toFloat3(),
        materialInstance = materialLoader.createColorInstance(Color.Yellow)
    ).apply {
        position = Vector3.add(point1Vector3, point2Vector3).scaled(.5f).toPosition()
        quaternion = Quaternion(
            rotationFromAToB.x,
            rotationFromAToB.y,
            rotationFromAToB.z,
            rotationFromAToB.w
        )
    }

    val cylinder = CylinderNode(
        engine,
        radius = 0.0025f,
        height = length,
        center = Vector3.zero().toFloat3(),
        materialInstance = materialLoader.createColorInstance(Color.Yellow)
    ).apply {
        worldPosition = Vector3.add(point1Vector3, point2Vector3).scaled(.5f).toPosition()
//        worldRotation = Rotation(quaternionCal.x,quaternionCal.y,quaternionCal.z)
//        worldQuaternion = quaternionCal // Adjusted rotation
    }

//    cube.parent = anchor1


    return cube
}

private fun calculateDistance(x: Float, y: Float, z: Float): Float {
    return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}

private fun changeUnit(distanceMeter: Float, unit: String): Float {
    return when (unit) {
        "cm" -> distanceMeter * 100
        "mm" -> distanceMeter * 1000
        else -> distanceMeter
    }
}

//private const val kModelFile = "models/anim.glb"
const val kMaxModelInstances = 10
fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    modelInstances: MutableList<ModelInstance>,
    anchor: Anchor,
    kModelFile: String = "models/anim.glb"
): AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor)
    val modelNode = ModelNode(
        modelInstance = modelInstances.apply {
            if (isEmpty()) {
                this += modelLoader.createInstancedModel(kModelFile, kMaxModelInstances)
            }
        }.removeLast(),
        // Scale to fit in a 0.5 meters cube
        scaleToUnits = 0.1f
    ).apply {
        // Model Node needs to be editable for independent rotation from the anchor rotation
        isEditable = true
    }
    val boundingBoxNode = CubeNode(
        engine,
        size = modelNode.extents,
        center = modelNode.center,
        materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
    ).apply {
        isVisible = false
    }
    modelNode.addChildNode(boundingBoxNode)
    anchorNode.addChildNode(modelNode)

    listOf(modelNode, anchorNode).forEach {
        it.onEditingChanged = { editingTransforms ->
            boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
        }
    }
    return anchorNode
}