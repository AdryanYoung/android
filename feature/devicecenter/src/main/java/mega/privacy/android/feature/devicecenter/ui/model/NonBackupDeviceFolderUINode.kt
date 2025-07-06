package mega.privacy.android.feature.devicecenter.ui.model

import kotlinx.serialization.Serializable
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A UI data class representing a normal Device Folder
 *
 * One Scenario that this Folder can be created is when the User enables Camera Uploads and uploads
 * content
 *
 * @property id The Folder ID
 * @property name The Folder Name
 * @property icon The Folder Icon
 * @property status The Folder Status
 * @property rootHandle The Folder Root Handle
 * @property localFolderPath The Folder Local Path
 */
@Serializable
data class NonBackupDeviceFolderUINode(
    override val id: String,
    override val name: String,
    override val icon: DeviceCenterUINodeIcon,
    override val status: DeviceCenterUINodeStatus,
    override val rootHandle: Long,
    val localFolderPath: String
) : DeviceFolderUINode
