package mega.privacy.android.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.PushesRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default [PushesRepository] implementation.
 *
 * @property context           Required for getting shared preferences.
 * @property megaApi           Required for registering push notifications.
 * @property ioDispatcher      Required for launching coroutines.
 * @property megaChatApi       Required for notifying about pushes.
 * @property chatRequestMapper [ChatRequestMapper]
 * @property appEventGateway    Required for getting app events
 */
internal class DefaultPushesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApi: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaChatApi: MegaChatApiGateway,
    private val chatRequestMapper: ChatRequestMapper,
    private val appEventGateway: AppEventGateway,
) : PushesRepository {

    override fun getPushToken(): String =
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE)
            .getString(NEW_TOKEN, "") ?: ""

    override suspend fun registerPushNotifications(deviceType: Int, newToken: String): String =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApi.registerPushNotifications(
                    deviceType, newToken, OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestRegisterPushNotificationsCompleted(continuation)
                    )
                )
            }
        }

    private fun onRequestRegisterPushNotificationsCompleted(continuation: Continuation<String>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(request.text))
            } else {
                continuation.failWithError(error, "onRequestRegisterPushNotificationsCompleted")
            }
        }

    override fun setPushToken(newToken: String) {
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE)
            .edit {
                putString(NEW_TOKEN, newToken)
            }
    }

    override suspend fun pushReceived(beep: Boolean) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { _: MegaChatRequest, error: MegaChatError ->
                        Timber.d("PushMessageWorker onRequestPushReceivedCompleted")
                        if (error.errorCode == MegaChatError.ERROR_OK && !megaApi.isEphemeralPlusPlus) {
                            continuation.resumeWith(Result.success(Unit))
                        } else {
                            continuation.failWithError(error, "onRequestPushReceivedCompleted")
                        }
                    }
                )
                megaChatApi.pushReceived(beep, listener)
            }
        }

    override suspend fun clearPushToken() = withContext(ioDispatcher) {
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE).edit {
            clear()
        }
    }

    override suspend fun broadcastPushNotificationSettings() = withContext(ioDispatcher) {
        appEventGateway.broadcastPushNotificationSettings()
    }

    override fun monitorPushNotificationSettings() =
        appEventGateway.monitorPushNotificationSettings()

    companion object {
        private const val PUSH_TOKEN = "PUSH_TOKEN"
        private const val NEW_TOKEN = "NEW_TOKEN"
    }
}