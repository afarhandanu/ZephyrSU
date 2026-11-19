package com.sukisu.ultra.ui.screen.fptstore

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlinx.coroutines.launch
import com.sukisu.ultra.R
import com.sukisu.ultra.data.themestore.LocalTheme
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.component.material.ExpressiveScaffold
import com.sukisu.ultra.ui.component.material.SearchAppBar
import com.sukisu.ultra.ui.component.material.SnackBarHost
import com.sukisu.ultra.ui.component.material.TopBarBackButton
import com.sukisu.ultra.ui.component.themeimport.rememberFptThemeImportRequest
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.theme.MiuixSkinMaterialTheme
import com.sukisu.ultra.ui.viewmodel.FptThemeStoreViewModel

/**
 * The local `.fpt` theme library: everything already downloaded into app storage, each entry
 * applyable with a tap and removable with a long press. Applying goes through the same
 * `.fpt` importer the rest of the app uses.
 *
 * Adapted from FinaSU, plus an import action: FinaSU opens `.fpt` files from its wallpaper
 * screen (M5.2), so the local library gains its own file picker here until then.
 */
@Composable
fun FptMyThemesScreen() {
    // The Miuix skin provides no Material theme, so this Material page would
    // render white in dark mode without the wrapper (see MiuixSkinMaterialTheme).
    when (LocalUiMode.current) {
        UiMode.Material -> FptMyThemesScreenContent()
        UiMode.Miuix -> MiuixSkinMaterialTheme { FptMyThemesScreenContent() }
    }
}

@Composable
private fun FptMyThemesScreenContent() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<FptThemeStoreViewModel>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val gridState = rememberLazyStaggeredGridState()

    val appliedMessage = stringResource(R.string.theme_store_applied)
    val applyFailedMessage = stringResource(R.string.theme_store_apply_failed)
    val requestImport = rememberFptThemeImportRequest { result ->
        scope.launch { snackbarHostState.showSnackbar(if (result.isSuccess) appliedMessage else
            "$applyFailedMessage: ${result.exceptionOrNull()?.message.orEmpty()}") }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(requestImport)
    }

    var selectedTheme by remember { mutableStateOf<LocalTheme?>(null) }
    var deleteTheme by remember { mutableStateOf<LocalTheme?>(null) }

    ExpressiveScaffold(
        topBar = {
            SearchAppBar(
                snackbarHostState = snackbarHostState,
                title = { Text(stringResource(R.string.my_themes_title)) },
                searchText = viewModel.localSearchQuery,
                onSearchTextChange = viewModel::onLocalSearchQueryChange,
                onClearClick = { viewModel.onLocalSearchQueryChange("") },
                scrollBehavior = scrollBehavior,
                navigationIcon = { TopBarBackButton(onClick = { navigator.pop() }) },
                actions = {
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(
                            Icons.Filled.FileDownload,
                            contentDescription = stringResource(R.string.fpt_import_file_title),
                        )
                    }
                },
                searchContent = { bottomPadding, _ ->
                    MyThemeResults(
                        themes = viewModel.filteredLocalThemes,
                        contentPadding = searchContentPadding(bottomPadding),
                        onSelect = { selectedTheme = it },
                        onDelete = { deleteTheme = it },
                    )
                },
                defaultContent = { bottomPadding, _ ->
                    MyThemeResults(
                        themes = viewModel.filteredLocalThemes,
                        contentPadding = searchContentPadding(bottomPadding),
                        onSelect = { selectedTheme = it },
                        onDelete = { deleteTheme = it },
                    )
                },
            )
        },
        snackbarHost = { SnackBarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (viewModel.filteredLocalThemes.isEmpty()) {
                FptEmptyThemes(
                    message = stringResource(
                        if (viewModel.localThemes.isEmpty()) R.string.my_themes_empty
                        else R.string.theme_store_empty,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = viewModel.isRefreshingLocal,
                    onRefresh = { viewModel.loadLocalThemes() },
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            isRefreshing = viewModel.isRefreshingLocal,
                            state = pullToRefreshState,
                        )
                    },
                ) {
                    MyThemeResults(
                        themes = viewModel.filteredLocalThemes,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        gridState = gridState,
                        onSelect = { selectedTheme = it },
                        onDelete = { deleteTheme = it },
                    )
                }
            }
        }
    }

    val detail = selectedTheme
    if (detail != null) {
        AlertDialog(
            onDismissRequest = { selectedTheme = null },
            title = { Text(detail.name) },
            text = {
                Column {
                    Text(stringResource(R.string.theme_store_author, detail.author), style = MaterialTheme.typography.bodyMedium)
                    if (detail.version.isNotBlank()) {
                        Text(stringResource(R.string.theme_store_version, detail.version), style = MaterialTheme.typography.labelMedium)
                    }
                    if (detail.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(detail.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        requestImport(Uri.fromFile(File(detail.localPath)))
                    }
                    selectedTheme = null
                }) {
                    Text(stringResource(R.string.theme_store_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTheme = null }) {
                    Text(stringResource(R.string.theme_store_cancel))
                }
            },
        )
    }

    val toDelete = deleteTheme
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTheme = null },
            title = { Text(stringResource(R.string.my_themes_delete_title)) },
            text = { Text(stringResource(R.string.my_themes_delete_message, toDelete.name)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteTheme(toDelete)
                    deleteTheme = null
                }) {
                    Text(stringResource(R.string.my_themes_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTheme = null }) {
                    Text(stringResource(R.string.theme_store_cancel))
                }
            },
        )
    }
}

@Composable
private fun MyThemeResults(
    themes: List<LocalTheme>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onSelect: (LocalTheme) -> Unit,
    onDelete: (LocalTheme) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(128.dp),
        modifier = modifier.fillMaxSize(),
        state = gridState,
        contentPadding = contentPadding,
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(themes, key = { it.id }) { theme ->
            FptThemePreviewCard(
                name = theme.name,
                model = File(theme.previewImagePath).takeIf { it.exists() }
                    ?: theme.previewUrl.takeIf { it.isNotBlank() },
                onClick = { onSelect(theme) },
                onLongClick = { onDelete(theme) },
            )
        }
    }
}
