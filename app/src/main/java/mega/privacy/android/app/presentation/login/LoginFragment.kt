package mega.privacy.android.app.presentation.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getChatManagement
import mega.privacy.android.app.MegaApplication.Companion.isIsHeartBeatAlive
import mega.privacy.android.app.MegaApplication.Companion.setHeartBeatAlive
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.canBeHandled
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.LoginViewModel.Companion.ACTION_FORCE_RELOAD_ACCOUNT
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.LoginView
import mega.privacy.android.app.presentation.login.view.NewLoginView
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.setStartScreenTimeStamp
import mega.privacy.android.app.presentation.weakaccountprotection.WeakAccountProtectionAlertActivity
import mega.privacy.android.app.providers.FileProviderActivity
import mega.privacy.android.app.upgradeAccount.ChooseAccountActivity
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.LAUNCH_INTENT
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL_EMAIL
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.support.SupportEmailTicket
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.LoginScreenEvent
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Login fragment.
 *
 * @property getThemeMode [MonitorThemeModeUseCase]
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    @Inject
    @LoginMutex
    lateinit var loginMutex: Mutex

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    private val viewModel: LoginViewModel by activityViewModels()

    private val billingViewModel by activityViewModels<BillingViewModel>()

    private var insertMKDialog: AlertDialog? = null
    private var confirmLogoutDialog: AlertDialog? = null

    private var intentExtras: Bundle? = null
    private var intentData: Uri? = null
    private var intentAction: String? = null
    private var intentDataString: String? = null
    private var intentParentHandle: Long = -1
    private var intentShareInfo: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { LoginScreen() }
    }

    override fun onDestroy() {
        confirmLogoutDialog?.dismiss()
        super.onDestroy()
    }

    @Composable
    private fun LoginScreen() {
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        EventEffect(uiState.checkRecoveryKeyEvent, viewModel::onCheckRecoveryKeyEventConsumed) {
            if (it.isSuccess) {
                val data = it.getOrThrow()
                navigateToChangePassword(link = data.link, value = data.recoveryKey)
            } else {
                val e = it.exceptionOrNull()
                Timber.e(e)
                val code = (e as? MegaException)?.errorCode ?: Int.MIN_VALUE
                handleRkError(code)
            }
        }

        EventEffect(uiState.onBackPressedEvent, viewModel::consumedOnBackPressedEvent) {
            onBackPressed(uiState)
        }

        LaunchedEffect(uiState.isLoginRequired) {
            if (uiState.isLoginRequired) {
                confirmLogoutDialog?.dismiss()
            }
        }

        LaunchedEffect(uiState.ongoingTransfersExist) {
            if (uiState.ongoingTransfersExist == true) {
                showCancelTransfersDialog()
            }
        }

        LaunchedEffect(uiState.intentState) {
            uiState.intentState?.let {
                when (it) {
                    LoginIntentState.ReadyForInitialSetup -> {
                        Timber.d("Ready to initial setup")
                        finishSetupIntent(uiState)
                    }

                    LoginIntentState.ReadyForFinalSetup -> {
                        Timber.d("Ready to finish")
                        readyToFinish(uiState)
                    }

                    else -> {
                        /* Nothing to update */
                        Timber.d("Intent state: $this")
                    }
                }
            }
        }

        if (uiState.isLoginNewDesignEnabled == true) {
            AndroidTheme(isDark = uiState.themeMode.isDarkMode()) {
                LaunchedEffect(Unit) {
                    Analytics.tracker.trackEvent(LoginScreenEvent)
                }
                NewLoginView(
                    state = uiState,
                    onEmailChanged = viewModel::onEmailChanged,
                    onPasswordChanged = viewModel::onPasswordChanged,
                    onLoginClicked = {
                        LoginActivity.isBackFromLoginPage = false
                        viewModel.onLoginClicked(false)
                        billingViewModel.loadSkus()
                        billingViewModel.loadPurchases()
                    },
                    onForgotPassword = { onForgotPassword(uiState.accountSession?.email) },
                    onCreateAccount = ::onCreateAccount,
                    onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
                    on2FAChanged = viewModel::on2FAChanged,
                    onLostAuthenticatorDevice = ::onLostAuthenticationDevice,
                    onBackPressed = { onBackPressed(uiState) },
                    onReportIssue = ::openLoginIssueHelpdeskPage,
                    onLoginExceptionConsumed = viewModel::setLoginErrorConsumed,
                    onResetAccountBlockedEvent = viewModel::resetAccountBlockedEvent,
                    onResendVerificationEmail = viewModel::resendVerificationEmail,
                    onResetResendVerificationEmailEvent = viewModel::resetResendVerificationEmailEvent,
                    stopLogin = viewModel::stopLogin,
                )
            }
        } else if (uiState.isLoginNewDesignEnabled == false) {
            EventEffect(
                event = uiState.accountBlockedEvent,
                onConsumed = viewModel::resetAccountBlockedEvent
            ) {
                showBlockedDialogLegacy(it)
            }
            OriginalTheme(isDark = uiState.themeMode.isDarkMode()) {
                LoginView(
                    state = uiState,
                    onEmailChanged = viewModel::onEmailChanged,
                    onPasswordChanged = viewModel::onPasswordChanged,
                    onLoginClicked = {
                        LoginActivity.isBackFromLoginPage = false
                        viewModel.onLoginClicked(false)
                        billingViewModel.loadSkus()
                        billingViewModel.loadPurchases()
                    },
                    onForgotPassword = { onForgotPassword(uiState.accountSession?.email) },
                    onCreateAccount = ::onCreateAccount,
                    onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
                    on2FAPinChanged = viewModel::on2FAPinChanged,
                    on2FAChanged = viewModel::on2FAChanged,
                    onLostAuthenticatorDevice = ::onLostAuthenticationDevice,
                    onBackPressed = { onBackPressed(uiState) },
                    onFirstTime2FAConsumed = viewModel::onFirstTime2FAConsumed,
                    onReportIssue = ::openLoginIssueHelpdeskPage,
                )
            }
        }

        // Hide splash after UI is rendered, to prevent blinking
        LaunchedEffect(key1 = Unit) {
            delay(100)
            (activity as? LoginActivity)?.stopShowingSplashScreen()
        }
    }

    private fun openLoginIssueHelpdeskPage() {
        context.launchUrl(LOGIN_HELP_URL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupIntent()
    }

    /**
     * Gets data from the intent and performs the corresponding action if necessary.
     */
    @SuppressLint("NewApi")
    private fun setupIntent() = (requireActivity() as LoginActivity).intent?.let { intent ->
        intentAction = intent.action

        intentAction?.let { action ->
            Timber.d("action is: %s", action)
            when (action) {
                Constants.ACTION_CONFIRM -> {
                    handleConfirmationIntent(intent)
                    return
                }

                Constants.ACTION_RESET_PASS -> {
                    val link = intent.dataString
                    val isLoggedIn = intent.getBooleanExtra(LoginActivity.EXTRA_IS_LOGGED_IN, false)
                    if (link != null && !isLoggedIn) {
                        Timber.d("Link to resetPass: %s", link)
                        showDialogInsertMKToChangePass(link)
                        viewModel.intentSet()
                    }
                    return
                }

                Constants.ACTION_PASS_CHANGED -> {
                    when (val code = intent.getIntExtra(Constants.RESULT, MegaError.API_OK)) {
                        MegaError.API_OK -> viewModel.setSnackbarMessageId(R.string.pass_changed_alert)
                        else -> handleRkError(code)
                    }
                    viewModel.intentSet()
                    return
                }

                Constants.ACTION_SHOW_WARNING_ACCOUNT_BLOCKED -> {
                    val accountBlockedString =
                        intent.getStringExtra(Constants.ACCOUNT_BLOCKED_STRING)
                    val accountBlockedType: AccountBlockedType? =
                        intent.serializable(Constants.ACCOUNT_BLOCKED_TYPE)

                    if (accountBlockedString != null && accountBlockedType != null && !TextUtil.isTextEmpty(
                            accountBlockedString
                        )
                    ) {
                        viewModel.triggerAccountBlockedEvent(
                            AccountBlockedDetail(
                                accountBlockedType,
                                accountBlockedString
                            )
                        )
                    }
                }

                ACTION_FORCE_RELOAD_ACCOUNT -> {
                    viewModel.setForceReloadAccountAsPendingAction()
                    return
                }
            }
        } ?: Timber.w("ACTION NULL")
    } ?: Timber.w("No INTENT")

    private fun finishSetupIntent(uiState: LoginState) {
        (requireActivity() as LoginActivity).intent?.apply {
            if (uiState.isAlreadyLoggedIn && !LoginActivity.isBackFromLoginPage) {
                Timber.d("Credentials NOT null")

                intentAction?.let { action ->
                    when (action) {
                        Constants.ACTION_REFRESH -> {
                            viewModel.fetchNodes(true)
                            return@apply
                        }

                        Constants.ACTION_REFRESH_API_SERVER -> {
                            intentParentHandle = getLongExtra("PARENT_HANDLE", -1)
                            startFastLogin()
                            return@apply
                        }

                        Constants.ACTION_REFRESH_AFTER_BLOCKED -> {
                            startFastLogin()
                            return@apply
                        }

                        else -> {
                            Timber.d("intent received $action")
                            when (action) {
                                Constants.ACTION_LOCATE_DOWNLOADED_FILE -> {
                                    intentExtras = extras
                                }

                                Constants.ACTION_SHOW_WARNING -> {
                                    intentExtras = extras
                                }

                                Constants.ACTION_EXPLORE_ZIP -> {
                                    intentExtras = extras
                                }

                                Constants.ACTION_OPEN_MEGA_FOLDER_LINK,
                                Constants.ACTION_IMPORT_LINK_FETCH_NODES,
                                Constants.ACTION_CHANGE_MAIL,
                                Constants.ACTION_CANCEL_ACCOUNT,
                                Constants.ACTION_OPEN_HANDLE_NODE,
                                Constants.ACTION_OPEN_CHAT_LINK,
                                Constants.ACTION_JOIN_OPEN_CHAT_LINK,
                                Constants.ACTION_RESET_PASS,
                                    -> {
                                    intentDataString = dataString
                                }

                                Constants.ACTION_FILE_PROVIDER -> {
                                    intentData = data
                                    intentExtras = extras
                                    intentDataString = null
                                }

                                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL,
                                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL,
                                    -> {
                                    intentData = data
                                }
                            }

                            if (uiState.rootNodesExists) {
                                var newIntent =
                                    Intent(requireContext(), ManagerActivity::class.java)

                                when (action) {
                                    Constants.ACTION_FILE_PROVIDER -> {
                                        newIntent =
                                            Intent(
                                                requireContext(),
                                                FileProviderActivity::class.java
                                            )
                                        intentExtras?.let { newIntent.putExtras(it) }
                                        newIntent.data = intentData
                                    }

                                    Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                        newIntent = getFileLinkIntent()
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        intentAction = Constants.ACTION_OPEN_MEGA_LINK
                                        newIntent.data = intentData
                                    }

                                    Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                        newIntent = getFolderLinkIntent()
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                        newIntent.data = intentData
                                    }

                                    Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        intentAction = Constants.ACTION_OPEN_CONTACTS_SECTION
                                        if (newIntent.getLongExtra(
                                                Constants.CONTACT_HANDLE,
                                                -1
                                            ) != -1L
                                        ) {
                                            newIntent.putExtra(
                                                Constants.CONTACT_HANDLE,
                                                newIntent.getLongExtra(Constants.CONTACT_HANDLE, -1)
                                            )
                                        }
                                    }
                                }

                                newIntent.action = intentAction

                                intentDataString?.let { newIntent.data = Uri.parse(it) }
                                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                                startActivity(newIntent)
                                requireActivity().finish()
                            } else {
                                startFastLogin()
                            }

                            return@apply
                        }
                    }
                }

                if (uiState.rootNodesExists && uiState.fetchNodesUpdate == null && !isIsHeartBeatAlive) {
                    Timber.d("rootNode != null")

                    var newIntent = Intent(requireContext(), ManagerActivity::class.java)

                    intentAction?.let { action ->
                        when (action) {
                            Constants.ACTION_FILE_PROVIDER -> {
                                newIntent =
                                    Intent(requireContext(), FileProviderActivity::class.java)
                                intentExtras?.let { newIntent.putExtras(it) }
                                newIntent.data = intentData
                            }

                            Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                newIntent = getFileLinkIntent()
                                newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                intentAction = Constants.ACTION_OPEN_MEGA_LINK
                                newIntent.data = intentData
                            }

                            Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                newIntent = getFolderLinkIntent()
                                newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                newIntent.data = intentData
                            }

                            Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                                if (getLongExtra(Constants.CONTACT_HANDLE, -1) != -1L) {
                                    newIntent.putExtra(
                                        Constants.CONTACT_HANDLE,
                                        getLongExtra(Constants.CONTACT_HANDLE, -1)
                                    )
                                }
                            }
                        }

                        newIntent.action = action
                        intentDataString?.let { newIntent.data = Uri.parse(it) }
                    }

                    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                    startActivity(newIntent)
                    (requireActivity() as LoginActivity).finish()
                } else {
                    Timber.d("rootNode is null or heart beat is alive -> do fast login")
                    setHeartBeatAlive(false)
                    startFastLogin()
                }

                return@apply
            }

            Timber.d("Credentials IS NULL")
            Timber.d("INTENT NOT NULL")

            intentAction?.let { action ->
                Timber.d("ACTION NOT NULL")
                val newIntent: Intent
                when (action) {
                    Constants.ACTION_FILE_PROVIDER -> {
                        newIntent = Intent(requireContext(), FileProviderActivity::class.java)
                        intentExtras?.let { newIntent.putExtras(it) }
                        newIntent.data = intentData
                        newIntent.action = action
                    }

                    Constants.ACTION_FILE_EXPLORER_UPLOAD -> {
                        viewModel.setSnackbarMessageId(R.string.login_before_share)
                    }

                    Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                        intentDataString = dataString
                    }
                }
            }
        }

        viewModel.intentSet()
    }

    private fun showBlockedDialogLegacy(accountBlockedDetail: AccountBlockedDetail) {
        if (accountBlockedDetail.type == AccountBlockedType.VERIFICATION_EMAIL) {
            if (!MegaApplication.isBlockedDueToWeakAccount && !MegaApplication.isWebOpenDueToEmailVerification) {
                startActivity(
                    Intent(
                        activity,
                        WeakAccountProtectionAlertActivity::class.java
                    )
                )
            }
        } else if (!TextUtil.isTextEmpty(accountBlockedDetail.text)) {
            Util.showErrorAlertDialog(accountBlockedDetail.text, false, activity)
        }
    }

    /**
     * Handles intent from confirmation email.
     *
     * @param intent Intent.
     */
    private fun handleConfirmationIntent(intent: Intent) {
        if (!viewModel.isConnected) {
            viewModel.setSnackbarMessageId(R.string.error_server_connection_problem)
            return
        }

        Timber.d("querySignupLink")
        intent.getStringExtra(Constants.EXTRA_CONFIRMATION)?.let { viewModel.checkSignupLink(it) }
    }

    private fun startFastLogin() {
        Timber.d("startFastLogin")
        viewModel.fastLogin(requireActivity().intent?.action == Constants.ACTION_REFRESH_API_SERVER)
    }

    /**
     * Checks pending actions and setups the final intent for launch before finish.
     */
    private fun readyToFinish(uiState: LoginState) {
        (requireActivity() as LoginActivity).intent?.apply {
            Timber.d("Intent not null")

            @Suppress("UNCHECKED_CAST")
            intentShareInfo = getBooleanExtra(FileExplorerActivity.EXTRA_FROM_SHARE, false)

            when {
                intentShareInfo -> {
                    Timber.d("Intent to share")
                    toSharePage()
                    return
                }

                Constants.ACTION_FILE_EXPLORER_UPLOAD == action && Constants.TYPE_TEXT_PLAIN == type -> {
                    Timber.d("Intent to FileExplorerActivity")
                    startActivity(
                        Intent(
                            requireContext(),
                            FileExplorerActivity::class.java
                        ).putExtra(
                            Intent.EXTRA_TEXT,
                            getStringExtra(Intent.EXTRA_TEXT)
                        )
                            .putExtra(
                                Intent.EXTRA_SUBJECT,
                                getStringExtra(Intent.EXTRA_SUBJECT)
                            )
                            .putExtra(
                                Intent.EXTRA_EMAIL,
                                getStringExtra(Intent.EXTRA_EMAIL)
                            )
                            .setAction(Intent.ACTION_SEND)
                            .setType(Constants.TYPE_TEXT_PLAIN)
                    )
                    requireActivity().finish()
                    return
                }

                Constants.ACTION_REFRESH == action && activity != null -> {
                    Timber.d("Intent to refresh")
                    requireActivity().apply {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    return
                }
            }
        }

        confirmLogoutDialog?.dismiss()
        val loginActivity = requireActivity() as LoginActivity
        val isLoggedInToConfirmedAccount =
            !loginActivity.intent.getStringExtra(Constants.EXTRA_CONFIRMATION).isNullOrEmpty()
                    && uiState.isAccountConfirmed
                    && uiState.accountSession?.email == uiState.temporalEmail
        if (!isLoggedInToConfirmedAccount) {
            if (getChatManagement().isPendingJoinLink()) {
                LoginActivity.isBackFromLoginPage = false
                getChatManagement().pendingJoinLink = null
            }
            Timber.d("confirmLink==null")
            Timber.d("OK fetch nodes")

            if (intentAction != null && intentDataString != null) {
                Timber.d("Intent action: $intentAction")

                when (intentAction) {
                    Constants.ACTION_CHANGE_MAIL -> {
                        Timber.d("Action change mail after fetch nodes")
                        val changeMailIntent = Intent(requireContext(), ManagerActivity::class.java)
                        changeMailIntent.action = Constants.ACTION_CHANGE_MAIL
                        changeMailIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(changeMailIntent)
                        loginActivity.finish()
                    }

                    Constants.ACTION_RESET_PASS -> {
                        Timber.d("Action reset pass after fetch nodes")
                        val resetPassIntent = Intent(requireContext(), ManagerActivity::class.java)
                        resetPassIntent.action = Constants.ACTION_RESET_PASS
                        resetPassIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(resetPassIntent)
                        loginActivity.finish()
                    }

                    Constants.ACTION_CANCEL_ACCOUNT -> {
                        Timber.d("Action cancel Account after fetch nodes")
                        val cancelAccountIntent =
                            Intent(requireContext(), ManagerActivity::class.java)
                        cancelAccountIntent.action = Constants.ACTION_CANCEL_ACCOUNT
                        cancelAccountIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(cancelAccountIntent)
                        loginActivity.finish()
                    }
                }
            }
            if (!uiState.pressedBackWhileLogin) {
                Timber.d("NOT backWhileLogin")

                if (intentParentHandle != -1L) {
                    Timber.d("Activity result OK")
                    val intent = Intent()
                    intent.putExtra("PARENT_HANDLE", intentParentHandle)
                    loginActivity.setResult(Activity.RESULT_OK, intent)
                    loginActivity.finish()
                } else {
                    lifecycleScope.launch {
                        var intent: Intent?
                        val refreshActivityIntent =
                            requireActivity().intent.parcelable<Intent>(LAUNCH_INTENT)
                        if (uiState.isAlreadyLoggedIn) {
                            Timber.d("isAlreadyLoggedIn")
                            intent = Intent(requireContext(), ManagerActivity::class.java)
                            setStartScreenTimeStamp(requireContext())
                            when (intentAction) {
                                Constants.ACTION_EXPORT_MASTER_KEY -> {
                                    Timber.d("ACTION_EXPORT_MK")
                                    intent.action = Constants.ACTION_EXPORT_MASTER_KEY
                                }

                                Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                                    if (intentDataString != null) {
                                        intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                                        intent.data = Uri.parse(intentDataString)
                                    }
                                }

                                else -> intent =
                                    refreshActivityIntent ?: handleLinkNavigation(loginActivity)
                            }
                            if (uiState.isFirstTime) {
                                Timber.d("First time")
                                intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                            }
                        } else {
                            var initialCam = false
                            if (uiState.hasPreferences) {
                                if (!uiState.hasCUSetting) {
                                    with(requireActivity()) {
                                        setStartScreenTimeStamp(this)

                                        Timber.d("First login")
                                        startActivity(
                                            Intent(
                                                this,
                                                ManagerActivity::class.java
                                            ).apply {
                                                putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                                            })

                                        finish()
                                    }
                                    initialCam = true
                                }
                            } else {
                                intent = Intent(requireContext(), ManagerActivity::class.java)
                                intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                                initialCam = true
                                setStartScreenTimeStamp(requireContext())
                            }
                            if (!initialCam) {
                                Timber.d("NOT initialCam")
                                intent = handleLinkNavigation(loginActivity)
                            } else {
                                Timber.d("initialCam YES")
                                intent = Intent(requireContext(), ManagerActivity::class.java)
                                Timber.d("The action is: %s", intentAction)
                                intent.action = intentAction
                                intentDataString?.let { intent.data = Uri.parse(it) }
                            }
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        if (intentAction == Constants.ACTION_REFRESH_API_SERVER
                            || intentAction == Constants.ACTION_REFRESH_AFTER_BLOCKED
                        ) {
                            intent.action = intentAction
                        }

                        if (viewModel.getStorageState() === StorageState.PayWall) {
                            Timber.d("show Paywall warning")
                            showOverDiskQuotaPaywallWarning(activity, true)
                        } else {
                            Timber.d("First launch")
                            val shouldShowNotificationPermission =
                                viewModel.shouldShowNotificationPermission()

                            intent.apply {
                                putExtra(
                                    IntentConstants.EXTRA_FIRST_LAUNCH,
                                    uiState.isFirstTimeLaunch
                                )
                                if (shouldShowNotificationPermission) {
                                    Timber.d("LoginFragment::shouldShowNotificationPermission")
                                    putExtra(
                                        IntentConstants.EXTRA_ASK_PERMISSIONS,
                                        true
                                    )
                                    putExtra(
                                        IntentConstants.EXTRA_SHOW_NOTIFICATION_PERMISSION,
                                        true
                                    )
                                }
                            }

                            // we show upgrade account for all accounts that are free and logged in for the first time
                            if (uiState.shouldShowUpgradeAccount) {
                                startActivity(
                                    intent.setClass(
                                        requireContext(),
                                        ChooseAccountActivity::class.java
                                    ).apply {
                                        putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, false)
                                        putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, false)
                                    }
                                )
                            } else {
                                startActivity(intent)
                            }
                        }
                        Timber.d("LoginActivity finish")
                        loginActivity.finish()
                    }
                }
            }
        } else {
            Timber.d("Go to ChooseAccountFragment")
            viewModel.updateIsAccountConfirmed(false)
            if (getChatManagement().isPendingJoinLink()) {
                LoginActivity.isBackFromLoginPage = false
                val intent = Intent(requireContext(), ManagerActivity::class.java)
                intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                intent.data = Uri.parse(getChatManagement().pendingJoinLink)
                startActivity(intent)
                getChatManagement().pendingJoinLink = null
                loginActivity.finish()
            } else if (uiState.isAlreadyLoggedIn) {
                startActivity(Intent(loginActivity, ChooseAccountActivity::class.java))
                loginActivity.finish()
            }
        }
    }

    private fun handleLinkNavigation(loginActivity: LoginActivity): Intent {
        var intent = Intent(requireContext(), ManagerActivity::class.java)
        if (intentAction != null) {
            Timber.d("The action is: %s", intentAction)
            when (intentAction) {
                Constants.ACTION_FILE_PROVIDER -> {
                    intent = Intent(requireContext(), FileProviderActivity::class.java)
                    intentExtras?.let { intent.putExtras(it) }
                    intent.data = intentData
                }

                Constants.ACTION_LOCATE_DOWNLOADED_FILE -> {
                    intentExtras?.let { intent.putExtras(it) }
                }

                Constants.ACTION_SHOW_WARNING -> {
                    intentExtras?.let { intent.putExtras(it) }
                }

                Constants.ACTION_EXPLORE_ZIP -> {
                    intentExtras?.let { intent.putExtras(it) }
                }

                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                    intent = getFileLinkIntent()
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.data = intentData
                }

                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                    intent = getFolderLinkIntent()
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                    intent.data = intentData
                }

                Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                    intent.putExtra(
                        Constants.CONTACT_HANDLE,
                        loginActivity.intent?.getLongExtra(Constants.CONTACT_HANDLE, -1),
                    )
                }
            }
            intent.action = intentAction
            intentDataString?.let { intent.data = Uri.parse(it) }
        } else {
            Timber.w("The intent action is NULL")
        }
        return intent
    }

    /**
     * Launches an intent to [FileExplorerActivity]
     */
    private fun toSharePage() = with(requireActivity()) {
        startActivity(
            this.intent.setClass(requireContext(), FileExplorerActivity::class.java)
        )
        finish()
    }

    /**
     * Shows a dialog for changing password.
     *
     * @param link Reset password link.
     */
    private fun showDialogInsertMKToChangePass(link: String) {
        Timber.d("link: %s", link)
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            Util.scaleWidthPx(20, resources.displayMetrics),
            Util.scaleHeightPx(20, resources.displayMetrics),
            Util.scaleWidthPx(17, resources.displayMetrics),
            0
        )
        val input = EditText(requireContext())
        layout.addView(input, params)
        input.setSingleLine()
        input.hint = getString(R.string.edit_text_insert_mk)
        input.setTextColor(getThemeColor(requireContext(), android.R.attr.textColorSecondary))
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onSubmitRK(input, link)
            } else {
                Timber.d("Other IME%s", actionId)
            }
            false
        }
        input.setImeActionLabel(getString(R.string.general_add), EditorInfo.IME_ACTION_DONE)
        val builder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setTitle(getString(R.string.title_dialog_insert_MK))
            .setMessage(getString(R.string.text_dialog_insert_MK))
            .setPositiveButton(getString(R.string.general_ok), null)
            .setNegativeButton(getString(sharedR.string.general_dialog_cancel_button), null)
            .setView(layout)
            .setOnDismissListener {
                Util.hideKeyboard(requireActivity(), InputMethodManager.HIDE_NOT_ALWAYS)
            }
        insertMKDialog = builder.create().apply {
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                onSubmitRK(input, link)
            }
        }
    }

    private fun onSubmitRK(input: EditText, link: String) {
        Timber.d("OK BUTTON PASSWORD")
        val value = input.text.toString().trim { it <= ' ' }
        if (value == "" || value.isEmpty()) {
            Timber.w("Input is empty")
            input.error = getString(R.string.invalid_string)
            input.requestFocus()
        } else {
            Timber.d("Positive button pressed - reset pass")
            viewModel.checkRecoveryKey(link, value)
            insertMKDialog?.dismiss()
        }
    }

    private fun navigateToChangePassword(link: String, value: String) {
        val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
        intent.action = Constants.ACTION_RESET_PASS_FROM_LINK
        intent.data = link.toUri()
        intent.putExtra(IntentConstants.EXTRA_MASTER_KEY, value)
        startActivity(intent)
    }

    private fun handleRkError(code: Int) {
        when (code) {
            MegaError.API_EKEY -> showAlertIncorrectRK()
            MegaError.API_EBLOCKED -> viewModel.setSnackbarMessageId(R.string.error_reset_account_blocked)
            else -> viewModel.setSnackbarMessageId(R.string.general_text_error)
        }
    }

    /**
     * Shows a confirmation dialog before cancelling the current in progress login.
     */
    private fun showConfirmLogoutDialog() {
        confirmLogoutDialog = MaterialAlertDialogBuilder(requireContext())
            .setCancelable(true)
            .setMessage(getString(R.string.confirm_cancel_login))
            .setPositiveButton(getString(R.string.general_positive_button)) { _, _ -> viewModel.stopLogin() }
            .setNegativeButton(getString(R.string.general_negative_button), null)
            .show()
    }

    /**
     * Performs on back pressed.
     *
     * # Disable back press when:
     * - Refreshing
     * # Minimize app when:
     * - Login is in progress
     * - Nodes are being fetched
     * # Show logout confirmation when:
     * - 2FA is required
     */
    fun onBackPressed(uiState: LoginState) {
        Timber.d("onBackPressed")
        with(uiState) {
            when {
                Constants.ACTION_REFRESH == intentAction || Constants.ACTION_REFRESH_API_SERVER == intentAction ->
                    return

                is2FARequired || multiFactorAuthState != null -> {
                    if (isLoginNewDesignEnabled == true) {
                        viewModel.stopLogin()
                    } else {
                        showConfirmLogoutDialog()
                    }
                }

                loginMutex.isLocked || isLoginInProgress || isFastLoginInProgress || fetchNodesUpdate != null ->
                    activity?.moveTaskToBack(true)

                else -> {
                    LoginActivity.isBackFromLoginPage = true
                    (requireActivity() as LoginActivity).showFragment(LoginFragmentType.Tour)
                }
            }
        }
    }

    /**
     * Shows the cancel transfers dialog.
     */
    private fun showCancelTransfersDialog() = AlertDialog.Builder(requireContext()).apply {
        setMessage(R.string.login_warning_abort_transfers)
        setPositiveButton(sharedR.string.login_text) { _, _ -> viewModel.onLoginClicked(true) }
        setNegativeButton(sharedR.string.general_dialog_cancel_button) { _, _ -> viewModel.resetOngoingTransfers() }
        setCancelable(false)
        show()
    }

    /**
     * Shows a warning informing the Recovery Key is not correct.
     */
    private fun showAlertIncorrectRK() {
        Timber.d("showAlertIncorrectRK")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.incorrect_MK_title))
            .setMessage(getString(R.string.incorrect_MK))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.general_ok), null)
            .show()
    }

    private fun getFolderLinkIntent(): Intent {
        return Intent(requireContext(), FolderLinkComposeActivity::class.java)
    }

    private fun getFileLinkIntent(): Intent {
        return Intent(requireContext(), FileLinkComposeActivity::class.java)
    }

    private fun onForgotPassword(typedEmail: String?) {
        Timber.d("Click on button_forgot_pass")
        context.launchUrl(
            if (typedEmail.isNullOrEmpty()) {
                RECOVERY_URL
            } else {
                RECOVERY_URL_EMAIL + Base64.encodeToString(typedEmail.toByteArray(), Base64.DEFAULT)
                    .replace("\n", "")
            }
        )
    }

    private fun onCreateAccount() {
        (requireActivity() as LoginActivity).showFragment(LoginFragmentType.CreateAccount)
    }

    private fun onLostAuthenticationDevice() {
        context.launchUrl(RECOVERY_URL)
    }

    private fun sendSupportEmail(ticket: SupportEmailTicket) {
        val fileUri = getLogFileUri(ticket)
        val emailIntent = getEmailIntent(ticket, fileUri)
        if (emailIntent.canBeHandled(requireContext())) {
            startActivity(emailIntent)
        }
    }

    private fun getLogFileUri(
        ticket: SupportEmailTicket,
    ) = ticket.logs?.let { file ->
        context?.let {
            FileProvider.getUriForFile(
                it,
                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                file
            )
        }
    }

    private fun getEmailIntent(
        ticket: SupportEmailTicket,
        fileUri: Uri?,
    ) = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(ticket.email))
        putExtra(Intent.EXTRA_SUBJECT, ticket.subject)
        putExtra(Intent.EXTRA_TEXT, ticket.ticket)
        fileUri?.let<Uri, Unit> {
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    companion object {
        private const val LOGIN_HELP_URL = "https://help.mega.io/accounts/login-issues"
    }
}
