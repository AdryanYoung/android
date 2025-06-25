package mega.privacy.android.app.utils

import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.EntryPointsModule
import mega.privacy.android.data.gateway.CacheFolderGateway
import java.io.File

/**
 * CacheFolder Manager
 *
 * Call to corresponding [CacheRepository] to perform operations related to cache folders.
 */
object CacheFolderManager {
    /**
     * THUMBNAIL_FOLDER
     */
    const val THUMBNAIL_FOLDER = "thumbnailsMEGA"

    /**
     * PREVIEW_FOLDER
     */
    const val PREVIEW_FOLDER = "previewsMEGA"

    /**
     * AVATAR_FOLDER
     */
    const val AVATAR_FOLDER = "avatarsMEGA"

    /**
     * QR_FOLDER
     */
    private const val QR_FOLDER = "qrMEGA"

    /**
     * VOICE_CLIP_FOLDER
     */
    const val VOICE_CLIP_FOLDER = "voiceClipsMEGA"

    /**
     * TEMPORARY_FOLDER
     */
    const val TEMPORARY_FOLDER = "tempMEGA"

    /**
     * CacheFolder Gateway
     */
    val cacheFolderGateway: CacheFolderGateway by lazy {
        EntryPointAccessors.fromApplication(
            MegaApplication.getInstance(),
            EntryPointsModule.CacheFolderManagerEntryPoint::class.java
        ).cacheFolderGateway
    }

    /**
     * Get Cache Folder given folder Name
     */
    @JvmStatic
    fun getCacheFolder(folderName: String): File? = cacheFolderGateway.getCacheFolder(folderName)

    /**
     * Create Cache Folders
     */
    @JvmStatic
    fun createCacheFolders() {
        cacheFolderGateway.apply {
            createCacheFolder(THUMBNAIL_FOLDER)
            createCacheFolder(PREVIEW_FOLDER)
            createCacheFolder(AVATAR_FOLDER)
            createCacheFolder(QR_FOLDER)
            createCacheFolder(VOICE_CLIP_FOLDER)
        }
    }

    /**
     * Get Avatar Cache File Instance
     */
    @JvmStatic
    fun buildAvatarFile(fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(AVATAR_FOLDER, fileName)
    }

    /**
     * Get Voice Clip Cache File Instance
     */
    @JvmStatic
    fun buildVoiceClipFile(fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(VOICE_CLIP_FOLDER, fileName)
    }

    /**
     * Get Temp Cache File Instance
     */
    @JvmStatic
    fun buildTempFile(fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(TEMPORARY_FOLDER, fileName)
    }

    /**
     * Get Cache File Instance given folderName & fileName
     */
    @JvmStatic
    fun getCacheFile(folderName: String, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(folderName, fileName)
    }
}