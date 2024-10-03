package mega.privacy.android.domain.entity.node

import kotlin.time.Duration

/**
 * The typed video node entity
 *
 * @property fileNode include the all properties of default node
 * @property duration the video duration
 * @property elementID the element id if the video is belong to a playlist
 * @property isOutShared the video file's parent folder whether is out shared
 * @property watchedTimestamp the timestamp when the video is watched
 * @property collectionTitle the collection title of the video
 */
data class TypedVideoNode(
    private val fileNode: FileNode,
    val duration: Duration,
    val elementID: Long?,
    val isOutShared: Boolean,
    val watchedTimestamp: Long,
    val collectionTitle: String?
) : TypedFileNode, FileNode by fileNode
