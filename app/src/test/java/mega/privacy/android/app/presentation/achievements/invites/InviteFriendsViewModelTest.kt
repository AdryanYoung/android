package mega.privacy.android.app.presentation.achievements.invites

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.achievement.Achievement
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteFriendsViewModelTest {
    private lateinit var underTest: InviteFriendsViewModel
    private val reward100Mb = 104857600L
    private val expirationInDays = 100
    private val achievementsMock = AchievementsOverview(
        allAchievements = listOf(
            Achievement(
                reward100Mb,
                0L,
                AchievementType.MEGA_ACHIEVEMENT_INVITE,
                expirationInDays
            )
        ),
        awardedAchievements = emptyList(),
        currentStorageInBytes = 64716327836L,
        achievedStorageFromReferralsInBytes = reward100Mb,
        achievedTransferFromReferralsInBytes = reward100Mb
    )

    private val getAccountAchievementsOverviewUseCase: GetAccountAchievementsOverviewUseCase =
        mock()

    @BeforeEach
    fun resetMocks() {
        reset(getAccountAchievementsOverviewUseCase)
    }

    @Test
    fun `test that on view model init should fetch use case and update ui state correctly`() =
        runTest {
            initMocks()
            initTestClass(
                inviteFriends = InviteFriends(0L)
            )

            underTest.uiState.test {
                assertThat(awaitItem().grantStorageInBytes).isEqualTo(reward100Mb)
            }
        }

    @Test
    fun `test that getAccountAchievementsOverviewUseCase should not be called when referral storage value from saved state exists`() =
        runTest {
            initTestClass(
                inviteFriends = InviteFriends(reward100Mb)
            )

            verify(getAccountAchievementsOverviewUseCase, never()).invoke()
            underTest.uiState.test {
                assertThat(awaitItem().grantStorageInBytes).isEqualTo(reward100Mb)
            }
        }

    private fun initTestClass(
        inviteFriends: InviteFriends
    ) {
        underTest = InviteFriendsViewModel(
            getAccountAchievementsOverviewUseCase = getAccountAchievementsOverviewUseCase,
            inviteFriendsArgs = inviteFriends
        )
    }

    private suspend fun initMocks() {
        whenever(getAccountAchievementsOverviewUseCase()).doReturn(achievementsMock)
    }
}