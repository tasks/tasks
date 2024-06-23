package org.tasks.compose.components

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import org.tasks.compose.pickers.label

fun imageVectorByName(label: String): ImageVector? =
    try {
        val cl = Class.forName("androidx.compose.material.icons.outlined.${label.label}Kt")
        val method = cl.declaredMethods.first()
        method.invoke(null, Icons.Outlined) as ImageVector
    } catch (_: Throwable) {
        null
    }
