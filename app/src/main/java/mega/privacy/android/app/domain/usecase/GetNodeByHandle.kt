package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get the node corresponding to a handle
 */
@Deprecated("Use GetNodeInfoUseCase instead", ReplaceWith("GetNodeInfoUseCase"))
fun interface GetNodeByHandle {
    /**
     * Get the node corresponding to a handle
     *
     * @param handle
     * @return A node corresponding to the given handle, null if cannot be retrieved
     */
    suspend operator fun invoke(handle: Long): MegaNode?
}