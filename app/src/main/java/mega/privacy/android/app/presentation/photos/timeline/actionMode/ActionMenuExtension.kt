package mega.privacy.android.app.presentation.photos.timeline.actionMode

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.clearSelectedPhotos
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.selectAllShowingPhotos
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil

fun PhotosFragment.actionSaveToDevice() {
    lifecycleScope.launch {
        val selectedNodes = timelineViewModel.getSelectedNodes()
        managerActivity.saveNodesToDevice(
            nodes = selectedNodes,
            highPriority = false,
            isFolderLink = false,
            fromChat = false,
            withStartMessage = true,
        )
    }
}

fun PhotosFragment.actionShareLink() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    try {
        selectedPhotosIds.let {
            if (it.size == 1) {
                LinksUtil.showGetLinkActivity(managerActivity, it[0])
            } else {
                LinksUtil.showGetLinkActivity(managerActivity, it.toLongArray())
            }
        }
    } catch (e: Exception) {
        e.printStackTrace() // workaround for potential risk if selectedPhotosIds is huge, need to refactor showGetLinkActivity
    }
}

fun PhotosFragment.actionSendToChat() {
    lifecycleScope.launch {
        val selectedNodes = timelineViewModel.getSelectedNodes()
        managerActivity.attachNodesToChats(selectedNodes)
    }
}

fun PhotosFragment.actionShareOut() {
    lifecycleScope.launch {
        val selectedNodes = timelineViewModel.getSelectedNodes()
        MegaNodeUtil.shareNodes(managerActivity, selectedNodes)
    }
}


fun PhotosFragment.actionSelectAll() {
    timelineViewModel.selectAllShowingPhotos()
}


fun PhotosFragment.actionClearSelection() {
    timelineViewModel.clearSelectedPhotos()
}

fun PhotosFragment.actionMove() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    NodeController(managerActivity).chooseLocationToMoveNodes(selectedPhotosIds)
}


fun PhotosFragment.actionCopy() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    NodeController(managerActivity).chooseLocationToCopyNodes(selectedPhotosIds)
}


fun PhotosFragment.actionMoveToTrash() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    if (selectedPhotosIds.isNotEmpty()) {
        ConfirmMoveToRubbishBinDialogFragment.newInstance(selectedPhotosIds)
            .show(
                managerActivity.supportFragmentManager,
                ConfirmMoveToRubbishBinDialogFragment.TAG
            )
    }
}

fun PhotosFragment.actionRemoveLink() {
    RemovePublicLinkDialogFragment.newInstance(
        listOf(
            timelineViewModel.getSelectedIds().firstOrNull() ?: 0
        )
    )
        .show(requireActivity().supportFragmentManager, RemovePublicLinkDialogFragment.TAG)
}

fun PhotosFragment.destroyActionMode() {
    timelineViewModel.clearSelectedPhotos()
}
