package com.sukisu.ultra.ui.screen.settings.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.material.IconTile
import com.sukisu.ultra.ui.component.material.SegmentedColumn
import com.sukisu.ultra.ui.component.material.SegmentedListItem
import com.sukisu.ultra.ui.component.miuix.AppCard
import com.sukisu.ultra.ui.screen.settings.SettingsUiState
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * The results UI for Nova settings search, ported from FinaSU's
 * `ui/screen/settings/search/SettingsSearchResults.kt` (Milestone 3.1).
 *
 * Adaptations for Nova (the FinaSU hub was NOT ported — see MERGE_PLAN.md §3.9):
 * groups are Nova's settings sections and sub-screens instead of FinaSU hub
 * categories; the floating navigation-bar availability gates are gone, because
 * Nova has no floating bar; and a Miuix variant is added, since Nova's settings
 * page has one. The Material variant keeps FinaSU's segmented rows and its
 * SemiBold match emphasis; the Miuix variant uses plain text rows (no
 * AnnotatedString usage with Miuix Text exists in the codebase).
 */

/** Emphasis is a weight, never a different colour. */
private val MatchEmphasis = SpanStyle(fontWeight = FontWeight.SemiBold)

/**
 * The query's results, grouped by section.
 *
 * Availability is part of the match: a row the device cannot show is never offered, which is what
 * keeps the search honest about what the pages actually contain. The filtering is deliberately not
 * remembered — the predicates read settings that live outside [uiState], and the matching itself is
 * far cheaper than getting that wrong.
 */
fun settingsSearchGroups(
    query: String,
    uiState: SettingsUiState,
    entries: List<ResolvedSettingsEntry>,
): List<SettingsSearchGroup> =
    groupSettingsResults(
        matchSettings(query, entries.filter { it.entry.available(uiState) })
    )

/**
 * The results under the search field: one group per section, in the settings page's own order.
 */
@Composable
fun SettingsSearchResults(
    groups: List<SettingsSearchGroup>,
    onSelect: (SettingsSearchEntry) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
) {
    if (groups.isEmpty()) {
        SettingsSearchEmptyState(modifier.fillMaxSize())
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        groups.forEach { group ->
            item(key = group.key) {
                SegmentedColumn(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                    title = group.title,
                ) {
                    group.results.forEach { result ->
                        item(key = result.entry.entry.id) {
                            SettingsResultRow(result) { onSelect(result.entry.entry) }
                        }
                    }
                }
            }
        }
        item(key = "spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

/**
 * What the full screen search shows before anything is typed: the sections themselves, so
 * opening the field and then remembering what you wanted is not a dead end.
 */
@Composable
fun SettingsSearchShortcuts(
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        SegmentedColumn(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
        ) {
            settingsSearchSections.forEach { section ->
                item(key = section.key) {
                    val label = stringResource(section.titleRes)
                    SegmentedListItem(
                        onClick = { onOpenSection(section.key) },
                        headlineContent = { Text(label) },
                        leadingContent = { IconTile(section.icon, label) },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsResultRow(result: SettingsSearchResult, onClick: () -> Unit) {
    val entry = result.entry
    SegmentedListItem(
        onClick = onClick,
        headlineContent = { Text(matchedText(entry.title, result.titleMatches)) },
        supportingContent = if (entry.summary.isNotEmpty()) {
            { Text(matchedText(entry.summary, result.summaryMatches)) }
        } else {
            null
        },
        leadingContent = { IconTile(entry.entry.icon, entry.title) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
    )
}

/** Emphasises the parts of [text] the query hit; the rest of the row keeps its own style. */
private fun matchedText(text: String, ranges: List<IntRange>): AnnotatedString {
    if (ranges.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            if (start < end) addStyle(MatchEmphasis, start, end)
        }
    }
}

@Composable
private fun SettingsSearchEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_search_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The Miuix results under the search field: one card per section, in the settings page's own
 * order. Plain text rows — Miuix Text has no match highlighting in this codebase.
 */
@Composable
fun SettingsSearchResultsMiuix(
    groups: List<SettingsSearchGroup>,
    onSelect: (SettingsSearchEntry) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (groups.isEmpty()) {
        SettingsSearchEmptyStateMiuix(modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .overScrollVertical(),
        contentPadding = contentPadding,
    ) {
        item(key = "spacer-top") { Spacer(Modifier.height(6.dp)) }
        groups.forEach { group ->
            item(key = "title-${group.key}") {
                MiuixText(
                    text = group.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight(600),
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 28.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                )
            }
            item(key = group.key) {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    group.results.forEach { result ->
                        val entry = result.entry
                        ArrowPreference(
                            title = entry.title,
                            summary = entry.summary.ifEmpty { null },
                            startAction = {
                                MiuixIcon(
                                    entry.entry.icon,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = entry.title,
                                    tint = colorScheme.onBackground,
                                )
                            },
                            onClick = { onSelect(entry.entry) },
                        )
                    }
                }
            }
        }
        item(key = "spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

/** The Miuix shortcuts shown before anything is typed: the sections themselves. */
@Composable
fun SettingsSearchShortcutsMiuix(
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(6.dp))
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            settingsSearchSections.forEach { section ->
                val label = stringResource(section.titleRes)
                ArrowPreference(
                    title = label,
                    startAction = {
                        MiuixIcon(
                            section.icon,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = label,
                            tint = colorScheme.onBackground,
                        )
                    },
                    onClick = { onOpenSection(section.key) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsSearchEmptyStateMiuix(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MiuixText(
            text = stringResource(R.string.settings_search_empty),
            fontSize = 15.sp,
            fontWeight = FontWeight(500),
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
