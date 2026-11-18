package com.sukisu.ultra.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukisu.ultra.BuildConfig
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.component.onboarding.FirstRunGuide
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.theme.MiuixSkinMaterialTheme
import com.sukisu.ultra.ui.util.BuiltinAppIcons
import com.sukisu.ultra.ui.util.customText
import com.sukisu.ultra.ui.util.CustomTextKey
import com.sukisu.ultra.ui.viewmodel.SettingsViewModel

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val viewModel = viewModel<SettingsViewModel>()
    // First-run guide replay (ported from FinaSU, Milestone 3).
    var showGuide by remember { mutableStateOf(false) }
    val settingsState by viewModel.uiState.collectAsStateWithLifecycle()
    val htmlString = stringResource(
        id = R.string.about_source_code,
        "<b><a href=\"https://github.com/afarhandanu/ZephyrSU\">GitHub</a></b>",
        "<b><a href=\"https://t.me/SukiKSU\">Telegram</a></b>",
    )
    val state = AboutUiState(
        title = customText(CustomTextKey.PAGE_ABOUT),
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        icon = BuiltinAppIcons.find(settingsState.appIconId),
        links = extractLinks(htmlString),
        checkUpdate = settingsState.checkUpdate,
        checkCiUpdate = settingsState.checkCiUpdate,
        checkModuleUpdate = settingsState.checkModuleUpdate,
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = uriHandler::openUri,
        onSetCheckUpdate = viewModel::setCheckUpdate,
        onSetCheckCiUpdate = viewModel::setCheckCiUpdate,
        onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
        onShowGuide = { showGuide = true },
    )

    // The Material 3 Expressive page is shared by both skins; the Miuix skin has no Material theme above it.
    when (LocalUiMode.current) {
        UiMode.Material -> AboutScreenMaterial(state, actions)
        UiMode.Miuix -> MiuixSkinMaterialTheme { AboutScreenMaterial(state, actions) }
    }
    if (showGuide) {
        FirstRunGuide(onDismiss = { showGuide = false })
    }
}
