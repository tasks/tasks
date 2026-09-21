package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.AppStore
import org.tasks.PlatformConfiguration
import org.tasks.TasksUrls
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.works_with_filter_all
import tasks.kmp.generated.resources.works_with_missing
import tasks.kmp.generated.resources.works_with_submit_pull_request

@Composable
fun WorksWithScreen(
    configuration: PlatformConfiguration,
    hasSubscription: Boolean,
    onLinkClick: (String) -> Unit,
    onPricingClick: () -> Unit,
    onMcpSettingsClick: () -> Unit,
    bottomInsets: @Composable () -> Unit = {},
) {
    var selected by remember { mutableStateOf<WorksWithTag?>(null) }
    val current = remember(configuration) {
        if (configuration.isAndroid) WorksWithTag.ANDROID else WorksWithTag.DESKTOP
    }
    val filters = remember(configuration) {
        val shown = worksWithGroups(configuration).flatMap { it.entries }.flatMap { it.tags }
        WorksWithTag.entries.filter { it in shown }
    }
    val groups = remember(configuration, selected) { worksWithGroups(configuration, selected) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SettingsContentPadding),
            horizontalArrangement = Arrangement.spacedBy(SettingsSectionGap),
        ) {
            FilterChip(
                selected = selected == null,
                onClick = { selected = null },
                label = { Text(stringResource(Res.string.works_with_filter_all)) },
            )
            filters.forEach { tag ->
                FilterChip(
                    selected = selected == tag,
                    onClick = { selected = if (selected == tag) null else tag },
                    label = { Text(stringResource(tag.label)) },
                )
            }
        }

        groups.forEach { group ->
            SectionHeader(
                stringResource(group.title),
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                group.entries.forEachIndexed { index, entry ->
                    val labels = WorksWithTag.entries
                        .filter { it in entry.tags && it != current }
                        .map { it.label }
                    val store = configuration.appStore
                    val wrongStore = entry.requiresStore != null &&
                        store != AppStore.NONE && store != entry.requiresStore
                    val summary = when {
                        wrongStore -> entry.requiresStoreDescription
                        configuration.isLibre -> entry.libreDescription
                        else -> null
                    } ?: entry.description
                    SettingsItemCard(
                        position = CardPosition.forIndex(index, group.entries.size),
                    ) {
                        val links = entry.links
                        PreferenceRow(
                            title = stringResource(entry.title),
                            summary = stringResource(summary),
                            icon = entry.icon,
                            iconDrawable = entry.iconDrawable,
                            iconTint = if (entry.tintIcon) null else Color.Unspecified,
                            enabled = links != null,
                            onClick = links?.let {
                                {
                                    when {
                                        entry.showPricingIfUnsubscribed && !hasSubscription ->
                                            onPricingClick()
                                        entry.openMcpSettingsOnDesktop && !configuration.isAndroid ->
                                            onMcpSettingsClick()
                                        else ->
                                            onLinkClick(it.url(configuration.appStore))
                                    }
                                }
                            },
                            trailing = if (labels.isEmpty()) null else {
                                { ChipRow(labels) }
                            },
                        )
                    }
                }
            }
        }

        MissingSomething(
            onLinkClick = onLinkClick,
            modifier = Modifier.padding(
                horizontal = SettingsContentPadding,
                vertical = SettingsContentPadding * 2,
            ),
        )
        bottomInsets()
    }
}

@Composable
private fun MissingSomething(onLinkClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val pullRequest = stringResource(Res.string.works_with_submit_pull_request)
    val email = TasksUrls.SUPPORT_EMAIL
    val sentence = stringResource(Res.string.works_with_missing, pullRequest, email)
    val linkStyles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
    val text = buildAnnotatedString {
        append(sentence)
        listOf(
            pullRequest to TasksUrls.WORKS_WITH_SOURCE,
            email to "mailto:$email?subject=Works%20with%20Tasks.org",
        ).forEach { (label, url) ->
            val start = sentence.indexOf(label)
            if (start < 0) return@forEach
            addLink(
                LinkAnnotation.Url(url, linkStyles) { onLinkClick(url) },
                start,
                start + label.length,
            )
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ChipRow(labels: List<StringResource>) {
    Row(
        modifier = Modifier.padding(end = SettingsContentPadding),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEach { label ->
            Badge {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 22.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Badge(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        content()
    }
}
