package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Ui state for attachment messages
 * @property fileTypeResId the icon resource id, null if there is no icon
 * @property fileName
 * @property fileSize in bytes
 * @property previewUri uri string to file preview
 * @property duration String representation of the duration of the file in case it's playable, null otherwise
 * @property loadProgress load progress, null if it's not loading
 * @property compressionProgress compression progress, null if it's not compressing
 * @property isError true if there was any error downloading, uploading or attaching the file
 * @property areTransfersPaused true if the transfers are paused.
 */
data class AttachmentMessageUiState(
    val fileTypeResId: Int? = null,
    val fileName: String = "",
    val fileSize: String = "",
    val previewUri: UriPath? = null,
    val duration: String? = null,
    val loadProgress: Progress? = null,
    val compressionProgress: Progress? = null,
    val isError: Boolean = false,
    val areTransfersPaused: Boolean = false,
) {
    val progress = if (compressionProgress == null) {
        loadProgress
    } else {
        Progress((compressionProgress.floatValue + (loadProgress?.floatValue ?: 0f)) / 2)
    }
}