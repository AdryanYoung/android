package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.file.FileMessageView

/**
 * View for chat message with node attachment
 */
@Composable
fun NodeAttachmentMessageView(
    message: NodeAttachmentMessage,
    uiState: AttachmentMessageUiState,
    modifier: Modifier = Modifier,
) {
    FileMessageView(
        isMe = message.isMine,
        fileTypeResId = uiState.fileTypeResId,
        previewUri = uiState.previewUri?.toUri(),
        loadProgress = uiState.progress?.floatValue,
        fileName = uiState.fileName,
        fileSize = uiState.fileSize,
        duration = uiState.duration,
        modifier = modifier,
    )
}
