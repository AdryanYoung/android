package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to get if the user should be prompted to save the download destination as default
 */
class ShouldPromptToSaveDestinationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() =
        settingsRepository.isShouldPromptToSaveDestination()
}