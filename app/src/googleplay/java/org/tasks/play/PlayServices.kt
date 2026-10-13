package org.tasks.play

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability.getInstance
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode.PLAY_STORE_NOT_FOUND
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import javax.inject.Inject

class PlayServices @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val firebase: Firebase,
) {
    fun isAvailable() =
        getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    fun requestReview(activity: ComponentActivity) = activity.lifecycleScope.launch {
        if (firebase.reviewCooldown) {
            return@launch
        }
        try {
            with(ReviewManagerFactory.create(context)) {
                val request = requestReview()
                launchReview(activity, request)
            }
            preferences.lastReviewRequest = currentTimeMillis()
            firebase.logEvent(R.string.event_request_review)
        } catch (e: ReviewException) {
            when (e.errorCode) {
                PLAY_STORE_NOT_FOUND -> Timber.e(e)
                else -> firebase.reportException(e)
            }
        } catch (e: Exception) {
            firebase.reportException(e)
        }
    }
}