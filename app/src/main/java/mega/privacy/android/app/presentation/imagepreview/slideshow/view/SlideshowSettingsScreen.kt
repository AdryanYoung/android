package mega.privacy.android.app.presentation.imagepreview.slideshow.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.SlideshowSettingViewModel
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

@Composable
fun SlideshowSettingScreen(
    slideshowSettingViewModel: SlideshowSettingViewModel = hiltViewModel(),
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    MegaScaffold(
        scaffoldState = rememberScaffoldState(),
        topBar = {
            MegaAppBar(
                title = stringResource(R.string.slideshow_settings_page_title),
                appBarType = AppBarType.BACK_NAVIGATION,
                elevation = 0.dp,
                onNavigationPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            SlideshowSettingsView(
                slideshowSettingViewModel = slideshowSettingViewModel,
                topPadding = 0.dp,
            )
        }
    }
}