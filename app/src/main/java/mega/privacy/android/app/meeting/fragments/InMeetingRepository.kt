package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.AddContactListener
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatVideoListenerInterface
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMeetingRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context,
) {

    /**
     * Get a call from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatCall
     */
    fun getMeeting(chatId: Long): MegaChatCall? =
        if (chatId == MEGACHAT_INVALID_HANDLE) null else megaChatApi.getChatCall(chatId)


    /**
     * Method to know if it's me
     *
     * @param peerId The handle
     * @return True, if it's me. False, otherwise
     */
    fun isMe(peerId: Long?): Boolean {
        return peerId == megaApi.myUserHandleBinary
    }

    /**
     * Get a chat from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatRoom
     */
    fun getChatRoom(chatId: Long): MegaChatRoom? =
        if (chatId == MEGACHAT_INVALID_HANDLE) null else megaChatApi.getChatRoom(chatId)

    /**
     * Get contact name
     *
     * @param peerId contact handle
     * @return String The contact's name
     */
    fun getContactOneToOneCallName(peerId: Long): String {
        val name: String =
            ChatController(MegaApplication.getInstance().applicationContext).getParticipantFirstName(
                peerId
            )
        if (TextUtil.isTextEmpty(name)) {
            return megaChatApi.getContactEmail(peerId)
        }

        return name
    }

    /**
     * Method for creating a meeting
     *
     * @param meetingName the name of the meeting
     * @param listener MegaChatRequestListenerInterface
     */
    fun createMeeting(meetingName: String, listener: MegaChatRequestListenerInterface) =
        megaChatApi.createMeeting(meetingName, false, false, true, listener)

    /**
     * Method for getting a participant's email
     *
     * @param peerId user handle of participant
     * @param listener MegaRequestListenerInterface
     * @return the email of the participant
     */
    fun getEmailParticipant(peerId: Long, listener: MegaRequestListenerInterface): String? {
        if (isMe(peerId))
            return megaChatApi.myEmail

        val email = megaChatApi.getUserEmailFromCache(peerId)

        if (email != null)
            return email

        megaApi.getUserEmail(peerId, listener)
        return null
    }

    /**
     * Get the avatar
     *
     * @param peerId user Handle of a participant
     * @return the avatar the avatar of a participant
     */
    fun getAvatarBitmap(peerId: Long, getRemoteAvatar: () -> Unit): Bitmap? {
        var avatar = CallUtil.getImageAvatarCall(peerId)
        if (avatar == null) {
            getRemoteAvatar()

            avatar = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }

        return avatar
    }

    /**
     * Get my name as participant
     *
     * @return My name
     */
    fun getMyName(): String {
        return megaChatApi.myFullname
    }

    /**
     * Method to know if a user is my contact
     *
     * @param peerId user handle of a participant
     * @return True, if it's. False, otherwise.
     */
    fun isMyContact(peerId: Long): Boolean {
        val email = ChatController(context).getParticipantEmail(peerId)
        val contact = megaApi.getContact(email)

        return contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE
    }

    /**
     * Method to get the participant's name
     *
     * @param peerId user handle of a participant
     * @return The name of a participant
     */
    fun participantName(peerId: Long): String? =
        if (peerId == MEGACHAT_INVALID_HANDLE) null
        else ChatController(context).getParticipantFullName(peerId)

    /**
     * Method of obtaining the remote video
     *
     * @param chatId chat ID
     * @param clientId client ID of a participant
     * @param hiRes If it's has High resolution
     * @param listener MegaChatVideoListenerInterface
     */
    fun addChatRemoteVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface,
    ) {
        if (hiRes) {
            Timber.d("Add Chat remote video listener of client $clientId , with HiRes")
        } else {
            Timber.d("Add Chat remote video listener of client $clientId , with LowRes")
        }

        megaChatApi.addChatRemoteVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of remove the remote video
     *
     * @param chatId chat ID
     * @param clientId client ID of a participant
     * @param hiRes If it's has High resolution
     * @param listener MegaChatVideoListenerInterface
     */
    fun removeChatRemoteVideoListener(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MegaChatVideoListenerInterface,
    ) {
        if (hiRes) {
            Timber.d("Remove Chat remote video listener of client $clientId, with HiRes")
        } else {
            Timber.d("Remove Chat remote video listener of client $clientId, with LowRes")
        }

        megaChatApi.removeChatVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method to get own privileges in a chat
     *
     * @param chatId chat ID
     * @return my privileges
     */
    fun getOwnPrivileges(chatId: Long): Int {
        getChatRoom(chatId)?.let {
            return it.ownPrivilege
        }

        return -1
    }

    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface) =
        megaChatApi.openChatPreview(link, listener)

    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface) {
        if (!MegaApplication.getChatManagement().isAlreadyJoining(chatId)) {
            Timber.d("Joining to public chat with ID $chatId")
            MegaApplication.getChatManagement().addJoiningChatId(chatId)
            megaChatApi.autojoinPublicChat(chatId, listener)
        }
    }

    fun rejoinPublicChat(
        chatId: Long,
        publicChatHandle: Long,
        listener: MegaChatRequestListenerInterface,
    ) {
        Timber.d("Rejoining to public chat with ID $chatId")
        megaChatApi.autorejoinPublicChat(chatId, publicChatHandle, listener)
    }

    fun getMyFullName(): String {
        val name = megaChatApi.myFullname
        if (name != null)
            return name

        return megaChatApi.myEmail
    }

    fun getMyInfo(
        moderator: Boolean,
        audio: Boolean,
        video: Boolean,
        getRemoteAvatar: () -> Unit,
    ): Participant {
        return Participant(
            megaApi.myUserHandleBinary,
            MEGACHAT_INVALID_HANDLE,
            megaChatApi.myFullname ?: "",
            getAvatarBitmapByPeerId(megaApi.myUserHandleBinary, getRemoteAvatar),
            true,
            moderator,
            audio,
            video,
            isAudioDetected = false,
            isGuest = megaApi.isEphemeralPlusPlus
        )
    }

    /**
     * Method for getting a participant's avatar
     *
     * @param peerId user handle of participant
     */
    fun getAvatarBitmapByPeerId(peerId: Long, getRemoteAvatar: () -> Unit): Bitmap? {
        var bitmap: Bitmap?
        val mail = ChatController(context).getParticipantEmail(peerId)

        val userHandleString = MegaApiAndroid.userHandleToBase64(peerId)
        val myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaApi.myUserHandleBinary)
        bitmap = if (userHandleString == myUserHandleEncoded) {
            AvatarUtil.getAvatarBitmap(mail)
        } else {
            if (TextUtil.isTextEmpty(mail)) AvatarUtil.getAvatarBitmap(userHandleString) else AvatarUtil.getUserAvatar(
                userHandleString,
                mail
            )
        }

        if (bitmap == null) {
            getRemoteAvatar()
            bitmap = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }

        return bitmap
    }

    /**
     * Method for getting a participant's email
     *
     * @param peerId user handle of participant
     */
    fun getParticipantEmail(peerId: Long): String? =
        ChatController(context).getParticipantEmail(peerId)

    /**
     * Determine if I am a guest
     *
     * @return True, if my account is an ephemeral account. False otherwise
     */
    fun amIAGuest(): Boolean = megaApi.isEphemeralPlusPlus

    /**
     * Send add contact invitation
     *
     * @param context the Context
     * @param peerId the peerId of users
     * @param callback the callback for sending add contact request
     */
    fun addContact(context: Context, peerId: Long, callback: (String) -> Unit) {
        megaApi.inviteContact(
            ChatController(context).getParticipantEmail(peerId),
            null,
            MegaContactRequest.INVITE_ACTION_ADD,
            AddContactListener(callback, context)
        )
    }
}
