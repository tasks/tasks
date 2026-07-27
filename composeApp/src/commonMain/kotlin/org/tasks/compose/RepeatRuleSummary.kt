package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import org.tasks.repeats.RepeatRuleToString

sealed interface RepeatRuleSummary {
    data object Loading : RepeatRuleSummary

    data object DoesNotRepeat : RepeatRuleSummary

    data class Repeats(val text: String) : RepeatRuleSummary
}

@Composable
fun rememberRepeatRuleSummary(recurrence: String?): RepeatRuleSummary {
    val repeatRuleToString = koinInject<RepeatRuleToString>()
    var summary by remember(recurrence, repeatRuleToString) {
        mutableStateOf(
            if (recurrence.isNullOrBlank()) {
                RepeatRuleSummary.DoesNotRepeat
            } else {
                RepeatRuleSummary.Loading
            }
        )
    }
    LaunchedEffect(recurrence, repeatRuleToString) {
        summary = repeatRuleToString.toString(recurrence)
            ?.let { RepeatRuleSummary.Repeats(it) }
            ?: RepeatRuleSummary.DoesNotRepeat
    }
    return summary
}
