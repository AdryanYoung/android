package mega.privacy.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class for video recently watched item
 *
 * @property videoHandle the video handle
 * @property watchedTimestamp the timestamp when the video is watched
 * @property collectionId the collection id of the video
 * @property collectionTitle the collection title of the video
 */
@Serializable
data class VideoRecentlyWatchedItem(
    @SerialName("videoHandle") val videoHandle: Long,
    @SerialName("watchedTimestamp") val watchedTimestamp: Long,
    @SerialName("collectionId") val collectionId: Long = 0,
    @SerialName("collectionTitle") val collectionTitle: String? = null
)