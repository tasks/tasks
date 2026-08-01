package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.net.toUri
import com.todoroo.astrid.voice.VoiceOutputAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.tasks.PlatformConfiguration
import org.tasks.R
import org.tasks.injection.ApplicationScope
import org.tasks.extensions.Context.getResourceUri
import org.tasks.preferences.Preferences
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.viewmodel.NotificationsViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationsHiltViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val voiceOutputAssistant: VoiceOutputAssistant,
    platformConfiguration: PlatformConfiguration,
    @ApplicationScope persistenceScope: CoroutineScope,
) : NotificationsViewModel(
    appPreferences = preferences,
    platformConfiguration = platformConfiguration,
    persistenceScope = persistenceScope,
) {

    private val ttsChecks = Channel<Unit>(Channel.CONFLATED)
    val ttsCheckRequests: Flow<Unit> = ttsChecks.receiveAsFlow()

    init {
        refreshAndroidState()
    }

    override fun refreshState() {
        super.refreshState()
        refreshAndroidState()
    }

    private fun refreshAndroidState() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        showBatteryOptimization =
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        refreshCompletionSoundName()
    }

    override fun rescheduleNotifications(cancelExisting: Boolean) {
        NotificationSchedulerIntentService.enqueueWork(context, cancelExisting)
    }

    override fun updateVoice(enabled: Boolean) {
        val needsTtsCheck: Boolean
        try {
            if (enabled) {
                needsTtsCheck = !voiceOutputAssistant.isTTSInitialized
            } else {
                needsTtsCheck = false
                if (voiceOutputAssistant.isTTSInitialized) {
                    voiceOutputAssistant.shutdown()
                }
            }
        } catch (e: VerifyError) {
            Timber.e(e)
            super.updateVoice(false)
            return
        }
        super.updateVoice(enabled)
        if (needsTtsCheck) {
            ttsChecks.trySend(Unit)
        }
    }

    fun handleTtsCheckResult(resultCode: Int) {
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            voiceOutputAssistant.initTTS()
        }
    }

    fun getCompletionRingtoneValue(): String? =
        preferences.getStringValue(R.string.p_completion_ringtone)

    fun handleCompletionSoundResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val ringtone: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            preferences.setString(R.string.p_completion_ringtone, ringtone?.toString() ?: "")
            refreshCompletionSoundName()
        }
    }

    private fun refreshCompletionSoundName() {
        val value = preferences.getStringValue(R.string.p_completion_ringtone)
        val defaultUri = context.getResourceUri(R.raw.long_rising_tone)
        completionSoundName = when {
            value == "" -> context.getString(R.string.none)
            value != null -> {
                val uri = value.toUri()
                if (uri == defaultUri) {
                    context.getString(R.string.settings_default)
                } else {
                    try {
                        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: value
                    } catch (e: SecurityException) {
                        Timber.e(e)
                        value
                    }
                }
            }
            else -> context.getString(R.string.settings_default)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceOutputAssistant.shutdown()
    }
}
