package mega.privacy.android.app.utils;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static mega.privacy.android.app.utils.Constants.NAME;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.main.megachat.ContactAttachmentActivity;
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity;
import mega.privacy.android.domain.entity.Contact;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaUser;

public class ContactUtil {

    /**
     * To retrieve the contact from cache
     *
     * @param contactHandle The contact's ID
     * @return contact
     * @deprecated Use GetContactFromCacheByHandleUseCase instead.
     */
    public static Contact getContactDB(long contactHandle) {
        return MegaApplication.getInstance().getDbH().findContactByHandle(contactHandle);
    }

    public static String getMegaUserNameDB(MegaUser user) {
        if (user == null) return null;
        String nameContact = getContactNameDB(user.getHandle());
        if (nameContact != null) {
            return nameContact;
        }

        return user.getEmail();
    }

    public static String getContactNameDB(Contact contactDB) {
        if (contactDB == null) {
            return null;
        }

        String nicknameText = contactDB.getNickname();
        if (nicknameText != null) {
            return nicknameText;
        }

        String firstNameText = contactDB.getFirstName();
        String lastNameText = contactDB.getLastName();
        String emailText = contactDB.getEmail();

        return buildFullName(firstNameText, lastNameText, emailText);
    }

    public static String getContactNameDB(long contactHandle) {
        Contact contactDB = getContactDB(contactHandle);
        if (contactDB != null) {
            return getContactNameDB(contactDB);
        }

        return null;
    }

    public static String getNicknameContact(long contactHandle) {
        Contact contactDB = getContactDB(contactHandle);
        if (contactDB == null)
            return null;

        return contactDB.getNickname();
    }

    public static String getNicknameContact(String email) {
        Contact contactDB = MegaApplication.getInstance().getDbH().findContactByEmail(email);
        if (contactDB != null) {
            return contactDB.getNickname();
        }

        return null;
    }

    public static String buildFullName(String name, String lastName, String mail) {
        String fullName = "";
        if (!isTextEmpty(name)) {
            fullName = name;
            if (!isTextEmpty(lastName)) {
                fullName = fullName + " " + lastName;
            }
        } else if (!isTextEmpty(lastName)) {
            fullName = lastName;
        } else if (!isTextEmpty(mail)) {
            fullName = mail;
        }
        return fullName;
    }

    public static String getFirstNameDB(long contactHandle) {
        Contact contactDB = getContactDB(contactHandle);
        if (contactDB != null) {
            String nicknameText = contactDB.getNickname();
            if (nicknameText != null) {
                return nicknameText;
            }

            String firstNameText = contactDB.getFirstName();
            if (!isTextEmpty(firstNameText)) {
                return firstNameText;
            }

            String lastNameText = contactDB.getLastName();
            if (!isTextEmpty(lastNameText)) {
                return lastNameText;
            }

            String emailText = contactDB.getEmail();
            if (!isTextEmpty(emailText)) {
                return emailText;
            }
        }
        return "";
    }

    public static void notifyNicknameUpdate(Context context, long userHandle) {
        notifyUserNameUpdate(context, ACTION_UPDATE_NICKNAME, userHandle);
    }

    public static void notifyFirstNameUpdate(Context context, long userHandle) {
        notifyUserNameUpdate(context, ACTION_UPDATE_FIRST_NAME, userHandle);
    }

    public static void notifyLastNameUpdate(Context context, long userHandle) {
        notifyUserNameUpdate(context, ACTION_UPDATE_LAST_NAME, userHandle);
    }

    public static void notifyUserNameUpdate(Context context, String action, long userHandle) {
        Intent intent = new Intent(action)
                .putExtra(EXTRA_USER_HANDLE, userHandle).setPackage(context.getApplicationContext().getPackageName());
        context.sendBroadcast(intent);
    }

    /**
     * Checks if the user who their handle is received by parameter is a contact.
     *
     * @param userHandle handle of the user
     * @return true if the user is a contact, false otherwise.
     */
    public static boolean isContact(long userHandle) {
        if (userHandle == INVALID_HANDLE) {
            return false;
        }

        return isContact(MegaApiJava.userHandleToBase64(userHandle));
    }

    /**
     * Checks if the user who their email of handle in base64 is received by parameter is a contact.
     *
     * @param emailOrUserHandleBase64 email or user's handle in base64
     * @return true if the user is a contact, false otherwise.
     */
    public static boolean isContact(String emailOrUserHandleBase64) {
        if (isTextEmpty(emailOrUserHandleBase64)) {
            return false;
        }

        MegaUser contact = MegaApplication.getInstance().getMegaApi().getContact(emailOrUserHandleBase64);
        return contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE;
    }

    /**
     * Gets a contact's email from DB.
     *
     * @param contactHandle contact's identifier
     * @return The contact's email.
     */
    public static String getContactEmailDB(long contactHandle) {
        Contact contactDB = getContactDB(contactHandle);
        return contactDB != null ? getContactEmailDB(contactDB) : null;
    }

    /**
     * Gets a contact's email from DB.
     *
     * @param contactDB contact's MegaContactDB
     * @return The contact's email.
     */
    public static String getContactEmailDB(Contact contactDB) {
        return contactDB != null ? contactDB.getEmail() : null;
    }

    /**
     * Method to open ContactInfoActivity.class.
     *
     * @param context Activity context.
     * @param name    The name of the contact.
     */
    public static void openContactInfoActivity(Context context, String name) {
        Intent i = new Intent(context, ContactInfoActivity.class);
        i.putExtra(NAME, name);
        context.startActivity(i);
    }

    /**
     * Method to open ContactAttachmentActivity.class.
     *
     * @param context Activity context.
     * @param chatId  The ID of a chat.
     * @param msgId   The ID of a message.
     */
    public static void openContactAttachmentActivity(Context context, long chatId, long msgId) {
        Intent i = new Intent(context, ContactAttachmentActivity.class);
        i.putExtra(CHAT_ID, chatId);
        i.putExtra(MESSAGE_ID, msgId);
        context.startActivity(i);
    }
}
