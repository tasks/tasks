package org.tasks.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.compose.pickers.label

@Composable
fun TasksIcon(
    modifier: Modifier = Modifier,
    label: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(modifier = modifier.size(24.dp)) {
        val loadedImageVector by produceState<ImageVector?>(initialValue = null, label) {
            value = withContext(Dispatchers.IO) {
                imageVectorByName(label)
            }
        }
        loadedImageVector?.let { vector ->
            Icon(
                imageVector = vector,
                contentDescription = label,
                tint = tint,
            )
        }
    }
}

fun imageVectorByName(label: String?): ImageVector? = label?.let {
    try {
        val cl = Class.forName("androidx.compose.material.icons.outlined.${it.label}Kt")
        val method = cl.declaredMethods.first()
        method.invoke(null, Icons.Outlined) as ImageVector
    } catch (_: Throwable) {
        null
    }
}
