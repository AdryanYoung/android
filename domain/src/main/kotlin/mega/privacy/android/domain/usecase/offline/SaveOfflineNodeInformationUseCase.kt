package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import javax.inject.Inject

/**
 * Save the [OfflineNodeInformation] of this node, also all needed ancestors recursively
 */
class SaveOfflineNodeInformationUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
) {
    /**
     * invoke the use case
     * @param nodeId [NodeId] of the node
     */
    suspend operator fun invoke(nodeId: NodeId, originalName: String) {
        //we don't want to save backup parent node (Vault)
        val backupRootNodeId = monitorBackupFolder().firstOrNull()?.getOrNull() ?: NodeId(-1L)
        val driveRootNode = nodeRepository.getRootNode()?.id ?: NodeId(-1L)
        nodeRepository.getNodeById(nodeId)?.let { node ->
            //we need to save parents before the node itself
            saveNodeAndItsParentsRecursively(
                currentNode = node,
                driveRootNodeId = driveRootNode,
                backupRootNodeId = backupRootNodeId,
                originalName = originalName,
            )
        }
    }

    /**
     * Save offline information of all node's parents (not already saved) and then the node itself.
     * As offline information in the database has a reference to it's parent id, we need to save in that way
     * Root drive node and backup root parent are not saved.
     * @param currentNode the [NodeId] of the node we want to save
     * @param driveRootNodeId this node won't be saved as its children appear as root nodes in offline
     * @param backupRootNodeId this node needs to be saved, but not its parent ("Vault")
     * @param originalName The original name of the node we want to save
     */
    private suspend fun saveNodeAndItsParentsRecursively(
        currentNode: Node,
        driveRootNodeId: NodeId,
        backupRootNodeId: NodeId,
        originalName: String? = null,
    ): Long? {
        if (currentNode.id == driveRootNodeId) return null
        val offlineNodeInformation = nodeRepository.getOfflineNodeInformation(currentNode.id)
        return offlineNodeInformation?.id?.toLong() ?: nodeRepository.saveOfflineNodeInformation(
            getOfflineNodeInformationUseCase(currentNode, originalName),
            getParentOfflineInfoId(currentNode, backupRootNodeId, driveRootNodeId)
        )
    }

    private suspend fun getParentOfflineInfoId(
        node: Node,
        backupRootNodeId: NodeId,
        driveRootNodeId: NodeId,
    ): Long? {
        return if (node.id == backupRootNodeId) null else {
            nodeRepository.getNodeById(node.parentId)?.let {
                saveNodeAndItsParentsRecursively(
                    currentNode = it,
                    driveRootNodeId = driveRootNodeId,
                    backupRootNodeId = backupRootNodeId,
                )
            }
        }
    }
}
