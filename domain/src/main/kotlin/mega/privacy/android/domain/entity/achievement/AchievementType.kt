package mega.privacy.android.domain.entity.achievement

import kotlinx.serialization.Serializable

/**
 * Achievement Type identifier
 *
 * @property classValue: Value defined in megaapi.h SDK class
 */
@Serializable
enum class AchievementType(val classValue: Int) {

    /**
     * Achievement for welcoming user after creating account
     */
    MEGA_ACHIEVEMENT_WELCOME(1),

    /**
     * Achievement when user invites friends
     */
    MEGA_ACHIEVEMENT_INVITE(3),

    /**
     * Achievement when user installs MEGASync
     */
    MEGA_ACHIEVEMENT_DESKTOP_INSTALL(4),

    /**
     * Achievement when user install Mega mobile app
     */
    MEGA_ACHIEVEMENT_MOBILE_INSTALL(5),

    /**
     * Invalid Achievement
     */
    INVALID_ACHIEVEMENT(-1)
}