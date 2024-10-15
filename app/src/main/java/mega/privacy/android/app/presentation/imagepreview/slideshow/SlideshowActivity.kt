package mega.privacy.android.app.presentation.imagepreview.slideshow

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowScreen
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowSettingScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
class SlideshowActivity : BaseActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode
    private val slideshowViewModel: SlideshowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            val mode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTempTheme(isDark = mode.isDarkMode()) {
                NavHost(navController, startDestination = "slideshow") {
                    composable("slideshow") {
                        SlideshowScreen(
                            viewModel = slideshowViewModel,
                            onClickSettingMenu = {
                                navController.navigate("slideshowSetting")
                            },
                            onClickBack = ::finish,
                        )
                    }

                    composable("slideshowSetting") {
                        SlideshowSettingScreen()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        slideshowViewModel.clearImageResultCache()
        super.onDestroy()
    }
}