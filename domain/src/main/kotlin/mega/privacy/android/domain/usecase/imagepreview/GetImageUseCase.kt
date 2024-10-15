package mega.privacy.android.domain.usecase.imagepreview

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Fetch Thumbnail, Preview and Full Size Image given ImageNode
 */
class GetImageUseCase @Inject constructor(
    private val isFullSizeRequiredUseCase: IsFullSizeRequiredUseCase,
    private val photosRepository: PhotosRepository,
) {
    /**
     * Invoke
     *
     * @param node                  Typed Image Node
     * @param fullSize              Flag to request full size image despite data/size requirements
     * @param highPriority          Flag to request image with high priority
     * @param resetDownloads        Callback to reset downloads
     *
     * @return Flow<ImageResult>
     */
    operator fun invoke(
        node: TypedImageNode,
        fullSize: Boolean,
        highPriority: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> {
        return photosRepository.monitorImageResult(node.id) ?: flow {
            val imageResult = ImageResult(
                isVideo = node.type is VideoFileTypeInfo,
                thumbnailUri = node.thumbnailPath?.let { "$FILE$it" },
                previewUri = node.previewPath?.let { "$FILE$it" },
                fullSizeUri = node.fullSizePath?.let { "$FILE$it" },
            )

            val fullSizeRequired = isFullSizeRequiredUseCase(node, fullSize)

            if ((!fullSizeRequired && node.previewPath != null) || node.fullSizePath != null) {
                imageResult.isFullyLoaded = true
                emit(imageResult)
                photosRepository.saveImageResult(node.id, imageResult)
                return@flow
            } else {
                emit(imageResult)
                photosRepository.saveImageResult(node.id, imageResult)
            }

            if (node.thumbnailPath == null) {
                runCatching {
                    node.fetchThumbnail()
                }.onSuccess {
                    imageResult.thumbnailUri = "$FILE$it"
                    emit(imageResult)
                    photosRepository.saveImageResult(node.id, imageResult)
                }
            }

            if (node.previewPath == null) {
                runCatching {
                    node.fetchPreview()
                }.onSuccess {
                    imageResult.previewUri = "$FILE$it"
                    if (fullSizeRequired) {
                        emit(imageResult)
                        photosRepository.saveImageResult(node.id, imageResult)
                    } else {
                        imageResult.isFullyLoaded = true
                        emit(imageResult)
                        photosRepository.saveImageResult(node.id, imageResult)
                        return@flow
                    }
                }.onFailure { exception ->
                    if (!fullSizeRequired) {
                        throw exception
                    }
                }
            }

            if (fullSizeRequired) {
                node.fetchFullImage(highPriority) {
                    resetDownloads()
                }.catch { exception -> throw exception }.collect { result ->
                    when (result) {
                        is ImageProgress.Started -> {
                            imageResult.transferTag = result.transferTag
                            emit(imageResult)
                            photosRepository.saveImageResult(node.id, imageResult)
                        }

                        is ImageProgress.InProgress -> {
                            imageResult.totalBytes = result.totalBytes
                            imageResult.transferredBytes = result.transferredBytes
                            emit(imageResult)
                            photosRepository.saveImageResult(node.id, imageResult)
                        }

                        is ImageProgress.Completed -> {
                            imageResult.isFullyLoaded = true
                            imageResult.fullSizeUri = "$FILE${result.path}"
                            emit(imageResult)
                            photosRepository.saveImageResult(node.id, imageResult)
                        }
                    }
                }
            }
        }
    }

    companion object {
        /**
         * File path Prefix
         */
        const val FILE = "file://"
    }
}

