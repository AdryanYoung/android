package mega.privacy.android.domain.usecase.chat.participants

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import javax.inject.Inject

/**
 * Default get chat participants use case implementation.
 *
 * @property chatRepository                 [mega.privacy.android.domain.repository.ChatRepository]
 * @property chatParticipantsRepository     [mega.privacy.android.domain.repository.ChatParticipantsRepository]
 * @property contactsRepository             [mega.privacy.android.domain.repository.ContactsRepository]
 * @property avatarRepository               [mega.privacy.android.domain.repository.AvatarRepository]
 * @property requestUserLastGreenUseCase        [mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase]
 * @property defaultDispatcher              [kotlinx.coroutines.CoroutineDispatcher]

 */
class MonitorChatParticipantsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val contactsRepository: ContactsRepository,
    private val avatarRepository: AvatarRepository,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(chatId: Long): Flow<List<ChatParticipant>> = flow {
        val participants = mutableListOf<ChatParticipant>().apply {
            addAll(getParticipants(chatId, true))
        }
        emit(participants)
        emit(participants.requestParticipantsInfo())
        emitAll(
            merge(
                participants.monitorChatPresenceLastGreenUpdates(),
                participants.monitorChatOnlineStatusUpdates(),
                participants.monitorChatListItemUpdates(chatId),
                participants.monitorContactUpdates(),
                participants.monitorMyAvatarUpdates(),
                participants.monitorMyNameUpdates(),
                participants.monitorMyEmailUpdates()
            )
        )
    }.flowOn(defaultDispatcher)

    private suspend fun getParticipants(
        chatId: Long,
        preloadUserAttributes: Boolean = false,
    ): List<ChatParticipant> =
        chatParticipantsRepository.getAllChatParticipants(
            chatId = chatId,
            preloadUserAttributes = preloadUserAttributes
        ).toMutableList()

    private suspend fun MutableList<ChatParticipant>.requestParticipantsInfo(): MutableList<ChatParticipant> {
        filter { !it.isMe }
        forEach { participant ->
            apply {
                val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                val currentItem = get(currentItemIndex)
                withContext(defaultDispatcher) {
                    set(
                        currentItemIndex,
                        currentItem.copy(
                            status = chatParticipantsRepository.getStatus(
                                currentItem
                            ),
                            areCredentialsVerified = chatParticipantsRepository.areCredentialsVerified(
                                currentItem
                            ),
                            defaultAvatarColor = chatParticipantsRepository.getAvatarColor(
                                currentItem
                            ),
                            data = currentItem.data.copy(
                                alias = chatParticipantsRepository.getAlias(
                                    currentItem
                                ),
                                avatarUri = chatParticipantsRepository.getAvatarUri(currentItem)
                                    ?.toString()
                            )
                        )
                    )
                }
            }
        }

        return this
    }

    private fun MutableList<ChatParticipant>.monitorChatPresenceLastGreenUpdates(): Flow<MutableList<ChatParticipant>> =
        contactsRepository.monitorChatPresenceLastGreenUpdates()
            .filter { any { participant -> participant.handle == it.handle } }
            .map { update ->
                apply {
                    val currentItemIndex = indexOfFirst { it.handle == update.handle }
                    val currentItem = get(currentItemIndex)
                    set(currentItemIndex, currentItem.copy(lastSeen = update.lastGreen))
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorChatOnlineStatusUpdates(): Flow<MutableList<ChatParticipant>> =
        contactsRepository.monitorChatOnlineStatusUpdates()
            .filter { any { participant -> participant.handle == it.userHandle } }
            .map { update ->
                apply {
                    val currentItemIndex = indexOfFirst { it.handle == update.userHandle }
                    val currentItem = get(currentItemIndex)
                    if (update.status != UserChatStatus.Online) {
                        this@MonitorChatParticipantsUseCase.requestUserLastGreenUseCase(update.userHandle)
                        set(currentItemIndex, currentItem.copy(status = update.status))
                    } else {
                        set(
                            currentItemIndex,
                            currentItem.copy(status = update.status, lastSeen = null)
                        )
                    }
                }
                this
            }

    private suspend fun MutableList<ChatParticipant>.monitorChatListItemUpdates(
        chatId: Long,
    ): Flow<MutableList<ChatParticipant>> =
        chatRepository.monitorChatListItemUpdates()
            .filter { item ->
                item.chatId == chatId &&
                        (item.changes == ChatListItemChanges.Participants || item.changes == ChatListItemChanges.LastMessage)
            }
            .map { item ->
                apply {
                    if (item.changes == ChatListItemChanges.Participants) {
                        val newList = getParticipants(chatId)
                        newList.forEach { newItem ->
                            apply {
                                val newItemIndex = indexOfFirst { it.handle == newItem.handle }
                                if (newItemIndex == -1) {
                                    apply {
                                        add(
                                            newItem.copy(
                                                status = chatParticipantsRepository.getStatus(
                                                    newItem
                                                ),
                                                areCredentialsVerified = chatParticipantsRepository.areCredentialsVerified(
                                                    newItem
                                                ),
                                                defaultAvatarColor = chatParticipantsRepository.getAvatarColor(
                                                    newItem
                                                ),
                                                data = newItem.data.copy(
                                                    alias = chatParticipantsRepository.getAlias(
                                                        newItem
                                                    ),
                                                    avatarUri = chatParticipantsRepository.getAvatarUri(
                                                        newItem
                                                    )?.toString()
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        val listOfIndexToRemove: MutableList<ChatParticipant> = mutableListOf()
                        this.forEach { currentItem ->
                            apply {
                                val currentItemIndex =
                                    newList.indexOfFirst { it.handle == currentItem.handle }
                                if (currentItemIndex == -1) {
                                    listOfIndexToRemove.add(currentItem)
                                }
                            }
                        }

                        listOfIndexToRemove.forEach { participantToRemove ->
                            apply {
                                remove(participantToRemove)
                            }
                        }

                        return@map this
                    } else if (item.changes == ChatListItemChanges.LastMessage) {
                        if (item.lastMessageType == ChatRoomLastMessage.AlterParticipants) {
                            val newList = getParticipants(chatId)
                            newList.forEach { newItem ->
                                apply {
                                    val newItemIndex = indexOfFirst { it.handle == newItem.handle }
                                    if (newItemIndex == -1) {
                                        apply {
                                            add(
                                                newItem.copy(
                                                    status = chatParticipantsRepository.getStatus(
                                                        newItem
                                                    ),
                                                    areCredentialsVerified = chatParticipantsRepository.areCredentialsVerified(
                                                        newItem
                                                    ),
                                                    defaultAvatarColor = chatParticipantsRepository.getAvatarColor(
                                                        newItem
                                                    ),
                                                    data = newItem.data.copy(
                                                        alias = chatParticipantsRepository.getAlias(
                                                            newItem
                                                        ),
                                                        avatarUri = chatParticipantsRepository.getAvatarUri(
                                                            newItem
                                                        )?.toString()
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            return@map this
                        } else if (item.lastMessageType == ChatRoomLastMessage.PrivChange) {
                            map { participant ->
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = this[currentItemIndex]

                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            privilege = chatParticipantsRepository.getPermissions(
                                                chatId,
                                                currentItem
                                            ),
                                            privilegesUpdated = !currentItem.privilegesUpdated
                                        )
                                }
                            }
                            return@map this
                        }
                    }

                }
            }

    private fun MutableList<ChatParticipant>.monitorMyAvatarUpdates(): Flow<MutableList<ChatParticipant>> =
        avatarRepository.monitorMyAvatarFile()
            .map { file ->
                apply {
                    map { participant ->
                        if (participant.isMe) {
                            val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                            val currentItem = this[currentItemIndex]
                            var avatarUri: String? = null
                            file?.let {
                                if (it.exists() && it.length() > 0) {
                                    avatarUri = it.toString()
                                }
                            }
                            this[currentItemIndex] =
                                currentItem.copy(
                                    defaultAvatarColor = avatarRepository.getMyAvatarColor(),
                                    avatarUpdateTimestamp = System.currentTimeMillis(),
                                    data = currentItem.data.copy(
                                        avatarUri = avatarUri
                                    )
                                )
                        }
                    }
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorMyNameUpdates(): Flow<MutableList<ChatParticipant>> =
        chatRepository.monitorMyName()
            .map {
                apply {
                    map { participant ->
                        if (participant.isMe) {
                            val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                            val currentItem = get(currentItemIndex)
                            set(
                                currentItemIndex,
                                currentItem.copy(
                                    data = currentItem.data.copy(
                                        fullName = contactsRepository.getUserFullName(
                                            currentItem.handle
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorMyEmailUpdates(): Flow<MutableList<ChatParticipant>> =
        chatRepository.monitorMyEmail()
            .map { update ->
                apply {
                    map { participant ->
                        if (participant.isMe) {
                            update?.let { newEmail ->
                                val currentItemIndex =
                                    indexOfFirst { it.handle == participant.handle }
                                val currentItem = get(currentItemIndex)
                                set(currentItemIndex, currentItem.copy(email = newEmail))
                            }
                        }
                    }
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorContactUpdates(): Flow<MutableList<ChatParticipant>> =
        contactsRepository.monitorContactUpdates()
            .map { update ->
                update.changes.forEach { (userId, changes) ->
                    if (changes.contains(UserChanges.Alias)) {
                        map { participant ->
                            if (!participant.isMe) {
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = this[currentItemIndex]
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            data = currentItem.data.copy(
                                                alias = chatParticipantsRepository.getAlias(
                                                    currentItem
                                                )
                                            )
                                        )
                                }
                            }
                        }
                        return@map this
                    }
                    if (changes.contains(UserChanges.AuthenticationInformation)) {
                        map { participant ->
                            if (!participant.isMe) {
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = this[currentItemIndex]
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            areCredentialsVerified = chatParticipantsRepository.areCredentialsVerified(
                                                currentItem
                                            )
                                        )
                                }
                            }
                        }
                        return@map this
                    }

                    map { participant ->
                        if (!participant.isMe && participant.handle == userId.id) {
                            apply {
                                val currentItemIndex =
                                    indexOfFirst { it.handle == participant.handle }
                                val currentItem = this[currentItemIndex]
                                if (changes.contains(UserChanges.Avatar)) {
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            defaultAvatarColor = avatarRepository.getAvatarColor(
                                                currentItem.handle
                                            ),
                                            avatarUpdateTimestamp = System.currentTimeMillis(),
                                            data = currentItem.data.copy(
                                                avatarUri = chatParticipantsRepository.getAvatarUri(
                                                    currentItem,
                                                    true
                                                )?.toString()
                                            )
                                        )
                                }

                                if (changes.contains(UserChanges.Firstname) || changes.contains(
                                        UserChanges.Lastname
                                    )
                                ) {
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            data = currentItem.data.copy(
                                                fullName = contactsRepository.getUserFullName(
                                                    currentItem.handle
                                                )
                                            )
                                        )
                                }

                                if (changes.contains(UserChanges.Email)) {
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            email = contactsRepository.getUserEmail(currentItem.handle)
                                        )
                                }
                            }
                        }
                    }
                }
                this
            }
}