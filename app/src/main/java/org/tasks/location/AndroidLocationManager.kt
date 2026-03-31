package org.tasks.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Hilt-injectable wrapper — implementation lives in kmp/androidMain */
class HiltAndroidLocationManager @Inject constructor(
    @ApplicationContext context: Context,
) : AndroidLocationManager(context)
