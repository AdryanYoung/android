package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.exception.MegaException

/**
 * Repository for media player
 */
interface MediaPlayerRepository {

    /**
     * Returns a URL to a node in the local HTTP proxy server for folder link from MegaApiFolder
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun getLocalLinkForFolderLinkFromMegaApiFolder(nodeHandle: Long): String?

    /**
     * Returns a URL to a node in the local HTTP proxy server for folder link from MegaApi
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun getLocalLinkForFolderLinkFromMegaApi(nodeHandle: Long): String?

    /**
     * Returns a URL to a node in the local HTTP proxy server from MegaApi
     *
     * @param nodeHandle node Handle
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun getLocalLinkFromMegaApi(nodeHandle: Long): String?

    /**
     * Get all audio nodes
     *
     * @param order list order
     * @return audio nodes
     */
    suspend fun getAudioNodes(order: SortOrder): List<TypedAudioNode>

    /**
     * Get all video nodes
     *
     * @param order list order
     * @return video nodes
     */
    suspend fun getVideoNodes(order: SortOrder): List<TypedVideoNode>

    /**
     * Get thumbnail from MegaApiFolder
     *
     * @param nodeHandle node handle
     * @param path thumbnail path
     */
    @Throws(MegaException::class)
    suspend fun getThumbnailFromMegaApiFolder(nodeHandle: Long, path: String): Long?

    /**
     * Get thumbnail from MegaApi
     *
     * @param nodeHandle node handle
     * @param path thumbnail path
     */
    @Throws(MegaException::class)
    suspend fun getThumbnailFromMegaApi(nodeHandle: Long, path: String): Long?

    /**
     * Get audio children by parent node handle
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[TypedAudioNode]>?
     */
    suspend fun getAudioNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode>

    /**
     * Get video children by parent node handle
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[TypedVideoNode]>?
     */
    suspend fun getVideoNodesByParentHandle(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode>

    /**
     * Get audio children by parent handle from MegaApiFolder
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[TypedAudioNode]>?
     */
    suspend fun getAudiosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedAudioNode>

    /**
     * Get video children by parent handle from MegaApiFolder
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return List<[TypedVideoNode]>?
     */
    suspend fun getVideosByParentHandleFromMegaApiFolder(
        parentHandle: Long,
        order: SortOrder,
    ): List<TypedVideoNode>

    /**
     * Get audio nodes from public links
     * @param order list order
     * @return List<[TypedAudioNode]>
     */
    suspend fun getAudioNodesFromPublicLinks(order: SortOrder): List<TypedAudioNode>

    /**
     * Get video nodes from public links
     * @param order list order
     * @return List<[TypedVideoNode]>
     */
    suspend fun getVideoNodesFromPublicLinks(order: SortOrder): List<TypedVideoNode>

    /**
     * Get audio nodes from InShares
     * @param order list order
     * @return List<[TypedAudioNode]>
     */
    suspend fun getAudioNodesFromInShares(order: SortOrder): List<TypedAudioNode>

    /**
     * Get video nodes from InShares
     * @param order list order
     * @return List<[TypedVideoNode]>
     */
    suspend fun getVideoNodesFromInShares(order: SortOrder): List<TypedVideoNode>

    /**
     * Get audio nodes from OutShares
     * @param order list order
     * @return List<[TypedAudioNode]>
     */
    suspend fun getAudioNodesFromOutShares(lastHandle: Long, order: SortOrder): List<TypedAudioNode>

    /**
     * Get video nodes from OutShares
     * @param order list order
     * @return List<[TypedVideoNode]>
     */
    suspend fun getVideoNodesFromOutShares(lastHandle: Long, order: SortOrder): List<TypedVideoNode>

    /**
     * Get audio nodes by email
     *
     * @param email email of account
     * @return List<[TypedAudioNode]>?
     */
    suspend fun getAudioNodesByEmail(email: String): List<TypedAudioNode>?

    /**
     * Get video nodes by email
     *
     * @param email email of account
     * @return List<[TypedVideoNode]>?
     */
    suspend fun getVideoNodesByEmail(email: String): List<TypedVideoNode>?

    /**
     * Get username by email
     *
     * @param email email of account
     * @return username
     */
    suspend fun getUserNameByEmail(email: String): String?

    /**
     * MegaApi http server stop
     */
    suspend fun megaApiHttpServerStop()

    /**
     * MegaApiFolder http server stop
     */
    suspend fun megaApiFolderHttpServerStop()

    /**
     * MegaApi http server whether is running
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend fun megaApiHttpServerIsRunning(): Int

    /**
     * MegaApiFolder http server whether is running
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend fun megaApiFolderHttpServerIsRunning(): Int

    /**
     * MegaApi http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend fun megaApiHttpServerStart(): Boolean

    /**
     * MegaApiFolder http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend fun megaApiFolderHttpServerStart(): Boolean

    /**
     * MegaApi sets the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend fun megaApiHttpServerSetMaxBufferSize(bufferSize: Int)

    /**
     * MegaApiFolder sets the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend fun megaApiFolderHttpServerSetMaxBufferSize(bufferSize: Int)

    /**
     * Get the local folder path
     *
     * @param typedFileNode [TypedFileNode]
     * @return local file if it exists
     */
    suspend fun getLocalFilePath(typedFileNode: TypedFileNode?): String?

    /**
     * Delete the playback information
     *
     * @param mediaId the media id of deleted item
     */
    suspend fun deletePlaybackInformation(mediaId: Long)

    /**
     * Clear Playback Information
     */
    suspend fun clearPlaybackInformation()

    /**
     * Save the playback times
     */
    suspend fun savePlaybackTimes()

    /**
     * Update playback information
     *
     * @param playbackInformation the new playback information
     */
    suspend fun updatePlaybackInformation(playbackInformation: PlaybackInformation)

    /**
     * Monitor playback times
     *
     * @return Flow<Map<Long, PlaybackInformation>?>
     */
    fun monitorPlaybackTimes(): Flow<Map<Long, PlaybackInformation>?>

    /**
     * Get file url by node handle
     *
     * @param handle node handle
     * @return local link
     */
    suspend fun getFileUrlByNodeHandle(handle: Long): String?

    /**
     * Get subtitle file info list
     *
     * @param fileSuffix subtitle suffix
     * @return [SubtitleFileInfo] list
     */
    suspend fun getSubtitleFileInfoList(fileSuffix: String): List<SubtitleFileInfo>

    /**
     * Monitor the value of AudioBackgroundPlayEnabled
     *
     * @return Flow of Boolean
     */
    fun monitorAudioBackgroundPlayEnabled(): Flow<Boolean?>

    /**
     * Set the value of AudioBackgroundPlayEnabled
     *
     * @param value true is enable audio background play, otherwise is false.
     */
    suspend fun setAudioBackgroundPlayEnabled(value: Boolean)

    /**
     * Monitor the value of AudioShuffleEnabled
     *
     * @return Flow of Boolean
     */
    fun monitorAudioShuffleEnabled(): Flow<Boolean?>

    /**
     * Set the value of AudioShuffleEnabled
     *
     * @param value true is shuffled, otherwise is false.
     */
    suspend fun setAudioShuffleEnabled(value: Boolean)

    /**
     * Monitor the value of AudioRepeatMode
     *
     * @return Flow of RepeatToggleMode
     */
    fun monitorAudioRepeatMode(): Flow<RepeatToggleMode>

    /**
     * Set the value of AudioRepeatMode
     *
     * @param value Int value of audio repeat mode
     */
    suspend fun setAudioRepeatMode(value: Int)

    /**
     * Monitor the value of VideoRepeatMode
     *
     * @return Flow of RepeatToggleMode
     */
    fun monitorVideoRepeatMode(): Flow<RepeatToggleMode>

    /**
     * Set the value of VideoRepeatMode
     *
     * @param value Int value of video repeat mode
     */
    suspend fun setVideoRepeatMode(value: Int)

    /**
     * Get videos by searchType api
     *
     * @param handle handle
     * @param searchString containing a search string in their name
     * @param recursive is search recursively
     * @param order SortOrder
     * @return List<TypedVideoNode>
     */
    suspend fun getVideosBySearchType(
        handle: Long,
        searchString: String,
        recursive: Boolean,
        order: SortOrder,
    ): List<TypedVideoNode>?

    /**
     * Get audio nodes by handles
     *
     * @param handles handle list
     * @return List<[TypedAudioNode]>
     */
    suspend fun getAudioNodesByHandles(handles: List<Long>): List<TypedAudioNode>

    /**
     * Get video nodes by handles
     *
     * @param handles handle list
     * @return List<[TypedVideoNode]>
     */
    suspend fun getVideoNodesByHandles(handles: List<Long>): List<TypedVideoNode>

    /**
     * Get video node by handle
     *
     * @param handle node handle
     * @param attemptFromFolderApi whether attempt from folder api
     */
    suspend fun getVideoNodeByHandle(
        handle: Long,
        attemptFromFolderApi: Boolean = false,
    ): TypedVideoNode?

    /**
     * Get audio node by handle
     *
     * @param handle node handle
     * @param attemptFromFolderApi whether attempt from folder api
     */
    suspend fun getAudioNodeByHandle(
        handle: Long,
        attemptFromFolderApi: Boolean = false,
    ): TypedAudioNode?

    /**
     * Delete the media playback info by media handle
     *
     * @param mediaHandle the media id of deleted item
     */
    suspend fun deleteMediaPlaybackInfo(mediaHandle: Long)

    /**
     * Clear all playback infos
     */
    suspend fun clearAllPlaybackInfos()

    /**
     * Save the playback times
     */
    suspend fun saveAudioPlaybackInfo()

    /**
     * Update audio playback info
     *
     * @param info the new playback info
     */
    suspend fun updateAudioPlaybackInfo(info: MediaPlaybackInfo)

    /**
     * Get media playback info by handle
     *
     * @param handle the media handle
     * @return [MediaPlaybackInfo] if exists, otherwise null
     */
    suspend fun getMediaPlaybackInfo(handle: Long): MediaPlaybackInfo?
}
