package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.domain.entity.VideoAttachment
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VideoRepository
import javax.inject.Inject

internal class VideoRepositoryImpl @Inject constructor(
    private val videoCompressorGateway: VideoCompressorGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoRepository {

    override fun compressVideo(
        root: String,
        original: UriPath,
        newFilePath: String,
        quality: VideoQuality,
    ): Flow<VideoCompressionState> =
        videoCompressorGateway.apply {
            setOutputRoot(root)
            setVideoQuality(quality)
            addItems(
                listOf(
                    VideoAttachment(
                        original,
                        newFilePath,
                        id = null,
                        pendingMessageId = null,
                    )
                )
            )
        }.start()
            .cancellable()
            .flowOn(ioDispatcher)


}
