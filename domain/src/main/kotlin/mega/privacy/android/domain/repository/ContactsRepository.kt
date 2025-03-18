package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserLastGreen
import mega.privacy.android.domain.entity.user.UserUpdate

/**
 * Contacts repository.
 */
interface ContactsRepository {

    /**
     * Monitor contact request updates.
     *
     * @return A flow of all global contact request updates.
     */
    fun monitorContactRequestUpdates(): Flow<List<ContactRequest>>

    /**
     * Monitor updates on last green.
     *
     * @return A flow of [UserLastGreen]
     */
    fun monitorChatPresenceLastGreenUpdates(): Flow<UserLastGreen>

    /**
     * Requests last green of a user.
     *
     * @param userHandle User handle.
     */
    suspend fun requestLastGreen(userHandle: Long)

    /**
     * Monitor contact updates
     *
     * @return A flow of all global contact updates.
     */
    fun monitorContactUpdates(): Flow<UserUpdate>

    /**
     * Monitor updates on chat online statuses.
     *
     * @return A flow of [OnlineStatus].
     */
    fun monitorChatOnlineStatusUpdates(): Flow<OnlineStatus>

    /**
     * Monitor updates on my chat online status.
     *
     * @return A flow of [OnlineStatus].
     */
    fun monitorMyChatOnlineStatusUpdates(): Flow<OnlineStatus>

    /**
     * Monitor updates on chat connection state.
     *
     * @return A flow of [ChatConnectionState].
     */
    fun monitorChatConnectionStateUpdates(): Flow<ChatConnectionState>

    /**
     * Gets visible contacts with the cached data, not the updated one.
     *
     * @return A list with all visible contacts.
     */
    suspend fun getVisibleContacts(): List<ContactItem>

    /**
     * Gets all contacts name(including unknown, blocked etc) with the cached data, not the updated one.
     *
     * @return A map of contact emails and names.
     */
    suspend fun getAllContactsName(): Map<String, String?>

    /**
     * Gets the updated main data of a contact.
     *
     * @param contactItem [ContactItem] whose data is going to be requested.
     * @return [ContactData] containing the updated data.
     */
    suspend fun getContactData(contactItem: ContactItem): ContactData

    /**
     * Gets the ContactItem for a given [UserId]
     * @param userId of the user we want to fetch
     * @param skipCache if true a new fetch will be done, if false it may return a cached info
     */
    suspend fun getContactItem(userId: UserId, skipCache: Boolean): ContactItem?

    /**
     * Updates the contact list with the received contact updates.
     *
     * @param outdatedContactList Outdated contact list.
     * @param contactUpdates      Map with all contact updates.
     * @return The updated contact list.
     */
    suspend fun applyContactUpdates(
        outdatedContactList: List<ContactItem>,
        contactUpdates: UserUpdate,
    ): List<ContactItem>

    /**
     * Updates the contact list with the new contact.
     *
     * @param outdatedContactList Outdated contact list.
     * @param newContacts         List with new contacts.
     * @return The updated contact list.
     */
    suspend fun addNewContacts(
        outdatedContactList: List<ContactItem>,
        newContacts: List<ContactRequest>,
    ): List<ContactItem>

    /**
     * Get the alias of the given user if any
     *
     * @param handle User identifier.
     * @return User alias.
     */
    suspend fun getUserAlias(handle: Long): String

    /**
     * Get the email of the given user.
     *
     * @param handle    User identifier.
     * @param skipCache Skip cached result.
     * @return User email
     */
    suspend fun getUserEmail(handle: Long, skipCache: Boolean = false): String

    /**
     * Gets the name of the given user: The alias if any, the full name in other case.
     * If not available then returns the email.
     *
     * @param handle User identifier.
     * @param skipCache Skip cached result.
     * @param shouldNotify
     * @return User first name.
     */
    suspend fun getUserFirstName(
        handle: Long,
        skipCache: Boolean = false,
        shouldNotify: Boolean = false,
    ): String

    /**
     * Gets the name of the given user: The alias if any, the full name in other case.
     * If not available then returns the email.
     *
     * @param handle User identifier.
     * @param skipCache Skip cached result.
     * @param shouldNotify
     * @return User last name.
     */
    suspend fun getUserLastName(
        handle: Long,
        skipCache: Boolean = false,
        shouldNotify: Boolean = false,
    ): String

    /**
     * Get the full name of the given user.
     *
     * @param handle User identifier
     * @param skipCache Skip cached result.
     * @return User full name.
     */
    suspend fun getUserFullName(handle: Long, skipCache: Boolean = false): String

    /**
     * Checks if the credentials of a given user are already verified.
     *
     * @param userEmail The contact's email.
     * @return True if credentials are verified, false otherwise.
     */
    suspend fun areCredentialsVerified(userEmail: String): Boolean

    /**
     * Resets the credentials of the given user.
     *
     * @param userEmail The contact's email.
     */
    suspend fun resetCredentials(userEmail: String)

    /**
     * Verifies the credentials of the given user.
     *
     * @param userEmail The contact's email.
     */
    suspend fun verifyCredentials(userEmail: String)

    /**
     * Gets contact's credentials.
     *
     * @param userEmail User's email.
     * @return [AccountCredentials.ContactCredentials]
     */
    suspend fun getContactCredentials(userEmail: String): AccountCredentials.ContactCredentials?

    /**
     * Get current user first name
     *
     * @param forceRefresh If true, force read from backend, refresh cache and return.
     *                     If false, use value in cache
     * @return first name
     */
    suspend fun getCurrentUserFirstName(forceRefresh: Boolean): String

    /**
     * Get current user last name
     *
     * @param forceRefresh If true, force read from backend, refresh cache and return.
     *                     If false, use value in cache
     * @return last name
     */
    suspend fun getCurrentUserLastName(forceRefresh: Boolean): String

    /**
     * Update user first name
     *
     * @param value new user first name
     * @return
     */
    suspend fun updateCurrentUserFirstName(value: String): String

    /**
     * Update user last name
     *
     * @param value new user last name
     * @return
     */
    suspend fun updateCurrentUserLastName(value: String): String

    /**
     * Invite a new contact
     *
     * @param email Email of the new contact
     * @param handle Handle of the contact
     * @param message Message for the user (can be NULL)
     */
    suspend fun inviteContact(email: String, handle: Long, message: String?): InviteContactRequest

    /**
     * Get aliases
     * @return the map of key is user handle and value is user nick name
     */
    suspend fun getCurrentUserAliases(): Map<Long, String>

    /**
     * Get contact emails
     *
     * @return the map of key is handle and value is email
     */
    suspend fun getContactEmails(): Map<Long, String>

    /**
     * Clear contact database
     *
     */
    suspend fun clearContactDatabase()

    /**
     * Create or update contact
     *
     * @param handle
     * @param email
     * @param firstName
     * @param lastName
     * @param nickname
     */
    suspend fun createOrUpdateContact(
        handle: Long,
        email: String,
        firstName: String,
        lastName: String,
        nickname: String?,
    )

    /**
     * Get contact database size
     *
     * @return the number of record in database
     */
    suspend fun getContactDatabaseSize(): Int

    /**
     * Get contact email and save to local database
     *
     * @param handle user handle id
     */
    suspend fun getContactEmail(handle: Long): String

    /**
     * Get contact email and save to local database
     *
     * @param handle user handle id
     */
    suspend fun getUserOnlineStatusByHandle(handle: Long): UserChatStatus

    /**
     * Get user email from chat id
     *
     * @param handle user handle
     */
    suspend fun getUserEmailFromChat(handle: Long): String?

    /**
     * Get contact item from user email
     *
     * @param email user email
     * @param skipCache If true, force read from backend, refresh cache and return.
     *                  If false, use value in cache
     * @return [ContactItem]
     */
    suspend fun getContactItemFromUserEmail(email: String, skipCache: Boolean): ContactItem?

    /**
     * Set user alias
     *
     * @param name new nick name
     * @param userHandle user handle
     */
    suspend fun setUserAlias(name: String?, userHandle: Long): String?

    /**
     * Get the alias of the given user if any
     *
     * @param email User email.
     * @return User alias.
     */
    suspend fun getAvatarUri(email: String): String?

    /**
     * Deletes Avatar file from cache if exists
     *
     * @param email email id of the user
     */
    suspend fun deleteAvatar(email: String)

    /**
     * Remove the selected contact from mega account
     *
     * @param email email address of the user
     */
    suspend fun removeContact(email: String): Boolean

    /**
     * Get contact handle by email
     *
     * @param email
     * @return
     */
    suspend fun getContactHandleByEmail(email: String): Long

    /**
     * Get incoming contact requests
     * @return list of [ContactRequest]
     */
    suspend fun getIncomingContactRequests(): List<ContactRequest>

    /**
     * Manage a received contact request
     *
     * @param requestHandle         contact request identifier
     * @param contactRequestAction  contact request action
     */
    suspend fun manageReceivedContactRequest(
        requestHandle: Long,
        contactRequestAction: ContactRequestAction,
    )

    /**
     * Manage a sent contact request
     *
     * @param requestHandle         contact request identifier
     * @param contactRequestAction  contact request action
     */
    suspend fun manageSentContactRequest(
        requestHandle: Long,
        contactRequestAction: ContactRequestAction,
    )

    /**
     * Get contact link
     *
     * @param userHandle
     * @return
     */
    suspend fun getContactLink(userHandle: Long): ContactLink

    /**
     * Is contact request sent
     *
     * @param email
     */
    suspend fun isContactRequestSent(email: String): Boolean

    /**
     * Has any contact
     *
     * @return True if has any contact, false otherwise.
     */
    suspend fun hasAnyContact(): Boolean

    /**
     * Monitor whether a contact has removed me or I removed a contact.
     *
     * @return List of user ids of the removed contacts.
     */
    fun monitorContactRemoved(): Flow<List<Long>>

    /**
     * Monitor whether a contact has accepted my invitation or I accepted a contact invitation.
     *
     * @return List of user ids of the new contacts.
     */
    fun monitorNewContacts(): Flow<List<Long>>

    /**
     * Retrieves the contact's username from the database
     *
     * @param user The user, which can be potentially nullable
     * @return The username from the database, which can be potentially nullable
     */
    suspend fun getContactUserNameFromDatabase(user: String?): String?

    /**
     * Get [User] by [UserId]
     *
     * @param userId [UserId].
     * @return [User] object.
     *
     */
    suspend fun getUser(userId: UserId): User?

    /**
     * Monitor contact cache updates.
     *
     * @return A flow of [UserUpdate].
     */
    val monitorContactCacheUpdates: Flow<UserUpdate>

    /**
     * Get list of local contacts from the ContactGateway
     *
     * @return List of [LocalContact]
     */
    suspend fun getLocalContacts(): List<LocalContact>

    /**
     * Get list of local contact's numbers from the ContactGateway
     *
     * @return List of [LocalContact]
     */
    suspend fun getLocalContactNumbers(): List<LocalContact>

    /**
     * Get list of local contact's email addresses from the ContactGateway
     *
     * @return List of [LocalContact]
     */
    suspend fun getLocalContactEmailAddresses(): List<LocalContact>

    /**
     * Get list of MEGA contacts
     *
     * @return List of [User]
     */
    suspend fun getAvailableContacts(): List<User>

    /**
     * Get list of outgoing contact requests
     *
     * @return List of [ContactRequest]
     */
    suspend fun getOutgoingContactRequests(): List<ContactRequest>

    /**
     * Get contact from cache by its handle
     *
     * @param contactId The contact's ID
     * @return The [Contact]
     */
    fun monitorContactByHandle(contactId: Long): Flow<Contact>
}
