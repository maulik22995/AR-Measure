package com.hanif.armeasurearea.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.filament.Engine
import com.hanif.armeasurearea.R
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode

@Composable
fun ActionButton(
    icon: Painter,
    contentDescription: String,
    rotationAngle: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = icon,
        contentDescription = contentDescription,
        contentScale = ContentScale.Inside,
        modifier = modifier
            .size(60.dp)
//            .align(alignment)
            .background(shape = CircleShape, color = Color.White.copy(alpha = 0.5f))
            .graphicsLayer {
                rotationZ = rotationAngle
            }
            .clickable { onClick() }
    )
}