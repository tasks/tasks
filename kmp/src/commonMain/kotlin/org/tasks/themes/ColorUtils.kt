package org.tasks.themes

import androidx.compose.ui.graphics.Color

fun contentColorFor(backgroundColor: Int): Color =
    Color(contentColor(backgroundColor))
