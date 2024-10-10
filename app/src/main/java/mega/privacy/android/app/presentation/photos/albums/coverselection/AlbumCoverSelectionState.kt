package mega.privacy.android.app.presentation.photos.albums.coverselection

import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

data class AlbumCoverSelectionState(
    val album: Album.UserAlbum? = null,
    val isInvalidAlbum: Boolean = false,
    val photos: List<Photo> = listOf(),
    val uiPhotos: List<UIPhoto> = listOf(),
    val selectedPhoto: Photo? = null,
    val hasSelectedPhoto: Boolean = false,
    val isSelectionCompleted: Boolean = false,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
)

typealias PhotoDownload = suspend (
    photo: Photo,
    callback: (success: Boolean) -> Unit,
) -> Unit
