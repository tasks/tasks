package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.wrong_account_message

@Composable
fun conflictingAccountMessage(existingAccount: String): String = stringResource(
    Res.string.wrong_account_message,
    existingAccount,
    TasksUrls.SUPPORT_EMAIL,
)
