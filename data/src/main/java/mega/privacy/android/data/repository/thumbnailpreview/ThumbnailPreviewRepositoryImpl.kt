package mega.privacy.android.data.repository.thumbnailpreview

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.MegaNodeMapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal class ThumbnailPreviewRepositoryImpl @Inject constructor(
    private val megaApi: MegaApiGateway,
    private val megaApiFolder: MegaApiFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val stringWrapper: StringWrapper,
    private val megaNodeMapper: MegaNodeMapper,
) : ThumbnailPreviewRepository {

    override suspend fun getThumbnailFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            cacheGateway.getCacheFile(
                CacheFolderConstant.THUMBNAIL_FOLDER,
                getThumbnailOrPreviewFileName(handle)
            )?.takeIf { it.exists() }
        }

    override suspend fun getPublicNodeThumbnailFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(handle)?.run {
                getThumbnailFile(this).takeIf {
                    it?.exists() ?: false
                }
            }
        }

    private suspend fun getThumbnailFile(node: MegaNode): File? =
        cacheGateway.getCacheFile(
            CacheFolderConstant.THUMBNAIL_FOLDER,
            "${node.base64Handle}${FileConstant.JPG_EXTENSION}"
        )

    override suspend fun getThumbnailFromServer(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(handle)?.let { node ->
                if (!node.hasThumbnail()) return@withContext null
                getThumbnailFile(node)?.let { thumbnail ->
                    suspendCancellableCoroutine { continuation ->
                        val listener = continuation.getRequestListener("getThumbnailFromServer") {
                            thumbnail
                        }
                        megaApi.getThumbnail(node, thumbnail.absolutePath, listener)
                    }
                }
            }
        }

    override suspend fun getPublicNodeThumbnailFromServer(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(handle)?.let { node ->
                getThumbnailFile(node)?.let { thumbnail ->
                    suspendCancellableCoroutine { continuation ->
                        val listener =
                            continuation.getRequestListener("getPublicNodeThumbnailFromServer") {
                                thumbnail
                            }
                        megaApiFolder.getThumbnail(node, thumbnail.absolutePath, listener)
                    }
                }
            }
        }

    private suspend fun getPreviewFile(node: MegaNode): File? =
        cacheGateway.getCacheFile(
            CacheFolderConstant.PREVIEW_FOLDER,
            "${node.base64Handle}${FileConstant.JPG_EXTENSION}"
        )


    override suspend fun getPreviewFromLocal(typedNode: TypedNode): File? =
        withContext(ioDispatcher) {
            runCatching {
                megaNodeMapper(typedNode)?.run {
                    getPreviewFile(this).takeIf {
                        it?.exists() ?: false
                    }
                }
            }.onFailure {
                Timber.e(it)
            }.getOrNull()
        }

    override suspend fun getPreviewFromServer(typedNode: TypedNode): File? =
        withContext(ioDispatcher) {
            runCatching {
                megaNodeMapper(typedNode)?.let { node ->
                    getPreviewFile(node)?.let { preview ->
                        suspendCancellableCoroutine { continuation ->
                            val listener = continuation.getRequestListener("getPreviewFromServer") {
                                preview
                            }
                            megaApi.getPreview(node, preview.absolutePath, listener)
                        }
                    }
                }
            }.onFailure {
                Timber.e(it)
            }.getOrNull()
        }

    override suspend fun downloadThumbnail(
        handle: Long,
        callback: (success: Boolean) -> Unit,
    ) = withContext(ioDispatcher) {
        val node = megaApi.getMegaNodeByHandle(handle)
        val thumbnailFolderPath =
            cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.path

        if (node == null || thumbnailFolderPath == null || !node.hasThumbnail()) {
            callback(false)
        } else {
            megaApi.getThumbnail(
                node,
                getThumbnailPath(thumbnailFolderPath, node),
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        callback(error.errorCode == MegaError.API_OK)
                    }
                )
            )
        }
    }

    override suspend fun downloadPreview(
        handle: Long,
        callback: (success: Boolean) -> Unit,
    ) = withContext(ioDispatcher) {
        val node = megaApi.getMegaNodeByHandle(handle)
        val previewFolderPath =
            cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.path

        if (node == null || previewFolderPath == null || !node.hasPreview()) {
            callback(false)
        } else {
            megaApi.getPreview(
                node,
                getPreviewPath(previewFolderPath, node),
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        callback(error.errorCode == MegaError.API_OK)
                    }
                )
            )
        }
    }

    override suspend fun downloadPublicNodeThumbnail(
        handle: Long,
    ): Boolean = withContext(ioDispatcher) {
        val node = megaApiFolder.getMegaNodeByHandle(handle)
        val thumbnailFolderPath =
            cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.path

        if (node == null || thumbnailFolderPath == null || !node.hasThumbnail()) {
            return@withContext false
        } else {
            return@withContext suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getThumbnail") {
                    true
                }
                megaApi.getThumbnail(
                    node,
                    getThumbnailPath(thumbnailFolderPath, node),
                    listener
                )
            }
        }
    }

    override suspend fun downloadPublicNodePreview(
        handle: Long,
    ): Boolean = withContext(ioDispatcher) {
        val node = megaApiFolder.getMegaNodeByHandle(handle)
        val previewFolderPath =
            cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.path

        if (node == null || previewFolderPath == null || !node.hasPreview()) {
            return@withContext false
        } else {
            return@withContext suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getThumbnail") {
                    true
                }
                megaApi.getPreview(
                    node,
                    getPreviewPath(previewFolderPath, node),
                    listener
                )

            }
        }
    }

    private fun getPreviewPath(previewFolderPath: String, megaNode: MegaNode) =
        "$previewFolderPath${File.separator}${megaNode.getPreviewFileName()}"

    private fun getThumbnailPath(thumbnailFolderPath: String, megaNode: MegaNode) =
        "$thumbnailFolderPath${File.separator}${megaNode.getThumbnailFileName()}"

    private suspend fun getThumbnailFile(fileName: String): File? =
        cacheGateway.getCacheFile(CacheFolderConstant.THUMBNAIL_FOLDER, fileName)


    private suspend fun getPreviewFile(fileName: String): File? =
        cacheGateway.getCacheFile(CacheFolderConstant.PREVIEW_FOLDER, fileName)

    override suspend fun getThumbnailCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getThumbnailCacheFolder()?.path
    }

    override suspend fun getPreviewCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getPreviewCacheFolder()?.path
    }

    override suspend fun getFullSizeCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getFullSizeCacheFolder()?.path
    }


    override suspend fun createThumbnail(handle: Long, uriPath: UriPath) =
        withContext(ioDispatcher) {
            val thumbnailFileName = getThumbnailOrPreviewFileName(handle)
            val thumbnailFile = getThumbnailFile(thumbnailFileName)
            requireNotNull(thumbnailFile)
            megaApi.createThumbnail(uriPath.value, thumbnailFile.absolutePath)
        }


    override suspend fun createPreview(handle: Long, uriPath: UriPath) = withContext(ioDispatcher) {
        val previewFileName = getThumbnailOrPreviewFileName(handle)
        val previewFile = getPreviewFile(previewFileName)
        requireNotNull(previewFile)
        megaApi.createPreview(uriPath.value, previewFile.absolutePath)
    }

    override suspend fun createPreview(name: String, file: File) =
        withContext(ioDispatcher) {
            val previewFileName = getThumbnailOrPreviewFileName(name)
            val previewFile = getPreviewFile(previewFileName)
            requireNotNull(previewFile)
            megaApi.createPreview(file.absolutePath, previewFile.absolutePath)
        }

    override suspend fun deleteThumbnail(handle: Long) = withContext(ioDispatcher) {
        val thumbnailFileName = getThumbnailOrPreviewFileName(handle)
        getThumbnailFile(thumbnailFileName)?.takeIf { it.exists() }?.delete()
    }

    override suspend fun deletePreview(handle: Long) = withContext(ioDispatcher) {
        val previewFileName = getThumbnailOrPreviewFileName(handle)
        getPreviewFile(previewFileName)?.takeIf { it.exists() }?.delete()
    }

    override suspend fun getThumbnailOrPreviewFileName(nodeHandle: Long) =
        withContext(ioDispatcher) {
            "${megaApi.handleToBase64(nodeHandle)}.jpg"
        }

    override suspend fun getThumbnailOrPreviewFileName(name: String) =
        withContext(ioDispatcher) {
            "${stringWrapper.encodeBase64(name)}.jpg"
        }

    override suspend fun setThumbnail(nodeHandle: Long, srcFilePath: String) =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let {
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("setThumbnail") {}
                    megaApi.setThumbnail(it, srcFilePath, listener)
                }
            } ?: Unit
        }

    override suspend fun setPreview(nodeHandle: Long, srcFilePath: String) =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let {
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("setPreview") {}
                    megaApi.setPreview(it, srcFilePath, listener)
                }
            } ?: Unit
        }
}
