package com.hanif.armeasurearea

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.hanif.armeasurearea.composables.ActionButton
import com.hanif.armeasurearea.composables.InstructionalOverlay
import com.hanif.armeasurearea.ui.theme.LocalDimens
import com.hanif.armeasurearea.utils.Constant
import com.hanif.armeasurearea.utils.calculateTotalDistance
import com.hanif.armeasurearea.utils.changeUnit
import com.hanif.armeasurearea.utils.createAnchorNode
import com.hanif.armeasurearea.utils.createLineNode
import com.hanif.armeasurearea.utils.updateLineNode
import com.hanif.armeasurearea.utils.updateVerticalMeasuring
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberView


@Composable
fun DistanceScreen(innerPadding: PaddingValues, mainActivity: MainActivity) {
    var arSceneView by remember { mutableStateOf<ARSceneView?>(null) }
    var calculatedDistance by remember { mutableStateOf<String?>(null) }
    var centerSphereNode by remember { mutableStateOf<AnchorNode?>(null) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val planeRenderer by remember { mutableStateOf(true) }
    var measureVertical by remember { mutableStateOf(false) }
    var showCoachingOverlay by remember { mutableStateOf(true) }
    val modelInstances = remember { mutableListOf<ModelInstance>() }
    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var frame by remember { mutableStateOf<Frame?>(null) }
    // Allow resume on rotation
    val rotationAngle by animateFloatAsState(
        targetValue = if (!measureVertical) 0f else 90f, // Rotate 90 degrees for vertical
        animationSpec = tween(durationMillis = 300),
        label = "rotation" // Animation duration
    )

    Log.e("childNodes >>", childNodes.size.toString())

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.padding(innerPadding)) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {

            ARScene(
                activity = mainActivity,
                modifier = Modifier.fillMaxSize(),
                childNodes = childNodes,
                engine = engine,
                view = view,
                modelLoader = modelLoader,
                collisionSystem = collisionSystem,
                onViewCreated = { arSceneView = this },
                onViewUpdated = { arSceneView = this },
                onSessionCreated = { session: Session ->
                    Log.e("onSessionCreated >>", session.toString())
                },
                onSessionPaused = { session: Session ->
                    Log.e("onSessionPaused >>", session.toString())
                },
                onSessionResumed = { session: Session ->
                    Log.e("onSessionResumed >>", session.toString())
                },
                onSessionFailed = { e: Exception ->
                    Log.e("onSessionFailed >>", e.message.toString())
                },
                sessionConfiguration = { session, config ->
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    // disabling unused features should make processing faster
                    config.depthMode = Config.DepthMode.DISABLED
                    config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
                    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                    config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                },
                cameraNode = cameraNode,
                planeRenderer = planeRenderer,
                onTrackingFailureChanged = {
                    showCoachingOverlay = it != null
                    trackingFailureReason = it
                },
                onSessionUpdated = { _, updatedFrame ->
                    frame = updatedFrame
                    // Only update the pointer when tracking state is TRACKING
                    if (updatedFrame.camera.trackingState == TrackingState.PAUSED) showCoachingOverlay =
                        true
                    if (updatedFrame.camera.trackingState == TrackingState.TRACKING) {

                        if (measureVertical) {
                            updateVerticalMeasuring(
                                updatedFrame.camera.pose,
                                childNodes.last(),
                                centerSphereNode!!
                            )
                        } else {
                            // Extract the camera's position
                            val hitResults = updatedFrame.hitTest(
                                arSceneView!!.width / 2.0f,
                                arSceneView!!.height / 2.0f
                            ).filter {
                                (it.trackable as? Plane)?.isPoseInPolygon(it.hitPose) == true
                            }

                            val hitResult =
                                hitResults.firstOrNull {
                                    it.isValid(
                                        depthPoint = true,
                                        point = true
                                    )
                                }
                            hitResult?.createAnchorOrNull()?.let { anchorHit ->

                                if (centerSphereNode == null) {
                                    showCoachingOverlay = false
                                    // Create the sphere and add it as a child
                                    val sphereNode = CylinderNode(
                                        engine = engine,
                                        radius = 0.010f, // Sphere radius
                                        height = 0.0001f, // Sphere height
                                        materialInstance = materialLoader.createColorInstance(Color.Red)
                                    )
                                    centerSphereNode = AnchorNode(engine, anchorHit)
                                    centerSphereNode!!.name = Constant.POINTER

                                    (centerSphereNode as AnchorNode).addChildNode(sphereNode)

                                    childNodes += centerSphereNode!!
                                } else {
                                    showCoachingOverlay = false
                                    centerSphereNode?.apply {
                                        anchor = anchorHit
                                    }
                                }

                                /**
                                 * Start line from last Marker or update last line
                                 */
                                val allNodeExceptPointers =
                                    childNodes.filter { it.name != Constant.POINTER }
                                val lastNode = allNodeExceptPointers.lastOrNull()
                                if (lastNode is CubeNode) {
                                    // line is already present so update it as camera move
                                    val lastMarker =
                                        allNodeExceptPointers.findLast { it.name == Constant.MARKER }
                                    if (lastMarker != null && centerSphereNode != null) {
                                        updateLineNode(
                                            lastMarker,
                                            centerSphereNode!!,
                                            lineNode = lastNode
                                        )
                                    }
                                } else {
                                    // create new line
                                    if (lastNode != null && centerSphereNode != null) {
                                        childNodes += createLineNode(
                                            engine = engine,
                                            anchor1 = lastNode as AnchorNode,
                                            anchor2 = centerSphereNode as AnchorNode,
                                            materialLoader = materialLoader,
                                        ).apply {
                                            name = Constant.LINE
                                        }
                                    }
                                }
                            }
                        }
                        centerSphereNode?.isVisible = true
                    } else {
                        // Hide or deactivate the pointer if tracking is lost
                        centerSphereNode?.isVisible = false
                    }
                },
            )

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = LocalDimens.ARView.OverlayAlpha)),
                visible = showCoachingOverlay,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ARCoachingOverlay()
            }

            // Update the distance dynamically
            LaunchedEffect(childNodes.size) {
                val allAnchorNodes = childNodes.filter { it.name == Constant.MARKER }
                val totalDistance = calculateTotalDistance(allAnchorNodes)
                calculatedDistance = "Total Distance: ${
                    changeUnit(
                        totalDistance.toFloat(),
                        "cm"
                    )
                } cm"
            }

            ActionButton(
                icon = painterResource(R.drawable.ic_horizontal_24),
                contentDescription = "measurement",
                rotationAngle = rotationAngle,
                onClick = {
                    measureVertical = !measureVertical
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )

            InstructionalOverlay(
                trackingFailureReason = trackingFailureReason,
                childNodes = childNodes,
                calculatedDistance = calculatedDistance
            )

            if (childNodes.isNotEmpty()) {
                ActionButton(
                    icon = painterResource(R.drawable.vd_undo),
                    contentDescription = "Undo",
                    rotationAngle = rotationAngle,
                    onClick = {
                        val lastNode = childNodes.lastOrNull()
                        if (lastNode is AnchorNode && lastNode.name == Constant.POINTER) {
                            // Do nothing
                        } else {
                            if (lastNode is CubeNode) {
                                childNodes.removeLastOrNull()
                            }
                            childNodes.removeLastOrNull()
                        }
                    },
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomEnd)
                )
            }

            if (centerSphereNode?.isVisible == true) {
                ActionButton(
                    icon = painterResource(R.drawable.vd_add), // Replace with your actual icon
                    contentDescription = "Add Anchor",
                    rotationAngle = rotationAngle,
                    onClick = {
                        // Add marker at center position
                        centerSphereNode?.let { sphere ->
                            val newNode = createAnchorNode(
                                engine = engine,
                                modelLoader = modelLoader,
                                materialLoader = materialLoader,
                                modelInstances = modelInstances,
                                anchor = sphere.anchor,
                                kModelFile = "models/pin.glb"
                            )
                            newNode.name = Constant.MARKER
                            childNodes += newNode
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}
