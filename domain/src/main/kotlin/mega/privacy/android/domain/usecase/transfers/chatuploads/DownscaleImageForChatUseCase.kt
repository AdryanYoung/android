package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.ChatUploadNotCompressedReason
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForChatUploadUseCase
import javax.inject.Inject

/**
 * Use case to downscale an image that will be attached to a chat before uploading and return the state of the compression..
 */
class DownscaleImageForChatUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val getCacheFileForChatUploadUseCase: GetCacheFileForChatUploadUseCase,
) {
    /**
     * Invoke
     *
     * @return the state of the compression.
     */
    suspend operator fun invoke(uriPath: UriPath): Flow<ChatUploadCompressionState> {
        return getCacheFileForChatUploadUseCase(uriPath)?.let { destination ->
            fileSystemRepository.downscaleImage(
                original = uriPath,
                destination = destination,
                maxPixels = DOWNSCALE_IMAGES_PX
            )
            flowOf(
                if (destination.exists()) {
                    ChatUploadCompressionState.Compressed(destination)
                } else {
                    ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.FailedToCompress)
                }
            )
        } ?: flowOf(
            ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.NoCacheFile)
        )
    }

    companion object {
        internal const val DOWNSCALE_IMAGES_PX = 2000000L
    }
}