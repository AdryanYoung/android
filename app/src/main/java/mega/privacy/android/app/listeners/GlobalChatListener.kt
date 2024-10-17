package mega.privacy.android.app.listeners

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.BroadcastChatSignalPresenceUseCase
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApi.INIT_ONLINE_SESSION
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatPresenceConfig
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber
import javax.inject.Inject

class GlobalChatListener @Inject constructor(
    private val application: Application,
    private val chatManagement: ChatManagement,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val broadcastChatSignalPresenceUseCase: BroadcastChatSignalPresenceUseCase
) : MegaChatListenerInterface {
    override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {
        if (item != null) {
            onChatListItemUpdate(item)
        }
    }

    override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {
        if (newState == INIT_ONLINE_SESSION) {
            api?.let {
                val list = it.chatListItems
                if (!list.isNullOrEmpty()) {
                    for (i in 0 until list.size) {
                        MegaApplication.getChatManagement().addCurrentGroupChat(list[i].chatId)
                    }
                }
            }
        }
    }

    override fun onChatOnlineStatusUpdate(
        api: MegaChatApiJava?,
        userhandle: Long,
        status: Int,
        inProgress: Boolean,
    ) {}

    override fun onChatPresenceConfigUpdate(
        api: MegaChatApiJava?,
        config: MegaChatPresenceConfig?,
    ) {
        if (config?.isPending == false) {
            Timber.d("Broadcast signal presence to app event")
            applicationScope.launch {
                broadcastChatSignalPresenceUseCase()
            }
        }
    }

    override fun onChatConnectionStateUpdate(api: MegaChatApiJava?, chatid: Long, newState: Int) {
    }

    override fun onChatPresenceLastGreen(api: MegaChatApiJava?, userhandle: Long, lastGreen: Int) {
    }

    override fun onDbError(api: MegaChatApiJava?, error: Int, msg: String?) {
        Timber.e("MEGAChatSDK onDBError occurred. Error $error with message $msg")
        when (error) {
            MegaChatApi.DB_ERROR_IO -> activityLifecycleHandler.getCurrentActivity()
                ?.finishAndRemoveTask()

            MegaChatApi.DB_ERROR_FULL -> post {
                activityLifecycleHandler.getCurrentActivity()?.let {
                    Util.showErrorAlertDialog(
                        application.getString(R.string.error_not_enough_free_space),
                        true, it
                    )
                }
            }
        }
    }

    private fun onChatListItemUpdate(item: MegaChatListItem) {
        if (!item.isGroup) return

        if ((item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV) && item.ownPrivilege != MegaChatRoom.PRIV_RM) || item.hasChanged(
                MegaChatListItem.CHANGE_TYPE_CALL
            )
        ) {
            chatManagement.checkActiveGroupChat(item.chatId)
        }

        if ((item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV) && item.ownPrivilege == MegaChatRoom.PRIV_RM) || item.hasChanged(
                MegaChatListItem.CHANGE_TYPE_CLOSED
            )
        ) {
            chatManagement.removeActiveChatAndNotificationShown(item.chatId)
        }
    }
}
