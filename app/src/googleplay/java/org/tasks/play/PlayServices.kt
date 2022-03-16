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
import org.tasks.time.DateTimeUtils.printTimestamp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlayServices @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val firebase: Firebase,
) {
    fun isAvailable() =
        getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    suspend fun requestReview(activity: Activity) {
        val now = now()
        val reviewCutoff = preferences.lastReviewRequest + REVIEW_COOLDOWN
        if (reviewCutoff > now) {
            Timber.d("review cooldown: ${printTimestamp(reviewCutoff)}")
            return
        }
        val installCutoff =
            preferences.installDate + TimeUnit.DAYS.toMillis(firebase.reviewCooldown)
        if (installCutoff > now || reviewCutoff > now) {
            Timber.d("install cooldown: ${printTimestamp(installCutoff)}")
            return
        }
        try {
            with(ReviewManagerFactory.create(context)) {
                val request = requestReview()
                launchReview(activity, request)
                preferences.lastReviewRequest = now
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {
        private val REVIEW_COOLDOWN = TimeUnit.DAYS.toMillis(30)
    }
}