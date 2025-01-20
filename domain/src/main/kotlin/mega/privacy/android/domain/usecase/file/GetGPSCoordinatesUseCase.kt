package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get GPS coordinates use case
 *
 * @property fileSystemRepository
 * @constructor Create empty Get GPS coordinates use case
 */
class GetGPSCoordinatesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @param filePath Local path of the file.
     * @param isVideo True if is a video, false if is an image.
     * @return GPS coordinates.
     */
    suspend operator fun invoke(uriPath: UriPath, isVideo: Boolean) =
        if (isVideo) {
            fileSystemRepository.getVideoGPSCoordinates(uriPath)
        } else {
            fileSystemRepository.getPhotoGPSCoordinates(uriPath)
        }
}
