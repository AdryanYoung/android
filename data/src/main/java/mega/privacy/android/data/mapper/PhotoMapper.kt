package mega.privacy.android.data.mapper

import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * The mapper class for converting the data entity to Photo.Image
 */
typealias ImageMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long?,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards FileTypeInfo,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards String,
) -> @JvmSuppressWildcards Photo.Image

internal fun toImage(
    id: Long,
    albumPhotoId: Long? = null,
    parentId: Long,
    name: String,
    isFavourite: Boolean,
    creationTime: LocalDateTime,
    modificationTime: LocalDateTime,
    thumbnailFilePath: String?,
    previewFilePath: String?,
    fileTypeInfo: FileTypeInfo,
    size: Long,
    isTakenDown: Boolean,
    isSensitive: Boolean,
    isSensitiveInherited: Boolean,
    base64Id: String,
) = Photo.Image(
    id = id,
    albumPhotoId = albumPhotoId,
    parentId = parentId,
    name = name,
    isFavourite = isFavourite,
    creationTime = creationTime,
    modificationTime = modificationTime,
    thumbnailFilePath = thumbnailFilePath,
    previewFilePath = previewFilePath,
    fileTypeInfo = fileTypeInfo,
    size = size,
    isTakenDown = isTakenDown,
    isSensitive = isSensitive,
    isSensitiveInherited = isSensitiveInherited,
    base64Id = base64Id,
)

/**
 * The mapper class for converting the data entity to Photo.Video
 */
typealias VideoMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long?,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards LocalDateTime,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards FileTypeInfo,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards String,
) -> @JvmSuppressWildcards Photo.Video

internal fun toVideo(
    id: Long,
    albumPhotoId: Long? = null,
    parentId: Long,
    name: String,
    isFavourite: Boolean,
    creationTime: LocalDateTime,
    modificationTime: LocalDateTime,
    thumbnailFilePath: String?,
    previewFilePath: String?,
    fileTypeInfo: FileTypeInfo,
    size: Long,
    isTakenDown: Boolean,
    isSensitive: Boolean,
    isSensitiveInherited: Boolean,
    base64Id: String,
) = Photo.Video(
    id = id,
    albumPhotoId = albumPhotoId,
    parentId = parentId,
    name = name,
    isFavourite = isFavourite,
    creationTime = creationTime,
    modificationTime = modificationTime,
    thumbnailFilePath = thumbnailFilePath,
    previewFilePath = previewFilePath,
    fileTypeInfo = fileTypeInfo as VideoFileTypeInfo,
    size = size,
    isTakenDown = isTakenDown,
    isSensitive = isSensitive,
    isSensitiveInherited = isSensitiveInherited,
    base64Id = base64Id
)

class PhotoMapper @Inject constructor(
    private val imageMapper: ImageMapper,
    private val videoMapper: VideoMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val dateUtilFacade: DateUtilWrapper,
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
    private val megaApiGateway: MegaApiGateway,
) {
    suspend operator fun invoke(node: MegaNode, albumPhotoId: AlbumPhotoId?): Photo? {
        return when (val fileType = fileTypeInfoMapper(node.name, node.duration)) {
            is ImageFileTypeInfo -> {
                imageMapper(
                    node.handle,
                    albumPhotoId?.id,
                    node.parentHandle,
                    node.name,
                    node.isFavourite,
                    dateUtilFacade.fromEpoch(node.creationTime),
                    dateUtilFacade.fromEpoch(node.modificationTime),
                    getThumbnailFilePath(node),
                    getPreviewFilePath(node),
                    fileType,
                    node.size,
                    node.isTakenDown,
                    node.isMarkedSensitive,
                    megaApiGateway.isSensitiveInherited(node),
                    node.base64Handle
                )
            }

            is VideoFileTypeInfo -> {
                videoMapper(
                    node.handle,
                    albumPhotoId?.id,
                    node.parentHandle,
                    node.name,
                    node.isFavourite,
                    dateUtilFacade.fromEpoch(node.creationTime),
                    dateUtilFacade.fromEpoch(node.modificationTime),
                    getThumbnailFilePath(node),
                    getPreviewFilePath(node),
                    fileType,
                    node.size,
                    node.isTakenDown,
                    node.isMarkedSensitive,
                    megaApiGateway.isSensitiveInherited(node),
                    node.base64Handle
                )
            }

            else -> {
                null
            }
        }
    }

    private suspend fun getThumbnailFilePath(node: MegaNode): String? {
        return thumbnailPreviewRepository.getThumbnailCacheFolderPath()?.let { path ->
            "$path${File.separator}${node.getThumbnailFileName()}"
        }
    }

    private suspend fun getPreviewFilePath(node: MegaNode): String? {
        return thumbnailPreviewRepository.getPreviewCacheFolderPath()?.let { path ->
            "$path${File.separator}${node.getPreviewFileName()}"
        }
    }
}
