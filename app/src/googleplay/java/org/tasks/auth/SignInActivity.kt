package org.tasks.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import at.bitfire.dav4jvm.exception.HttpException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.billing.PurchaseDialog.Companion.FRAG_TAG_PURCHASE_DIALOG
import org.tasks.billing.PurchaseDialog.Companion.newPurchaseDialog
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.CaldavAccount
import org.tasks.gtasks.PlayServices
import org.tasks.injection.InjectingAppCompatActivity
import org.tasks.ui.Toaster
import javax.inject.Inject

@AndroidEntryPoint
class SignInActivity : InjectingAppCompatActivity() {

    @Inject lateinit var toaster: Toaster
    @Inject lateinit var provider: CaldavClientProvider
    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var firebase: Firebase

    val viewModel: SignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.observe(this, this::onSignIn, this::onError)

        lifecycleScope.launch {
            playServices
                    .getSignedInAccount()
                    ?.let { validate(it) }
                    ?: startActivityForResult(playServices.signInIntent, RC_SIGN_IN)
        }
    }

    private fun onSignIn(account: CaldavAccount?) {
        account?.let { toaster.longToast(getString(R.string.logged_in, it.name)) }
        finish()
    }

    private fun onError(t: Throwable) {
        if (t is HttpException && t.code == 402) {
            newPurchaseDialog(true).show(supportFragmentManager, FRAG_TAG_PURCHASE_DIALOG)
        } else {
            firebase.reportException(t)
            toaster.longToast(t.message)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            playServices
                    .signInFromIntent(data)
                    ?.let { validate(it) }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun validate(account: OauthSignIn) = lifecycleScope.launch(Dispatchers.IO) {
        viewModel.validate(account.id!!, account.email!!, account.idToken!!)
    }

    companion object {
        private const val RC_SIGN_IN = 10000
    }
}