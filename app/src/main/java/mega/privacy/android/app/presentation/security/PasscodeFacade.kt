package mega.privacy.android.app.presentation.security

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.app.utils.Util.setAppFontSize
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.security.InvalidParameterException
import javax.inject.Inject

/**
 * Passcode facade
 *
 * A component that can be injected onto any [AppCompatActivity] to enforce a Passcode
 * security check.
 *
 * All the necessary setup and implementation is handled inside this class and requires no
 * further changes to the host activity
 *
 * @property passcodeUtil
 * @property passcodeManagement
 * @property megaApi
 * @property context
 */
class PasscodeFacade @Inject constructor(
    private val passcodeUtil: PasscodeUtil,
    private val passcodeManagement: PasscodeManagement,
    @MegaApi private val megaApi: MegaApiAndroid,
    @ActivityContext private val context: Context,
) : LifecycleEventObserver, SavedStateRegistry.SavedStateProvider, PasscodeCheck {

    companion object {
        private const val SCREEN_ORIENTATION = "SCREEN_ORIENTATION"
        private const val PROVIDER = "PasscodeFacade"
    }

    private var lastStart = 0L

    /**
     * Used to control when onResume comes from a screen orientation change.
     * Since onConfigurationChanged is not implemented in all activities,
     * it cannot be used for that purpose.
     *
     * @see android.app.Activity#onConfigurationChanged(Configuration)
     */
    private var isScreenRotation = false

    /**
     * Used to disable passcode.
     * E.g.: PdfViewerActivity when it is opened from outside the app.
     */
    private var isDisabled = false

    /**
     * Used to ignore onPause when it is opening a chat message notification.
     */
    private var ignoringDueToChatNotification = false

    init {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
            ?: throw InvalidParameterException("PasscodeFacade can only be injected into LifecycleOwner")

        lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            else -> return
        }
    }

    private fun onCreate() {
        val registry = (context as? SavedStateRegistryOwner)?.savedStateRegistry ?: return
        registry.registerSavedStateProvider(PROVIDER, this)
        val state = registry.consumeRestoredStateForKey(PROVIDER)
        if (state != null &&
            context.resources.configuration.orientation != state.getInt(SCREEN_ORIENTATION)
        ) {
            isScreenRotation = true
        }
    }

    fun onPause() {
        if (isDisabled || ignoringDueToChatNotification) {
            ignoringDueToChatNotification = false
            return
        }

        passcodeUtil.pauseUpdate()
        lastStart = System.currentTimeMillis()
    }

    fun onResume() {

        ignoringDueToChatNotification = shouldIgnoreDueToChatNotification()

        if (isDisabled || ignoringDueToChatNotification) {
            return
        }

        setAppFontSize(context as Activity)

        if (isScreenRotation && !passcodeManagement.needsOpenAgain) {
            isScreenRotation = false
            passcodeUtil.resume()
        } else if (passcodeManagement.showPasscodeScreen) {
            passcodeUtil.resume()
        }
    }

    override fun saveState(): Bundle =
        bundleOf(SCREEN_ORIENTATION to context.resources.configuration.orientation)


    override fun disablePasscode() {
        isDisabled = true
    }

    /**
     * Checks if should proceed with passcode check due to a chat notification message.
     *
     * If the app is opening a chat notification message, means first the ManagerActivity
     * will be opened and then ChatActivity. That also means the passcode would be shown
     * first, and then the chat room. This will cause the user could interact with the app,
     * without having to enter the passcode. This will cause too the passcode will be shown after
     * the chat room and the app be closed. Which results in bad a behavior.
     * In summary, if all those requirements are true, the passcode should not be checked. Which
     * results in the right behaviour: If enabled and complies all requirements, the passcode
     * should be asked in the chat room screen.
     *
     * @return True if should proceed with the passcode check, false otherwise.
     */
    private fun shouldIgnoreDueToChatNotification(): Boolean =
        context is ManagerActivity
                && ACTION_CHAT_NOTIFICATION_MESSAGE == context.intent?.action
                && MEGACHAT_INVALID_HANDLE != context.intent?.getLongExtra(CHAT_ID,
            MEGACHAT_INVALID_HANDLE)
}