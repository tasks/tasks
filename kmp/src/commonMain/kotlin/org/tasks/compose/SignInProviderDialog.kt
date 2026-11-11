package org.tasks.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.help
import tasks.kmp.generated.resources.ic_apple
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_octocat
import tasks.kmp.generated.resources.sign_in_to_tasks
import tasks.kmp.generated.resources.sign_in_with_apple
import tasks.kmp.generated.resources.sign_in_with_github
import tasks.kmp.generated.resources.sign_in_with_google

enum class SignInProvider {
    GOOGLE,
    GITHUB,
    APPLE,
}

val DEFAULT_SIGN_IN_PROVIDERS = listOf(SignInProvider.GOOGLE, SignInProvider.APPLE, SignInProvider.GITHUB)
val APP_STORE_SIGN_IN_PROVIDERS = listOf(SignInProvider.APPLE, SignInProvider.GOOGLE, SignInProvider.GITHUB)

@Composable
fun SignInProviderDialog(
    onSelected: (SignInProvider) -> Unit,
    onHelp: () -> Unit,
    onCancel: () -> Unit,
    providers: List<SignInProvider> = DEFAULT_SIGN_IN_PROVIDERS,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(Res.string.sign_in_to_tasks),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        providers.forEach { provider ->
            when (provider) {
                SignInProvider.GOOGLE -> ProviderRow(
                    title = Res.string.sign_in_with_google,
                    icon = Res.drawable.ic_google,
                    onClick = { onSelected(SignInProvider.GOOGLE) },
                )
                SignInProvider.APPLE -> ProviderRow(
                    title = Res.string.sign_in_with_apple,
                    icon = Res.drawable.ic_apple,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = { onSelected(SignInProvider.APPLE) },
                )
                SignInProvider.GITHUB -> ProviderRow(
                    title = Res.string.sign_in_with_github,
                    icon = Res.drawable.ic_octocat,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    onClick = { onSelected(SignInProvider.GITHUB) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextButton(onClick = onHelp) {
                Text(
                    text = stringResource(Res.string.help),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(Res.string.cancel),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
internal fun SignInProviderButton(
    provider: SignInProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, icon, tint) = when (provider) {
        SignInProvider.GOOGLE -> Triple(Res.string.sign_in_with_google, Res.drawable.ic_google, null)
        SignInProvider.APPLE -> Triple(
            Res.string.sign_in_with_apple,
            Res.drawable.ic_apple,
            MaterialTheme.colorScheme.onSurface,
        )
        SignInProvider.GITHUB -> Triple(
            Res.string.sign_in_with_github,
            Res.drawable.ic_octocat,
            MaterialTheme.colorScheme.onSurface,
        )
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint ?: Color.Unspecified,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = stringResource(title),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ProviderRow(
    title: StringResource,
    icon: DrawableResource,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(title),
            tint = tint ?: Color.Unspecified,
            modifier = Modifier.padding(end = 16.dp).size(48.dp),
        )
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
