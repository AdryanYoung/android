package mega.privacy.android.app.main.controllers;

import static mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile;
import static mega.privacy.android.app.utils.ChatUtil.getMegaChatMessage;
import static mega.privacy.android.app.utils.ChatUtil.getMutedPeriodString;
import static mega.privacy.android.app.utils.Constants.ACTION_FORWARD_MESSAGES;
import static mega.privacy.android.app.utils.Constants.ID_CHAT_FROM;
import static mega.privacy.android.app.utils.Constants.ID_MESSAGES;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CHAT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER;
import static mega.privacy.android.app.utils.ContactUtil.getContactEmailDB;
import static mega.privacy.android.app.utils.ContactUtil.getContactNameDB;
import static mega.privacy.android.app.utils.ContactUtil.getFirstNameDB;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.MegaNodeUtil.existsMyChatFilesFolder;
import static mega.privacy.android.app.utils.MegaNodeUtil.getMyChatFilesFolder;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TimeUtils.getCorrectStringDependingOnCalendar;
import static mega.privacy.android.app.utils.Util.showSnackbar;
import static mega.privacy.android.app.utils.Util.toCDATA;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity;
import mega.privacy.android.app.utils.MeetingUtil;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage;
import mega.privacy.android.data.model.chat.NonContactInfo;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class ChatController {

    private final Context context;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH;
    private ExportListener exportListener;

    public ChatController(Context context) {
        Timber.d("ChatController created");
        this.context = context;

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if (dbH == null) {
            dbH = DbHandlerModuleKt.getDbHandler();
        }
    }

    public void archiveChat(MegaChatRoom chat) {
        Timber.d("Chat ID: %s", chat.getChatId());
        if (context instanceof GroupChatInfoActivity) {

            megaChatApi.archiveChat(chat.getChatId(), !chat.isArchived(), (GroupChatInfoActivity) context);
        }
    }

    public void deleteMessages(ArrayList<MegaChatMessage> messages, MegaChatRoom chat) {
        Timber.d("Messages to delete: %s", messages.size());
        for (int i = 0; i < messages.size(); i++) {
            deleteMessage(messages.get(i), chat.getChatId());
        }
    }

    public void deleteMessage(MegaChatMessage message, long chatId) {
        Timber.d("Message : %d, Chat ID: %d", message.getMsgId(), chatId);
        MegaChatMessage messageToDelete;
        if (message.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || message.getType() == MegaChatMessage.TYPE_VOICE_CLIP) {
            Timber.d("Delete node attachment message or voice clip message");
            if (message.getType() == MegaChatMessage.TYPE_VOICE_CLIP &&
                    message.getMegaNodeList() != null && message.getMegaNodeList().size() > 0 &&
                    message.getMegaNodeList().get(0) != null) {
                deleteOwnVoiceClip(context, message.getMegaNodeList().get(0).getName());
            }
            messageToDelete = megaChatApi.revokeAttachmentMessage(chatId, message.getMsgId());
        } else {
            Timber.d("Delete normal message with status = %s", message.getStatus());
            if (message.getStatus() == MegaChatMessage.STATUS_SENDING && message.getMsgId() == INVALID_HANDLE) {
                messageToDelete = megaChatApi.deleteMessage(chatId, message.getTempId());
            } else {
                messageToDelete = megaChatApi.deleteMessage(chatId, message.getMsgId());
            }
        }

        if (messageToDelete == null) {
            Timber.d("The message cannot be deleted");
        }
    }

    /*
     * Delete a voice note from local storage
     */
    public static void deleteOwnVoiceClip(Context mContext, String nameFile) {
        Timber.d("deleteOwnVoiceClip");
        File localFile = buildVoiceClipFile(nameFile);
        if (!isFileAvailable(localFile)) return;
        localFile.delete();
    }

    public void alterParticipantsPermissions(long chatid, long uh, int privilege) {
        Timber.d("Chat ID: %d, User (uh): %d, Priv: %d", chatid, uh, privilege);
        megaChatApi.updateChatPermissions(chatid, uh, privilege, (GroupChatInfoActivity) context);
    }

    public void removeParticipant(long chatid, long uh) {
        Timber.d("Chat ID: %d, User (uh): %d", chatid, uh);
        if (context == null) {
            Timber.w("Context is NULL");
        }
        megaChatApi.removeFromChat(chatid, uh, (GroupChatInfoActivity) context);
    }

    public void changeTitle(long chatid, String title) {
        if (context instanceof GroupChatInfoActivity) {
            megaChatApi.setChatTitle(chatid, title, (GroupChatInfoActivity) context);
        }
    }

    /**
     * Method to silence notifications for all chats or for a specific chat.
     *
     * @param option The selected mute option.
     */
    public void muteChat(String option) {
        if (context instanceof ChatNotificationsPreferencesActivity)
            return;

        switch (option) {
            case NOTIFICATIONS_ENABLED:
                showSnackbar(context, context.getString(R.string.success_unmuting_a_chat));
                break;

            case NOTIFICATIONS_DISABLED:
                showSnackbar(context, context.getString(R.string.notifications_are_already_muted));
                break;

            case NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING:
            case NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING:
                showSnackbar(context, getCorrectStringDependingOnCalendar(option, context));
                break;

            default:
                String text = getMutedPeriodString(option);
                if (!isTextEmpty(text)) {
                    showSnackbar(context, context.getString(R.string.success_muting_a_chat_for_specific_time, text));
                }
        }
    }

    public String createManagementString(MegaChatMessage message, MegaChatRoom chatRoom) {
        if (message == null) {
            Timber.w("Null MegaChatMessage");
            return null;
        }

        Timber.d("MessageID: %d, Chat ID: %d", message.getMsgId(), chatRoom.getChatId());
        long userHandle = message.getUserHandle();

        if (message.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) {
            Timber.d("ALTER PARTICIPANT MESSAGE!!");

            if (megaApi.getMyUser() != null && message.getHandleOfAction() == megaApi.getMyUser().getHandle()) {
                Timber.d("Me alter participant");

                StringBuilder builder = new StringBuilder();

                int privilege = message.getPrivilege();
                Timber.d("Privilege of me: %s", privilege);
                String textToShow;
                String fullNameAction = getParticipantFullName(message.getUserHandle());

                if (privilege != MegaChatRoom.PRIV_RM) {
                    Timber.d("I was added");
                    textToShow = String.format(context.getString(R.string.non_format_message_add_participant), megaChatApi.getMyFullname(), fullNameAction);
                } else {
                    Timber.d("I was removed or left");
                    if (message.getUserHandle() == message.getHandleOfAction()) {
                        Timber.d("I left the chat");
                        textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), megaChatApi.getMyFullname());

                    } else {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), megaChatApi.getMyFullname(), fullNameAction);
                    }
                }

                builder.append(textToShow);
                return builder.toString();

            } else {
                Timber.d("CONTACT Message type ALTER PARTICIPANTS");

                int privilege = message.getPrivilege();
                Timber.d("Privilege of the user: %s", privilege);

                String fullNameTitle = getParticipantFullName(message.getHandleOfAction());

                StringBuilder builder = new StringBuilder();

                String textToShow = "";
                if (privilege != MegaChatRoom.PRIV_RM) {
                    Timber.d("Participant was added");
                    if (megaApi.getMyUser() != null && message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        Timber.d("By me");
                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {
                        Timber.d("By other");
                        String fullNameAction = getParticipantFullName(message.getUserHandle());
                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, fullNameAction);
                    }
                }//END participant was added
                else {
                    Timber.d("Participant was removed or left");
                    if (megaApi.getMyUser() != null && message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {

                        if (message.getUserHandle() == message.getHandleOfAction()) {
                            Timber.d("The participant left the chat");

                            textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), fullNameTitle);

                        } else {
                            Timber.d("The participant was removed");
                            String fullNameAction = getParticipantFullName(message.getUserHandle());
                            textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, fullNameAction);
                        }
                    }
                } //END participant removed

                builder.append(textToShow);
                return builder.toString();

            } //END CONTACT MANAGEMENT MESSAGE
        } else if (message.getType() == MegaChatMessage.TYPE_PRIV_CHANGE) {
            int privilege = message.getPrivilege();
            Timber.d("Privilege of the user: %s", privilege);

            StringBuilder builder = new StringBuilder();
            String participantsNameWhoMadeTheAction = (megaApi.getMyUser() != null && message.getHandleOfAction() == megaApi.getMyUser().getHandle()) ? megaChatApi.getMyFullname() : getParticipantFullName(message.getHandleOfAction());
            String participantsNameWhosePermissionsWereChanged = (megaApi.getMyUser() != null && message.getUserHandle() == megaApi.getMyUser().getHandle()) ? megaChatApi.getMyFullname() : getParticipantFullName(message.getUserHandle());

            String textToShow = "";
            switch (privilege) {
                case MegaChatRoom.PRIV_MODERATOR:
                    textToShow = context.getString(R.string.chat_chats_list_last_message_permissions_changed_to_host, participantsNameWhoMadeTheAction, participantsNameWhosePermissionsWereChanged);
                    break;
                case MegaChatRoom.PRIV_STANDARD:
                    textToShow = context.getString(R.string.chat_chats_list_last_message_permissions_changed_to_standard, participantsNameWhoMadeTheAction, participantsNameWhosePermissionsWereChanged);
                    break;
                case MegaChatRoom.PRIV_RO:
                    textToShow = context.getString(R.string.chat_chats_list_last_message_permissions_changed_to_read_only, participantsNameWhoMadeTheAction, participantsNameWhosePermissionsWereChanged);
                    break;
            }

            builder.append(textToShow);
            return builder.toString();
        } else {
            Timber.d("Other type of messages");
            //OTHER TYPE OF MESSAGES
            if (megaApi.getMyUser() != null && megaApi.getMyUser().getHandle() == message.getUserHandle()) {
                Timber.d("MY message ID: %s", message.getMsgId());

                StringBuilder builder = new StringBuilder();
                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {
                    Timber.d("Message type NORMAL");

                    builder.append(context.getString(R.string.chat_last_message_sender_me)).append(": ");
                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        Timber.d("Message is edited");
                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        Timber.d("Message is deleted");

                        String textToShow = context.getString(R.string.text_deleted_message);
                        builder.append(textToShow);
                        return builder.toString();

                    } else {
                        builder.append(messageContent);
                        return builder.toString();
                    }
                } else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    Timber.d("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(megaChatApi.getMyFullname()));
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    builder.append(result);
                    return builder.toString();
                } else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    Timber.d("Message type TITLE CHANGE - Message ID: %s", message.getMsgId());

                    String messageContent = message.getContent();
                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), megaChatApi.getMyFullname(), messageContent);
                    builder.append(textToShow);
                    return builder.toString();

                } else if (message.getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                    builder.append(context.getString(R.string.chat_last_message_sender_me)).append(": ");
                    MegaChatContainsMeta meta = message.getContainsMeta();
                    if (meta != null) {
                        switch (meta.getType()) {
                            case MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW: {
                                String text = meta.getRichPreview().getText();
                                builder.append(text);
                                return builder.toString();
                            }
                            case MegaChatContainsMeta.CONTAINS_META_GEOLOCATION: {
                                String text = context.getString(R.string.title_geolocation_message);
                                builder.append(text);
                                return builder.toString();
                            }
                            case MegaChatContainsMeta.CONTAINS_META_GIPHY: {
                                String text = message.getContainsMeta().getGiphy().getTitle();
                                builder.append(text);
                                return builder.toString();
                            }
                        }
                    }

                    return "";
                } else if (message.getType() == MegaChatMessage.TYPE_CALL_STARTED) {
                    builder.append(context.getString(R.string.chat_last_message_sender_me)).append(": ");
                    String textToShow = MeetingUtil.getAppropriateStringForCallStarted(context).toString();
                    builder.append(textToShow);
                    return builder.toString();
                } else if (message.getType() == MegaChatMessage.TYPE_CALL_ENDED) {
                    builder.append(context.getString(R.string.chat_last_message_sender_me)).append(": ");
                    String textToShow = "";
                    switch (message.getTermCode()) {
                        case MegaChatMessage.END_CALL_REASON_BY_MODERATOR:
                        case MegaChatMessage.END_CALL_REASON_ENDED:
                            textToShow = MeetingUtil.getAppropriateStringForCallEnded(chatRoom, message.getDuration(), context).toString();
                            break;
                        case MegaChatMessage.END_CALL_REASON_REJECTED:
                            textToShow = MeetingUtil.getAppropriateStringForCallRejected(context).toString();
                            break;

                        case MegaChatMessage.END_CALL_REASON_NO_ANSWER:
                            textToShow = MeetingUtil.getAppropriateStringForCallNoAnswered(message.getUserHandle(), context).toString();
                            break;

                        case MegaChatMessage.END_CALL_REASON_FAILED:
                            textToShow = MeetingUtil.getAppropriateStringForCallFailed(context).toString();
                            break;

                        case MegaChatMessage.END_CALL_REASON_CANCELLED:
                            textToShow = MeetingUtil.getAppropriateStringForCallCancelled(message.getUserHandle(), context).toString();
                            break;
                    }

                    builder.append(textToShow);
                    return builder.toString();
                } else {
                    return "";
                }
            } else {
                Timber.d("Contact message!!");

                String fullNameTitle = getParticipantFullName(userHandle);
                StringBuilder builder = new StringBuilder();

                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {
                    builder.append(fullNameTitle).append(": ");
                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        Timber.d("Message is edited");

                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        Timber.d("Message is deleted");
                        String textToShow = "";
                        if (chatRoom.isGroup()) {
                            textToShow = String.format(context.getString(R.string.non_format_text_deleted_message_by), fullNameTitle);
                        } else {
                            textToShow = context.getString(R.string.text_deleted_message);
                        }

                        builder.append(textToShow);
                        return builder.toString();

                    } else {
                        builder.append(messageContent);
                        return builder.toString();

                    }
                } else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    Timber.d("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.non_format_history_cleared_by), fullNameTitle);
                    builder.append(textToShow);
                    return builder.toString();

                } else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    Timber.d("Message type CHANGE TITLE - Message ID: %s", message.getMsgId());

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), fullNameTitle, messageContent);
                    builder.append(textToShow);
                    return builder.toString();
                } else if (message.getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                    builder.append(fullNameTitle).append(": ");
                    MegaChatContainsMeta meta = message.getContainsMeta();
                    if (meta != null) {
                        switch (meta.getType()) {
                            case MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW: {
                                String text = meta.getRichPreview().getText();
                                builder.append(text);
                                return builder.toString();
                            }
                            case MegaChatContainsMeta.CONTAINS_META_GEOLOCATION: {
                                String text = context.getString(R.string.title_geolocation_message);
                                builder.append(text);
                                return builder.toString();
                            }
                            case MegaChatContainsMeta.CONTAINS_META_GIPHY: {
                                String text = message.getContainsMeta().getGiphy().getTitle();
                                builder.append(text);
                                return builder.toString();
                            }
                        }
                    }
                    return "";
                } else if (message.getType() == MegaChatMessage.TYPE_CALL_STARTED) {
                    builder.append(fullNameTitle).append(": ");
                    String textToShow = MeetingUtil.getAppropriateStringForCallStarted(context).toString();
                    builder.append(textToShow);
                    return builder.toString();
                } else if (message.getType() == MegaChatMessage.TYPE_CALL_ENDED) {
                    builder.append(fullNameTitle).append(": ");
                    String textToShow = "";
                    switch (message.getTermCode()) {
                        case MegaChatMessage.END_CALL_REASON_BY_MODERATOR:
                        case MegaChatMessage.END_CALL_REASON_ENDED:
                            textToShow = MeetingUtil.getAppropriateStringForCallEnded(chatRoom, message.getDuration(), context).toString();
                            break;

                        case MegaChatMessage.END_CALL_REASON_REJECTED:
                            textToShow = MeetingUtil.getAppropriateStringForCallRejected(context).toString();
                            break;

                        case MegaChatMessage.END_CALL_REASON_NO_ANSWER:
                            textToShow = MeetingUtil.getAppropriateStringForCallNoAnswered(message.getUserHandle(), context).toString();
                            break;

                        case MegaChatMessage.END_CALL_REASON_FAILED:
                            textToShow = MeetingUtil.getAppropriateStringForCallFailed(context).toString();
                            break;

                        case MegaChatMessage.END_CALL_REASON_CANCELLED:
                            textToShow = MeetingUtil.getAppropriateStringForCallCancelled(message.getUserHandle(), context).toString();
                            break;
                    }

                    builder.append(textToShow);
                    return builder.toString();
                } else {
                    Timber.d("Message type: %s", message.getType());
                    Timber.d("Message ID: %s", message.getMsgId());
                    return "";
                }
            }
        }
    }

    /**
     * Gets a partcipant's name (not contact).
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     *
     * @param userHandle participant's identifier
     * @return The participant's name.
     */
    private String getNonContactFirstName(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        if (nonContact == null) {
            return "";
        }

        String name = nonContact.getFirstName();

        if (isTextEmpty(name)) {
            name = nonContact.getLastName();
        }

        if (isTextEmpty(name)) {
            name = nonContact.getEmail();
        }

        return name;
    }

    /**
     * Gets a partcipant's full name (not contact).
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     *
     * @param userHandle participant's identifier
     * @return The participant's full name.
     */
    private String getNonContactFullName(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        if (nonContact == null) {
            return "";
        }

        String fullName = nonContact.getFullName();

        if (isTextEmpty(fullName)) {
            fullName = nonContact.getEmail();
        }

        return fullName;
    }

    /**
     * Gets a partcipant's email (not contact).
     *
     * @param userHandle participant's identifier
     * @return The participant's email.
     */
    private String getNonContactEmail(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        return nonContact != null ? nonContact.getEmail() : "";
    }

    public String getMyFullName() {

        String fullName = megaChatApi.getMyFullname();

        if (fullName != null) {
            if (fullName.isEmpty()) {
                Timber.d("Put MY email as fullname");
                String myEmail = megaChatApi.getMyEmail();
                String[] splitEmail = myEmail.split("[@._]");
                fullName = splitEmail[0];
            } else {
                if (fullName.trim().length() <= 0) {
                    Timber.d("Put MY email as fullname");
                    String myEmail = megaChatApi.getMyEmail();
                    String[] splitEmail = myEmail.split("[@._]");
                    fullName = splitEmail[0];
                }
            }
        } else {
            Timber.d("Put MY email as fullname");
            String myEmail = megaChatApi.getMyEmail();
            String[] splitEmail = myEmail.split("[@._]");
            fullName = splitEmail[0];
        }

        return fullName;
    }

    public void importNode(long idMessage, long idChat, int typeImport) {
        Timber.d("Message ID: %d, Chat ID: %d", idMessage, idChat);
        ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
        MegaChatMessage m = getMegaChatMessage(context, megaChatApi, idChat, idMessage);

        if (m != null) {
            AndroidMegaChatMessage aMessage = new AndroidMegaChatMessage(m);
            messages.add(aMessage);
            importNodesFromAndroidMessages(messages, typeImport);
        } else {
            Timber.w("Message cannot be recovered - null");
        }
    }

    public void importNodesFromMessages(ArrayList<MegaChatMessage> messages) {
        Timber.d("importNodesFromMessages");

        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if (context instanceof NodeAttachmentHistoryActivity) {
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    public void importNodesFromAndroidMessages(ArrayList<AndroidMegaChatMessage> messages, int typeImport) {
        Timber.d("importNodesFromAndroidMessages");

        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMessage().getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if (context instanceof NodeAttachmentHistoryActivity) {
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    public void prepareMessagesToForward(ArrayList<MegaChatMessage> messagesSelected, long idChat) {
        Timber.d("Number of messages: %d,Chat ID: %d", messagesSelected.size(), idChat);

        ArrayList<MegaChatMessage> messagesToImport = new ArrayList<>();
        long[] idMessages = new long[messagesSelected.size()];
        for (int i = 0; i < messagesSelected.size(); i++) {
            idMessages[i] = messagesSelected.get(i).getMsgId();

            Timber.d("Type of message: %s", messagesSelected.get(i).getType());
            if ((messagesSelected.get(i).getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) || (messagesSelected.get(i).getType() == MegaChatMessage.TYPE_VOICE_CLIP)) {
                if (messagesSelected.get(i).getUserHandle() != megaChatApi.getMyUserHandle()) {
                    //Node has to be imported
                    messagesToImport.add(messagesSelected.get(i));
                }
            }
        }

        if (messagesToImport.isEmpty()) {
            forwardMessages(messagesSelected, idChat);
        } else {
            if (context instanceof NodeAttachmentHistoryActivity) {
                ((NodeAttachmentHistoryActivity) context).storedUnhandledData(messagesSelected, messagesToImport);
                if (existsMyChatFilesFolder()) {
                    ((NodeAttachmentHistoryActivity) context).setMyChatFilesFolder(getMyChatFilesFolder());
                    ((NodeAttachmentHistoryActivity) context).handleStoredData();
                } else {
                    megaApi.getMyChatFilesFolder(new GetAttrUserListener(context));
                }
            }
        }
    }

    public void forwardMessages(ArrayList<MegaChatMessage> messagesSelected, long idChat) {
        Timber.d("Number of messages: %d, Chat ID: %d", messagesSelected.size(), idChat);

        long[] idMessages = new long[messagesSelected.size()];
        for (int i = 0; i < messagesSelected.size(); i++) {
            idMessages[i] = messagesSelected.get(i).getMsgId();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra(ID_MESSAGES, idMessages);
        i.putExtra(ID_CHAT_FROM, idChat);
        i.setAction(ACTION_FORWARD_MESSAGES);
        if (context instanceof NodeAttachmentHistoryActivity) {
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public MegaNode authorizeNodeIfPreview(MegaNode node, MegaChatRoom chatRoom) {
        if (chatRoom != null && chatRoom.isPreview()) {
            MegaNode nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.getAuthorizationToken());
            if (nodeAuthorized != null) {
                Timber.d("Authorized");
                return nodeAuthorized;
            }
        }
        Timber.d("NOT authorized");
        return node;
    }

    public boolean isInAnonymousMode() {
        return megaChatApi.getInitState() == MegaChatApi.INIT_ANONYMOUS;
    }

    public boolean isPreview(MegaChatRoom chatRoom) {
        if (chatRoom != null) {
            return chatRoom.isPreview();
        }

        return false;
    }

    /**
     * Stores in DB the user's attributes of a non contact.
     *
     * @param peerHandle identifier of the user to save
     */
    public void setNonContactAttributesInDB(long peerHandle) {
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        String firstName = megaChatApi.getUserFirstnameFromCache(peerHandle);
        if (!isTextEmpty(firstName)) {
            dbH.setNonContactFirstName(firstName, peerHandle + "");
        }

        String lastName = megaChatApi.getUserLastnameFromCache(peerHandle);
        if (!isTextEmpty(lastName)) {
            dbH.setNonContactLastName(lastName, peerHandle + "");
        }

        String email = megaChatApi.getUserEmailFromCache(peerHandle);
        if (!isTextEmpty(email)) {
            dbH.setNonContactEmail(email, peerHandle + "");
        }
    }

    /**
     * Use [GetParticipantFirstNameUseCase] instead.
     * Gets the participant's first name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     *
     * @param userHandle participant's identifier
     * @return The participant's first name
     */
    @Deprecated
    public String getParticipantFirstName(long userHandle) {
        String firstName = getFirstNameDB(userHandle);

        if (isTextEmpty(firstName)) {
            firstName = getNonContactFirstName(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserFirstnameFromCache(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserLastnameFromCache(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserEmailFromCache(userHandle);
        }

        return firstName;
    }

    /**
     * Gets the participant's full name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     *
     * @param handle participant's identifier
     * @return The participant's full name.
     */
    public String getParticipantFullName(long handle) {
        String fullName = getContactNameDB(handle);

        if (isTextEmpty(fullName)) {
            fullName = getNonContactFullName(handle);
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserFullnameFromCache(handle);
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserEmailFromCache(handle);
        }

        return fullName;
    }

    /**
     * Gets the participant's email.
     *
     * @param handle participant's identifier
     * @return The participant's email.
     */
    public String getParticipantEmail(long handle) {
        String email = getContactEmailDB(handle);

        if (isTextEmpty(email)) {
            email = getNonContactEmail(handle);
        }

        if (isTextEmpty(email)) {
            email = megaChatApi.getUserEmailFromCache(handle);
        }

        return email;
    }

    public void setExportListener(ExportListener exportListener) {
        this.exportListener = exportListener;
    }
}
