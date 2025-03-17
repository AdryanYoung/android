package mega.privacy.android.app.presentation.achievements.referral

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.achievements.referral.view.ReferralBonusesViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.mapper.NumberOfDaysMapper
import mega.privacy.android.data.mapper.ReferralBonusAchievementsMapper
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.AwardedAchievementInvite
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReferralBonusesViewModelTest {
    private lateinit var underTest: ReferralBonusesViewModel

    private val reward100Mb = 104857600L
    private val expirationInDays = 100L
    private val name = "Qwerty Uiop"
    private val emails = listOf("qwerty@uiop.com")
    private val contact = ContactItem(
        handle = 1L,
        email = emails[0],
        contactData = ContactData(
            fullName = name, alias = "KG", avatarUri = null,
            userVisibility = UserVisibility.Unknown,
        ),
        defaultAvatarColor = null,
        visibility = UserVisibility.Visible,
        timestamp = 0,
        areCredentialsVerified = true,
        status = UserChatStatus.Online,
        chatroomId = null,
    )

    private val getContactFromEmailUseCase: GetContactFromEmailUseCase = mock {
        onBlocking { invoke(emails[0], false) }.thenReturn(
            ContactItem(
                handle = 1L,
                email = emails.get(0),
                contactData = ContactData(
                    fullName = name, alias = "KG", avatarUri = null,
                    userVisibility = UserVisibility.Unknown,
                ),
                defaultAvatarColor = null,
                visibility = UserVisibility.Visible,
                timestamp = 0,
                areCredentialsVerified = true,
                status = UserChatStatus.Online,
                chatroomId = null,
            )
        )
    }
    private val getAccountAchievementsOverview: GetAccountAchievementsOverviewUseCase = mock {
        onBlocking { invoke() }.thenReturn(
            AchievementsOverview(
                allAchievements = emptyList(),
                awardedAchievements = listOf(
                    AwardedAchievementInvite(
                        awardId = 1,
                        expirationTimestampInSeconds = expirationInDays,
                        rewardedStorageInBytes = reward100Mb,
                        rewardedTransferInBytes = reward100Mb,
                        referredEmails = emails
                    )
                ),
                currentStorageInBytes = 64716327836L,
                achievedStorageFromReferralsInBytes = reward100Mb,
                achievedTransferFromReferralsInBytes = reward100Mb
            )
        )
    }
    private val referralBonusAchievementsMapper: ReferralBonusAchievementsMapper =
        ReferralBonusAchievementsMapper(NumberOfDaysMapper(mock()))

    @BeforeAll
    fun setup() {
        underTest = ReferralBonusesViewModel(
            getContactFromEmailUseCase = getContactFromEmailUseCase,
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverview,
            referralBonusAchievementsMapper = referralBonusAchievementsMapper
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getContactFromEmailUseCase,
            getAccountAchievementsOverview
        )
    }

    @Test
    fun `test that on view model init should fetch use case and update ui state correctly`() =
        runTest {
            underTest.uiState.test {
                val achievements = awaitItem().awardedInviteAchievements[0]
                assertThat(achievements.awardId).isEqualTo(1)
                assertThat(achievements.contact).isEqualTo(contact)
                assertThat(achievements.expirationTimestampInSeconds).isEqualTo(expirationInDays)
                assertThat(achievements.rewardedStorageInBytes).isEqualTo(reward100Mb)
                assertThat(achievements.rewardedTransferInBytes).isEqualTo(reward100Mb)
                assertThat(achievements.referredEmails).isEqualTo(emails)
            }
        }
}