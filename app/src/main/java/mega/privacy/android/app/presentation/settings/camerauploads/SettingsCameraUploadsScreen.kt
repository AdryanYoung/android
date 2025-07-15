package mega.privacy.android.app.presentation.settings.camerauploads

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.presentation.settings.camerauploads.navigation.SettingsCameraUploadsNavHostController
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

/**
 * A Composable holding all Settings Camera Uploads screens using the Navigation Controller
 * @param isShowHowToUploadPrompt Boolean indicating whether to show the how-to upload prompt
 */
@Composable
internal fun SettingsCameraUploadsScreen(
    isShowHowToUploadPrompt: Boolean,
    isShowDisableCameraUploads: Boolean,
) {
    val navHostController = rememberNavController()

    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
    ) { padding ->
        SettingsCameraUploadsNavHostController(
            modifier = Modifier.padding(padding),
            navHostController = navHostController,
            isShowHowToUploadPrompt = isShowHowToUploadPrompt,
            isShowDisableCameraUploads = isShowDisableCameraUploads,
        )
    }
}