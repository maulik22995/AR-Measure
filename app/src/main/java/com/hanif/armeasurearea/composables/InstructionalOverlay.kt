package com.hanif.armeasurearea.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.TrackingFailureReason
import com.hanif.armeasurearea.R
import io.github.sceneview.ar.getDescription
import io.github.sceneview.node.Node

@Composable
fun InstructionalOverlay(
    trackingFailureReason: TrackingFailureReason?,
    childNodes: List<Node>,
    calculatedDistance: String?
) {
    val context = LocalContext.current

    // Get the text for the instructional card
    val cardText = trackingFailureReason?.getDescription(context)
        ?: when {
            childNodes.isEmpty() -> stringResource(R.string.point_your_phone_down)
            childNodes.size == 1 -> stringResource(R.string.add_points)
            else -> ""
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (cardText.isNotEmpty()) {
            InstructionalCard(
                text = cardText,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        calculatedDistance?.let {
            Spacer(modifier = Modifier.height(16.dp))
            InstructionalCard(
                text = it,
                modifier = Modifier
                    .padding(horizontal = 32.dp, /*bottom = 60.dp*/)
            )
        }
    }
}

@Composable
private fun InstructionalCard(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
//        elevation = elevation
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            color = Color.Black
        )
    }
}
