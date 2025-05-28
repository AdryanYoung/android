package mega.privacy.android.app.presentation.achievements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.achievements.freetrial.megaPassFreeTrialScreen
import mega.privacy.android.app.presentation.achievements.freetrial.megaVPNFreeTrialScreen
import mega.privacy.android.app.presentation.achievements.freetrial.navigateToMegaPassFreeTrial
import mega.privacy.android.app.presentation.achievements.freetrial.navigateToMegaVPNFreeTrial
import mega.privacy.android.app.presentation.achievements.info.achievementsInfoScreen
import mega.privacy.android.app.presentation.achievements.info.navigateToAchievementsInfo
import mega.privacy.android.app.presentation.achievements.invites.inviteFriendsScreen
import mega.privacy.android.app.presentation.achievements.invites.navigateToInviteFriends
import mega.privacy.android.app.presentation.achievements.referral.navigateToReferralBonus
import mega.privacy.android.app.presentation.achievements.referral.referralBonusScreen
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Scaffold for the Achievements Flow Screen
 */
@Composable
fun AchievementsFeatureScreen(
    viewModel: AchievementsOverviewViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val navHostController = rememberNavController()

    /**
     * This event effect existed because ContactController is calling AchievementsActivity to show snackbar
     * This should be moved to AchievementsRoute once ContactController dependency is removed
     * The reason why this is put here is that it needs the activity lifecycle and also the view model instance
     * referenced in the activity, because ContactController is calling the activity method. Instead of passing
     * the view model instance to the AchievementsRoute, better to just put the effect to ease out future modification.
     */
    EventEffect(uiState.errorMessage, viewModel::resetErrorState) {
        snackbarHostState.showAutoDurationSnackbar(context.resources.getString(it))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        scaffoldState = rememberScaffoldState(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier,
                    snackbarData = data,
                    backgroundColor = MaterialTheme.colors.black_white
                )
            }
        }
    ) { padding ->
        AchievementsNavHostController(
            modifier = Modifier
                .padding(padding),
            navHostController = navHostController,
        )
    }
}

@Composable
internal fun AchievementsNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = AchievementMain
    ) {
        achievementScreen(
            onNavigateToInfoAchievements = navHostController::navigateToAchievementsInfo,
            onNavigateToInviteFriends = navHostController::navigateToInviteFriends,
            onNavigateToReferralBonuses = navHostController::navigateToReferralBonus,
            onNavigateToMegaVPNFreeTrial = navHostController::navigateToMegaVPNFreeTrial,
            onNavigateToMegaPassFreeTrial = navHostController::navigateToMegaPassFreeTrial
        )
        achievementsInfoScreen()
        referralBonusScreen()
        inviteFriendsScreen()
        megaVPNFreeTrialScreen()
        megaPassFreeTrialScreen()
    }
}
