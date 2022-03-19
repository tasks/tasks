package org.tasks.play

import android.app.Activity
import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability.getInstance
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.todoroo.andlib.utility.DateUtilities.now
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.analytics.Firebase
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class PlayServices @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val firebase: Firebase,
) {
    fun isAvailable() =
        getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    suspend fun requestReview(activity: Activity) {
        if (firebase.reviewCooldown) {
            Timber.d("review cooldown")
            return
        }
        if (firebase.installCooldown) {
            Timber.d("install cooldown")
            return
        }
        try {
            with(ReviewManagerFactory.create(context)) {
                val request = requestReview()
                launchReview(activity, request)
                preferences.lastReviewRequest = now()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}