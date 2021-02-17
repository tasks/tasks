package org.tasks.gtasks

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability.getInstance
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

object PlayServices {
    fun isAvailable(context: Context) =
        getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}