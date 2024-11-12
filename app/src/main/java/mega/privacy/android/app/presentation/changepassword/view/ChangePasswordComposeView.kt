package mega.privacy.android.app.presentation.changepassword.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.extensions.toStrengthAttribute
import mega.privacy.android.app.presentation.changepassword.model.ChangePasswordUIState
import mega.privacy.android.app.presentation.changepassword.model.PasswordStrengthAttribute
import mega.privacy.android.app.presentation.changepassword.view.Constants.CHANGE_PASSWORD_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.CONFIRM_PASSWORD_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.DISABLED_BUTTON_ALPHA
import mega.privacy.android.app.presentation.changepassword.view.Constants.ENABLED_BUTTON_ALPHA
import mega.privacy.android.app.presentation.changepassword.view.Constants.LOADING_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.PASSWORD_STRENGTH_BAR_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.PASSWORD_STRENGTH_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.PASSWORD_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.SNACKBAR_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.TNC_CHECKBOX_TEST_TAG
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.legacy.core.ui.controls.dialogs.LoadingDialog
import mega.privacy.android.legacy.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.controls.textfields.PasswordTextField
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import nz.mega.sdk.MegaApiJava

internal object Constants {
    const val DISABLED_BUTTON_ALPHA = 0.5f
    const val ENABLED_BUTTON_ALPHA = 1f

    /**
     * Test tag for message SnackBar
     */
    const val SNACKBAR_TEST_TAG = "snackbar_test_tag"

    /**
     * Test tag for loading
     */
    const val LOADING_DIALOG_TEST_TAG = "loading_test_tag"

    /**
     * Test tag for password text field
     */
    const val PASSWORD_TEST_TAG = "password_test_tag"

    /**
     * Test tag for confirm password text field
     */
    const val CONFIRM_PASSWORD_TEST_TAG = "c_password_test_tag"

    /**
     * Test tag for terms and condition checkbox
     */
    const val TNC_CHECKBOX_TEST_TAG = "tnc_cb_test_tag"

    /**
     * Test tag for password strength
     */
    const val PASSWORD_STRENGTH_TEST_TAG = "password_strength_test_tag"

    /**
     * Test tag for password strength bar
     */
    const val PASSWORD_STRENGTH_BAR_TEST_TAG = "password_strength_group_test_tag"

    /**
     * Test tag for password strength
     */
    const val SEE_PASSWORD_TEST_TAG = "see_password_test_tag"

    /**
     * Test tag for change password button
     */
    const val CHANGE_PASSWORD_BUTTON_TEST_TAG = "cp_button_test_tag"
}

/**
 * Main Compose View for Change Password & Reset Password Screen
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChangePasswordView(
    uiState: ChangePasswordUIState,
    onSnackBarShown: () -> Unit,
    onPasswordTextChanged: (String) -> Unit,
    onConfirmPasswordTextChanged: () -> Unit,
    onTnCLinkClickListener: () -> Unit,
    onTriggerChangePassword: (String) -> Unit,
    onTriggerResetPassword: (String) -> Unit,
    onValidatePassword: (String) -> Unit,
    onValidateOnSave: (String, String) -> Unit,
    onResetValidationState: () -> Unit,
    onAfterPasswordChanged: () -> Unit,
    onAfterPasswordReset: (isLoggedIn: Boolean, errorCode: Int?) -> Unit,
    onPromptedMultiFactorAuth: (String) -> Unit,
    onFinishActivity: () -> Unit,
    onShowAlert: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }
    val strengthAttribute = uiState.passwordStrength.toStrengthAttribute()
    val title =
        if (uiState.isResetPasswordMode) R.string.title_enter_new_password else R.string.my_account_change_password
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        scaffoldState = rememberScaffoldState(),
        topBar = {
            SimpleTopAppBar(
                titleId = title,
                elevation = scrollState.value > 0,
                onBackPressed = {
                    onBackPressedDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                Snackbar(
                    modifier = Modifier.testTag(SNACKBAR_TEST_TAG),
                    snackbarData = data,
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white
                )
            }
        }
    ) { padding ->
        val context = LocalContext.current
        var passwordText: String? by remember { mutableStateOf(null) }
        var confirmPasswordText: String? by remember { mutableStateOf(null) }
        var isTnCChecked by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val keyboardController = LocalSoftwareKeyboardController.current
        val noConnectionMessage = stringResource(id = R.string.error_server_connection_problem)
        val notCheckedMessage = stringResource(id = R.string.create_account_no_top)
        val onConfirmPasswordChange: () -> Unit = {
            keyboardController?.hide()

            when {
                uiState.isConnectedToNetwork.not() -> {
                    coroutineScope.launch {
                        snackBarHostState.showAutoDurationSnackbar(noConnectionMessage)
                    }
                }

                isTnCChecked.not() -> {
                    coroutineScope.launch {
                        snackBarHostState.showAutoDurationSnackbar(notCheckedMessage)
                    }
                }

                else -> {
                    onValidateOnSave(passwordText.orEmpty(), confirmPasswordText.orEmpty())
                }
            }
        }

        LaunchedEffect(uiState.snackBarMessage) {
            if (uiState.snackBarMessage != null) {
                snackBarHostState.showAutoDurationSnackbar(context.resources.getString(uiState.snackBarMessage))
                onSnackBarShown()
            }
        }

        LaunchedEffect(uiState.isShowAlertMessage) {
            if (uiState.isShowAlertMessage) {
                onShowAlert()
            }
        }

        LaunchedEffect(uiState.isPromptedMultiFactorAuth) {
            if (uiState.isPromptedMultiFactorAuth) {
                onPromptedMultiFactorAuth(passwordText.orEmpty())
            }
        }

        LaunchedEffect(uiState.isResetPasswordLinkValid) {
            if (uiState.isResetPasswordLinkValid.not()) {
                onFinishActivity()
            }
        }

        LaunchedEffect(uiState.isPasswordChanged) {
            if (uiState.isPasswordChanged) {
                onAfterPasswordChanged()
            }
        }

        LaunchedEffect(uiState.isPasswordReset) {
            if (uiState.isPasswordReset) {
                onAfterPasswordReset(uiState.isUserLoggedIn, uiState.errorCode)
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(start = 24.dp, end = 26.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val passwordLabel =
                if (uiState.isCurrentPassword) R.string.error_same_password else R.string.my_account_change_password_newPassword1
            val confirmPasswordFocusRequester = remember { FocusRequester() }

            PasswordTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 17.dp)
                    .testTag(PASSWORD_TEST_TAG),
                hint = stringResource(id = passwordLabel),
                onTextChange = { value ->
                    passwordText = value
                    onPasswordTextChanged(value)
                },
                text = passwordText.orEmpty(),
                errorText = if (passwordText != null && (uiState.isCurrentPassword || uiState.passwordError != null)) {
                    uiState.passwordError?.let { res ->
                        stringResource(id = res)
                    }.takeIf {
                        uiState.isCurrentPassword.not()
                    }
                } else null,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusRequester.requestFocus() }),
                onFocusChanged = { hasFocus ->
                    if (hasFocus.not() && uiState.isResetPasswordMode.not()) {
                        onValidatePassword(passwordText.orEmpty())
                    }
                }
            )

            if (uiState.passwordStrength > PasswordStrength.INVALID && (uiState.passwordError == null || uiState.isCurrentPassword)) {
                PasswordStrengthBar(strengthAttribute = strengthAttribute)
            }

            PasswordTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .testTag(CONFIRM_PASSWORD_TEST_TAG)
                    .focusRequester(confirmPasswordFocusRequester),
                hint = stringResource(id = R.string.my_account_change_password_newPassword2),
                onTextChange = { value ->
                    confirmPasswordText = value
                    onConfirmPasswordTextChanged()
                },
                text = confirmPasswordText.orEmpty(),
                imeAction = ImeAction.Done,
                errorText = uiState.confirmPasswordError?.let { res -> stringResource(id = res) }
                    .takeIf { uiState.confirmPasswordError != null },
                keyboardActions = KeyboardActions(onNext = { onConfirmPasswordChange() }),
            )

            TnCCheckboxDescription(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .testTag(TNC_CHECKBOX_TEST_TAG),
                onLinkClickListener = onTnCLinkClickListener,
                onCheckChanged = { isTnCChecked = it }
            )

            ChangePasswordActionButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 31.dp, bottom = 16.dp),
                isEnabled = uiState.isCurrentPassword.not(),
                isResetPasswordMode = uiState.isResetPasswordMode,
                isPasswordValidated = uiState.isSaveValidationSuccessful,
                passwordText = passwordText.orEmpty(),
                onTriggerResetPassword = onTriggerResetPassword,
                onTriggerChangePassword = onTriggerChangePassword,
                onAfterPasswordChanged = onResetValidationState,
                onFinishActivity = onFinishActivity,
                onConfirmButtonClick = onConfirmPasswordChange
            )

            uiState.loadingMessage?.let { res ->
                LoadingDialog(
                    modifier = Modifier.testTag(LOADING_DIALOG_TEST_TAG),
                    title = stringResource(id = res),
                    text = stringResource(id = res)
                )
            }
        }
    }
}

/**
 * Password Strength Bar for PasswordTextField
 */
@Composable
fun PasswordStrengthBar(strengthAttribute: PasswordStrengthAttribute) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize()
            .padding(top = 8.dp)
            .testTag(PASSWORD_STRENGTH_TEST_TAG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (0..MegaApiJava.PASSWORD_STRENGTH_STRONG).forEach { column ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .weight(1f, true)
                        .padding(horizontal = 5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .conditional(condition = column > strengthAttribute.strength) {
                            border(
                                1.dp,
                                MaterialTheme.colors.grey_alpha_012_white_alpha_038,
                                RoundedCornerShape(2.dp)
                            )
                        }
                        .conditional(condition = column <= strengthAttribute.strength) {
                            background(strengthAttribute.color)
                        }
                        .testTag(PASSWORD_STRENGTH_BAR_TEST_TAG)
                )
            }
        }

        Text(
            modifier = Modifier
                .padding(top = 8.dp, start = 5.dp),
            text = stringResource(id = strengthAttribute.description),
            fontSize = 14.sp,
            color = strengthAttribute.color
        )

        Text(
            modifier = Modifier
                .padding(top = 6.dp, start = 5.dp),
            text = stringResource(id = strengthAttribute.advice),
            style = MaterialTheme.typography.caption,
            letterSpacing = 0.sp,
            color = MaterialTheme.colors.textColorPrimary,
        )
    }
}


/**
 * Terms and Condition checkbox and description
 */
@Composable
fun TnCCheckboxDescription(
    modifier: Modifier,
    onLinkClickListener: () -> Unit,
    onCheckChanged: (Boolean) -> Unit,
) {
    var isChecked by remember { mutableStateOf(false) }

    Row(modifier = modifier) {
        Checkbox(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onCheckChanged(isChecked)
            }
        )
        MegaSpannedText(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable(onClick = onLinkClickListener),
            value = stringResource(id = R.string.top),
            baseStyle = MaterialTheme.typography.caption.copy(lineHeight = 1.5.em),
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(textDecoration = TextDecoration.Underline),
                SpanIndicator('B') to SpanStyle(color = MaterialTheme.colors.secondary)
            )
        )
    }
}

/**
 * Button Action Group to Cancel and Save
 */
@Composable
fun ChangePasswordActionButtonGroup(
    modifier: Modifier,
    passwordText: String,
    onTriggerChangePassword: (String) -> Unit,
    onTriggerResetPassword: (String) -> Unit,
    onAfterPasswordChanged: () -> Unit,
    onFinishActivity: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    isEnabled: Boolean = true,
    isResetPasswordMode: Boolean = false,
    isPasswordValidated: Boolean = false,
) {
    val buttonAlpha = if (isEnabled) ENABLED_BUTTON_ALPHA else DISABLED_BUTTON_ALPHA

    LaunchedEffect(isPasswordValidated) {
        if (isPasswordValidated.not()) return@LaunchedEffect

        if (isResetPasswordMode) {
            onTriggerResetPassword(passwordText)
        } else {
            onTriggerChangePassword(passwordText)
        }

        onAfterPasswordChanged()
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.End) {
        TextButton(
            modifier = Modifier
                .wrapContentWidth()
                .defaultMinSize(10.dp)
                .padding(end = 16.dp),
            onClick = { onFinishActivity() },
            content = {
                Text(
                    text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                    color = MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.button,
                )
            }
        )

        Button(
            modifier = Modifier
                .wrapContentWidth()
                .defaultMinSize(10.dp)
                .testTag(CHANGE_PASSWORD_BUTTON_TEST_TAG),
            enabled = isEnabled,
            onClick = onConfirmButtonClick,
            content = {
                Text(
                    text = stringResource(id = R.string.my_account_change_password),
                    style = MaterialTheme.typography.button,
                )
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary.copy(buttonAlpha),
                contentColor = MaterialTheme.colors.surface
            )
        )
    }
}