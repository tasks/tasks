package org.tasks.repeats

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.entity.CaldavAccount
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_ACCOUNT_TYPE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_DATE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_RRULE
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CustomRecurrenceHiltViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    locale: Locale,
) : CustomRecurrenceViewModel(
    rrule = savedStateHandle.get<String>(EXTRA_RRULE),
    dueDate = savedStateHandle.get<Long>(EXTRA_DATE) ?: 0L,
    accountType = savedStateHandle.get<Int>(EXTRA_ACCOUNT_TYPE) ?: CaldavAccount.TYPE_CALDAV,
    locale = locale,
)
