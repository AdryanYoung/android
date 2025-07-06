package mega.privacy.android.feature.devicecenter.ui.model.icon

import kotlinx.serialization.Serializable
import mega.privacy.android.icon.pack.R

/**
 * A sealed UI interface that represents different Device Icons
 */
@Serializable
sealed interface DeviceIconType : DeviceCenterUINodeIcon {

    /**
     * Represents an Android Device Icon
     */
    @Serializable
    data object Android : DeviceIconType {
        override val iconRes = R.drawable.ic_android_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents an iOS Device Icon
     */
    @Serializable
    data object IOS : DeviceIconType {
        override val iconRes = R.drawable.ic_ios_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Linux Device Icon
     */
    @Serializable
    data object Linux : DeviceIconType {
        override val iconRes = R.drawable.ic_pc_linux_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Mac Device Icon
     */
    @Serializable
    data object Mac : DeviceIconType {
        override val iconRes = R.drawable.ic_pc_mac_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Mobile Device Icon
     */
    @Serializable
    data object Mobile : DeviceIconType {
        override val iconRes = R.drawable.ic_mobile_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a PC Device Icon
     */
    @Serializable
    data object PC : DeviceIconType {
        override val iconRes = R.drawable.ic_pc_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }

    /**
     * Represents a Windows Device Icon
     */
    @Serializable
    data object Windows : DeviceIconType {
        override val iconRes = R.drawable.ic_pc_windows_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = true
    }
}