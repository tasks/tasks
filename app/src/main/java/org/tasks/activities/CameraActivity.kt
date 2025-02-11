package org.tasks.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.utility.Constants
import kotlinx.coroutines.launch
import org.tasks.files.FileHelper.newFile
import org.tasks.time.DateTime
import java.io.File
import java.io.IOException

class CameraActivity : AppCompatActivity() {
    private var uri: Uri? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            uri = savedInstanceState.getParcelable(EXTRA_URI)
        } else {
            lifecycleScope.launch {
                try {
                    uri =
                        newFile(
                            this@CameraActivity,
                            Uri.fromFile(cacheDir),
                            "image/jpeg",
                            DateTime().toString("yyyyMMddHHmm"),
                            ".jpeg"
                        )
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
                if (uri!!.scheme != ContentResolver.SCHEME_FILE) {
                    throw RuntimeException("Invalid Uri")
                }
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val shared =
                    FileProvider.getUriForFile(
                        this@CameraActivity, Constants.FILE_PROVIDER_AUTHORITY, File(
                            uri!!.path!!
                        )
                    )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, shared)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivityForResult(intent, REQUEST_CODE_CAMERA)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent()
                intent.setData(uri)
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(EXTRA_URI, uri)
    }

    companion object {
        private const val REQUEST_CODE_CAMERA = 75
        private const val EXTRA_URI = "extra_output"
    }
}
