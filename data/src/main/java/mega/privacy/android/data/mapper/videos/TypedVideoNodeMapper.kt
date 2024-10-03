package mega.privacy.android.data.mapper.videos

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class TypedVideoNodeMapper @Inject constructor() {
    operator fun invoke(
        fileNode: FileNode,
        duration: Int,
        elementID: Long? = null,
        isOutShared: Boolean = false,
        watchedTimestamp: Long = 0,
        collectionTitle: String? = null,
    ) = TypedVideoNode(
        fileNode = fileNode,
        duration = duration.seconds,
        elementID = elementID,
        isOutShared = isOutShared,
        watchedTimestamp = watchedTimestamp,
        collectionTitle = collectionTitle
    )
}