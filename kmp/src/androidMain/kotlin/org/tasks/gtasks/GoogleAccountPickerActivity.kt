package org.tasks.gtasks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GoogleAccountPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = request()
        if (request == null || request.result.isCompleted) {
            finish()
            return
        }
        lifecycleScope.launch {
            request.result.join()
            if (request.result.isCancelled) {
                finishActivity(REQUEST_CODE)
            }
            finish()
        }
        if (savedInstanceState == null) {
            @Suppress("DEPRECATION")
            startActivityForResult(request.intent, REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        request()?.result?.complete(ActivityResult(resultCode, data))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            request()?.result?.cancel()
        }
    }

    private fun request(): GoogleAccountPicker.Request? =
        GoogleAccountPicker.pending?.takeIf { it.id == intent.getLongExtra(EXTRA_REQUEST_ID, -1) }

    companion object {
        private const val EXTRA_REQUEST_ID = "request_id"
        private const val REQUEST_CODE = 1

        fun start(context: Context, requestId: Long) {
            context.startActivity(
                Intent(context, GoogleAccountPickerActivity::class.java)
                    .putExtra(EXTRA_REQUEST_ID, requestId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
