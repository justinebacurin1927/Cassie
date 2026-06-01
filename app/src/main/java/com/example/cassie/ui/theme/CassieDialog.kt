package com.example.cassie.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Modern glassmorphism dialog for Cassie.
 */
@Composable
fun CassieDialog(
    onDismissRequest: () -> Unit,
    dialogTitle: @Composable (() -> Unit)? = null,
    dialogText: @Composable (() -> Unit)? = null,
    dialogConfirmButton: @Composable (() -> Unit)? = null,
    dialogDismissButton: @Composable (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "dialogScale"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        containerColor = CassieColors.CardGrey.copy(alpha = 0.95f),
        titleContentColor = CassieColors.TextPrimary,
        textContentColor = CassieColors.TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = dialogTitle,
        text = dialogText,
        confirmButton = {
            if (dialogConfirmButton != null) {
                dialogConfirmButton()
            } else {
                TextButton(onClick = onDismissRequest) {
                    Text("Close", color = CassieColors.PurpleAccent)
                }
            }
        },
        dismissButton = {
            if (dialogDismissButton != null) {
                dialogDismissButton()
            }
        },
    )
}
