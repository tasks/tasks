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
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.printTimestamp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlayServices @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
) {
    fun isAvailable() =
        getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    suspend fun requestReview(activity: Activity) {
        val now = now()
        val installCutoff = preferences.installDate + INSTALL_COOLDOWN
        val reviewCutoff = preferences.lastReviewRequest + REVIEW_COOLDOWN
        if (installCutoff > now || reviewCutoff > now) {
            Timber.d("wait for review request: install=${printTimestamp(installCutoff)} review=${printTimestamp(reviewCutoff)}")
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
        private val INSTALL_COOLDOWN = TimeUnit.DAYS.toMillis(14)
        private val REVIEW_COOLDOWN = TimeUnit.DAYS.toMillis(30)
    }
}