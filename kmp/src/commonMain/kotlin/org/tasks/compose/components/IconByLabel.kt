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
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.compose.pickers.label
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

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

private val iconCache = ConcurrentHashMap<String, Optional<ImageVector>>()

/**
 * Resolving an icon means a [Class.forName] plus reflective invocation, and a miss additionally
 * costs a thrown [ClassNotFoundException]. Chips call this during composition on the main thread,
 * so results (including misses) are memoized for the lifetime of the process. The icon set is
 * static, so entries never need invalidating.
 */
fun imageVectorByName(label: String?): ImageVector? {
    if (label == null) {
        return null
    }
    return iconCache
        .getOrPut(label) {
            val iconName = label.label
            Optional.ofNullable(
                loadIcon("androidx.compose.material.icons.outlined.${iconName}Kt", Icons.Outlined)
                    ?: loadIcon(
                        "androidx.compose.material.icons.automirrored.outlined.${iconName}Kt",
                        Icons.AutoMirrored.Outlined
                    )
            )
        }
        .orElse(null)
}

private fun loadIcon(className: String, receiver: Any): ImageVector? =
    try {
        val cl = Class.forName(className)
        val method = cl.declaredMethods.first()
        method.invoke(null, receiver) as ImageVector
    } catch (_: Throwable) {
        null
    }
