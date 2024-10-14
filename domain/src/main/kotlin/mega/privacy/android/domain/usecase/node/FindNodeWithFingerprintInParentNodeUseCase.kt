package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Find the node by fingerprint or original fingerprint and check if it exists in the current parent folder
 * or not (except the rubbish bin folder)
 */
class FindNodeWithFingerprintInParentNodeUseCase @Inject constructor(
    private val getNodeFromCloudDriveUseCase: GetNodeFromCloudDriveUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
) {

    /**
     * Find the node by fingerprint or original fingerprint and check if it exists in the current parent folder
     * or not (except the rubbish bin folder)
     *
     * @param fingerprint the fingerprint of the original file
     * @param generatedFingerprint the computed fingerprint of the temporary file
     * @param parentNodeId parent node id to look for the node
     * @return a Pair<Boolean?, NodeId?>
     *         The first element will return true if it exists in the parent folder given in parameter, false otherwise, null if in rubbish bin
     *         The second element will return the node retrieved, null if cannot be retrieved
     */
    suspend operator fun invoke(
        fingerprint: String,
        generatedFingerprint: String?,
        parentNodeId: NodeId,
    ): Pair<Boolean?, NodeId?> {

        val cloudDriveNode = getNodeFromCloudDriveUseCase(
            fingerprint,
            generatedFingerprint,
            parentNodeId,
        )

        return cloudDriveNode?.let { node ->
            val isNodeInRubbishBin = isNodeInRubbishBinUseCase(node.id)
            val isNodeInParentFolder =
                node.parentId.longValue == parentNodeId.longValue

            when {
                isNodeInParentFolder -> Pair(true, node.id)
                !isNodeInRubbishBin && !isNodeInParentFolder -> Pair(false, node.id)
                else -> Pair(null, node.id)
            }
        } ?: run {
            Pair(false, null)
        }
    }
}

