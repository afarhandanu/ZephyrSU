package com.sukisu.ultra.ui.screen.plugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.viewmodel.PluginStoreViewModel

@Composable
fun PluginStoreScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<PluginStoreViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.ensureLoaded() }

    val actions = PluginStoreActions(
        onBack = { navigator.pop() },
        onRefresh = viewModel::refresh,
        onSearchTextChange = viewModel::updateSearchText,
        onClearSearch = viewModel::clearSearch,
        onInstall = viewModel::install,
        onNoticeShown = viewModel::clearNotice,
        onErrorShown = viewModel::clearError,
    )

    PluginStoreMaterial(
        uiState = uiState,
        actions = actions,
    )
}
