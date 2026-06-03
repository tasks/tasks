package org.tasks.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.Chevron
import org.tasks.compose.home.SystemBarScrim
import org.tasks.compose.settings.CardPosition
import org.tasks.compose.settings.SettingsCardGap
import org.tasks.compose.settings.SettingsContentPadding
import org.tasks.compose.settings.SettingsItemCard
import org.tasks.compose.settings.SettingsRowPadding
import org.tasks.previews.PREVIEW_NIGHT_MODE
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cloud_onboarding_faq_desktop_sync_answer
import tasks.kmp.generated.resources.cloud_onboarding_faq_desktop_sync_question
import tasks.kmp.generated.resources.cloud_onboarding_faq_google_access_answer
import tasks.kmp.generated.resources.cloud_onboarding_faq_google_access_question
import tasks.kmp.generated.resources.cloud_onboarding_faq_title
import tasks.kmp.generated.resources.cloud_onboarding_not_now
import tasks.kmp.generated.resources.cloud_onboarding_welcome_body
import tasks.kmp.generated.resources.cloud_onboarding_welcome_title
import tasks.kmp.generated.resources.ic_google
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.sign_in_with_google
import tasks.kmp.generated.resources.tasks_org

@Composable
internal fun WelcomeStep(
    showConfetti: Boolean,
    onSignIn: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val faqItems = listOf(
            stringResource(Res.string.cloud_onboarding_faq_google_access_question) to
                    stringResource(Res.string.cloud_onboarding_faq_google_access_answer),
            stringResource(Res.string.cloud_onboarding_faq_desktop_sync_question) to
                    stringResource(Res.string.cloud_onboarding_faq_desktop_sync_answer),
        )
        var expandedIndex by rememberSaveable { mutableStateOf(-1) }
        val onToggleIndex: (Int) -> Unit = { index ->
            expandedIndex = if (expandedIndex == index) -1 else index
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxHeight < 500.dp
            if (isCompact) {
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    WelcomeHeader(
                        modifier = Modifier.weight(1f),
                    )
                    WelcomeBody(
                        faqItems = faqItems,
                        expandedIndex = expandedIndex,
                        onToggleIndex = onToggleIndex,
                        onSignIn = onSignIn,
                        onClose = onClose,
                        insetSides = WindowInsetsSides.Top +
                                WindowInsetsSides.Bottom +
                                WindowInsetsSides.Horizontal,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    WelcomeHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    WelcomeBody(
                        faqItems = faqItems,
                        expandedIndex = expandedIndex,
                        onToggleIndex = onToggleIndex,
                        onSignIn = onSignIn,
                        onClose = onClose,
                        insetSides = WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        SystemBarScrim(
            modifier = Modifier
                .windowInsetsTopHeight(WindowInsets.systemBars)
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.surface,
        )
        SystemBarScrim(
            modifier = Modifier
                .windowInsetsBottomHeight(WindowInsets.systemBars)
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
        )
        val playConfetti = rememberSaveable { showConfetti }
        var animationComplete by rememberSaveable { mutableStateOf(false) }
        if (playConfetti && !animationComplete) {
            ConfettiKit(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        angle = 315,
                        spread = 70,
                        position = Position.Relative(0.0, 0.4),
                        emitter = Emitter(duration = 200.milliseconds).max(150),
                    ),
                    Party(
                        angle = 225,
                        spread = 70,
                        position = Position.Relative(1.0, 0.4),
                        emitter = Emitter(duration = 200.milliseconds).max(150),
                    ),
                ),
            )
            LaunchedEffect(Unit) {
                delay(3.seconds)
                animationComplete = true
            }
        }
    }
}

@Composable
private fun WelcomeHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeContent.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_round_icon),
            contentDescription = stringResource(Res.string.tasks_org),
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.cloud_onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomeBody(
    faqItems: List<Pair<String, String>>,
    expandedIndex: Int,
    onToggleIndex: (Int) -> Unit,
    onSignIn: () -> Unit,
    onClose: () -> Unit,
    insetSides: WindowInsetsSides,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeContent.only(insetSides)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.cloud_onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(Res.string.sign_in_with_google))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.cloud_onboarding_not_now))
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.cloud_onboarding_faq_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            faqItems.forEachIndexed { index, (question, answer) ->
                FaqItem(
                    question = question,
                    answer = answer,
                    expanded = expandedIndex == index,
                    onToggle = { onToggleIndex(index) },
                    position = CardPosition.forIndex(index, faqItems.size),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    position: CardPosition,
) {
    SettingsItemCard(position = position) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = SettingsContentPadding, vertical = SettingsRowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Chevron(collapsed = !expanded)
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = SettingsContentPadding,
                        end = SettingsContentPadding,
                        top = SettingsRowPadding,
                        bottom = SettingsRowPadding,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Welcome - Light")
@Preview(showBackground = true, uiMode = PREVIEW_NIGHT_MODE, name = "Welcome - Dark")
@Preview(showBackground = true, name = "Welcome - Compact", widthDp = 720, heightDp = 360)
@Composable
private fun WelcomeStepPreview() {
    TasksTheme {
        WelcomeStep(
            showConfetti = false,
            onSignIn = {},
            onClose = {},
        )
    }
}
