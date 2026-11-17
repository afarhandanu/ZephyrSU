package com.sukisu.ultra.ui.screen.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.sukisu.ultra.R
import com.sukisu.ultra.data.model.FolkMountMode
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.component.dialog.ConfirmResult
import com.sukisu.ultra.ui.component.dialog.rememberConfirmDialog
import com.sukisu.ultra.ui.navigation3.Navigator
import com.sukisu.ultra.ui.navigation3.Route
import com.sukisu.ultra.ui.viewmodel.MainActivityViewModel
import com.sukisu.ultra.ui.viewmodel.MainPagerConfig
import com.sukisu.ultra.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    val activityOwner = LocalActivity.current as? ViewModelStoreOwner
    val mainViewModel: MainActivityViewModel = if (activityOwner != null) {
        viewModel(viewModelStoreOwner = activityOwner)
    } else {
        viewModel()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pageRequest by mainViewModel.pageRequest.collectAsStateWithLifecycle()
    val sectionRequest = pageRequest?.takeIf {
        it.page == MainPagerConfig.LAST_PAGE_INDEX && isCurrentPage
    }
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            viewModel.refresh()
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            viewModel.refresh()
        }
        initialResumeHandled.value = true
        onPauseOrDispose { }
    }


    // Switching to NoMount changes who can see the injected files, so confirm it
    // once with the difference spelled out instead of changing it silently.
    val confirm = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    val onSetFolkMountMode: (FolkMountMode) -> Unit = { mode ->
        if (mode == FolkMountMode.NOMOUNT) {
            scope.launch {
                val result = confirm.awaitConfirm(
                    title = context.resources.getString(R.string.settings_folk_mount_mode_nomount),
                    content = context.resources.getString(R.string.settings_folk_mount_nomount_confirm),
                    confirm = context.resources.getString(R.string.settings_folk_mount_nomount_confirm_ok),
                )
                if (result is ConfirmResult.Confirmed) viewModel.setFolkMountMode(mode)
            }
        } else {
            viewModel.setFolkMountMode(mode)
        }
    }

    val actions = SettingsScreenActions(
        onSetCheckUpdate = viewModel::setCheckUpdate,
        onSetCheckCiUpdate = viewModel::setCheckCiUpdate,
        onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
        onSetAutoDeleteModuleZip = viewModel::setAutoDeleteModuleZip,
        onSetAutoDeleteUpdateApk = viewModel::setAutoDeleteUpdateApk,
        onOpenTheme = { navigator.push(Route.ColorPalette()) },
        onOpenThemeStore = { navigator.push(Route.ThemeStore()) },
        onOpenWallpaper = { navigator.push(Route.Wallpaper) },
        onOpenWallpaperGallery = { navigator.push(Route.WallpaperGallery) },
        onSetUiModeIndex = { index ->
            viewModel.setUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
        },
        onSetHomeLayoutStyle = viewModel::setHomeLayoutStyle,
        onSetShowNavSuperuser = viewModel::setShowNavSuperuser,
        onSetShowNavModule = viewModel::setShowNavModule,
        onSetFloatingAutoHide = viewModel::setFloatingAutoHide,
        onSetFloatingSwipeHide = viewModel::setFloatingSwipeHide,
        onOpenProfileTemplate = { navigator.push(Route.AppProfileTemplate) },
        onSetLanguage = { tag -> viewModel.setLanguage(context, tag) },
        onSetSuCompatMode = viewModel::setSuCompatMode,
        onSetKernelUmountEnabled = viewModel::setKernelUmountEnabled,
        onSetSelinuxHideEnabled = viewModel::setSelinuxHideEnabled,
        onSetAvcSpoofEnabled = viewModel::setAvcSpoofEnabled,
        onSetMountHideEnabled = viewModel::setMountHideEnabled,
        onSetSulogEnabled = viewModel::setSulogEnabled,
        onSetAdbRootEnabled = viewModel::setAdbRootEnabled,
        onSetShellSuAdbOnlyEnabled = viewModel::setShellSuAdbOnlyEnabled,
        onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
        onSetEnableWebDebugging = viewModel::setEnableWebDebugging,
        onSetAutoJailbreak = viewModel::setAutoJailbreak,
        onSetBuiltinZygisk = viewModel::setBuiltinZygisk,
        onDismissBuiltinZygiskFailure = viewModel::dismissBuiltinZygiskFailure,
        onSetUseSoftReboot = viewModel::setUseSoftReboot,
        onOpenAbout = { navigator.push(Route.About) },
        onOpenTools = { navigator.push(Route.Tool) },
        onOpenSusfsConfig = { navigator.push(Route.SuSFS()) },
        onOpenExtensions = { navigator.push(Route.Extensions) },
        onOpenBuiltinZygisk = { navigator.push(Route.BuiltinZygisk) },
        onSetSuiEnabled = viewModel::setSuiEnabled,
        onSetFolkMountMode = onSetFolkMountMode,
        onRepairSui = viewModel::repairSui,
        onManageSuiPermissions = { navigator.push(Route.SuiPermissions) },
        onSetBootHideEnabled = viewModel::setBootHideEnabled,
        onSetSELinuxMode = viewModel::setSELinuxMode,
        onOpenFont = { navigator.push(Route.Font) },
        onOpenMultimedia = { navigator.push(Route.Multimedia) },
        onOpenAuthentication = { navigator.push(Route.Authentication) },
        onOpenRoute = { navigator.push(it) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            sectionRequest = sectionRequest,
            onSectionConsumed = { token -> mainViewModel.consumePageRequest(token) },
        )
        UiMode.Material -> SettingPagerMaterial(
            uiState = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            sectionRequest = sectionRequest,
            onSectionConsumed = { token -> mainViewModel.consumePageRequest(token) },
        )
    }
}
