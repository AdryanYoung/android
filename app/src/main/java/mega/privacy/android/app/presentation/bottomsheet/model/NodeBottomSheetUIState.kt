package mega.privacy.android.app.presentation.bottomsheet.model

import mega.privacy.android.domain.entity.account.AccountDetail
import nz.mega.sdk.MegaNode

/**
 * Node bottom sheet ui state
 * @property canMoveNode
 * @property canRestoreNode
 * @property isOnline
 * @property isAvailableOffline
 * @property node
 * @property shareData
 * @property nodeDeviceCenterInformation
 * @property shareKeyCreated
 * @property accountDetail
 * @property isHiddenNodesOnboarded
 * @property isHidingActionAllowed
 */
data class NodeBottomSheetUIState(
    val canMoveNode: Boolean = false,
    val canRestoreNode: Boolean = false,
    val isOnline: Boolean = true,
    val isAvailableOffline: Boolean = false,
    val node: MegaNode? = null,
    val shareData: NodeShareInformation? = null,
    val nodeDeviceCenterInformation: NodeDeviceCenterInformation? = null,
    val shareKeyCreated: Boolean? = null,
    val accountDetail: AccountDetail? = null,
    val isHiddenNodesOnboarded: Boolean? = null,
    val isHidingActionAllowed: Boolean = false,
)