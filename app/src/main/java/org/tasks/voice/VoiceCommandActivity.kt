package org.tasks.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.extensions.Context.toast
import javax.inject.Inject

@AndroidEntryPoint
class VoiceCommandActivity : AppCompatActivity() {
    @Inject lateinit var taskCreator: TaskCreator
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var firebase: Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == AUTO_SEND) {
            lifecycleScope.launch {
                intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?.takeUnless { it.isBlank() }
                        ?.let {
                            taskCreator.basicQuickAddTask(it)
                            toast(R.string.voice_command_added_task)
                            firebase.addTask("note_to_self")
                        }
                finish()
            }
        } else {
            finish()
        }
    }

    companion object {
        private const val AUTO_SEND = "com.google.android.gm.action.AUTO_SEND"
    }
}