package org.tasks.gtasks

import android.app.Activity
import android.content.Intent
import com.todoroo.astrid.activity.MainActivity
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import org.tasks.auth.OauthSignIn
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class PlayServices @Inject constructor() {
    val signInIntent: Intent? = null

    val isPlayServicesAvailable: Boolean
        get() = false

    fun refreshAndCheck(): Boolean {
        return false
    }

    fun resolve(activity: Activity?) {}

    val status: String?
        get() = null

    fun check(mainActivity: MainActivity?): Disposable {
        return Disposables.empty()
    }

    fun getSignedInAccount(): OauthSignIn? = null

    fun signInFromIntent(data: Intent?): OauthSignIn? = null
}