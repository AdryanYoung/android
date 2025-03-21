package mega.privacy.android.app.presentation.photos.compose.photosfilter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.view.PhotosFilterView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.applyFilter
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onMediaTypeSelected
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onSourceSelected
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingFilterPage
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.updateRememberPreferences

@Composable
fun PhotosFilterScreen(
    timelineViewModel: TimelineViewModel,
    onExitScreen: () -> Unit,
) {
    val timeState by timelineViewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    PhotosFilterView(
        timelineViewState = timeState,
        onMediaTypeSelected = timelineViewModel::onMediaTypeSelected,
        onSourceSelected = timelineViewModel::onSourceSelected,
        applyFilter = { rememberFilter ->
            coroutineScope.launch {
                timelineViewModel.showingFilterPage(isShowing = false)
                timelineViewModel.applyFilter(rememberFilter)
                onExitScreen()
            }
        },
    )
}
