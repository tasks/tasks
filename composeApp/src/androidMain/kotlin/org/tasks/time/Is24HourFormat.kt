package org.tasks.time

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun is24HourFormat(): Boolean =
    DateFormat.is24HourFormat(LocalContext.current)
