package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.extensions.getZoneEndTime
import mega.privacy.android.app.presentation.extensions.getZoneStartTime
import mega.privacy.android.app.presentation.extensions.meeting.DialogOption
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingType
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

/**
 * Data class defining the state of [CreateScheduledMeetingViewModel]
 *
 * @property scheduledMeeting                           [ChatScheduledMeeting].
 * @property openAddContact                             True, if should open Add contact screen. False, if not.
 * @property chatIdToOpenInfoScreen                     Chat id to open the scheduled meeting info screen.
 * @property finish                         True, if the activity is to be terminated.
 * @property meetingTitle                               Meeting title.
 * @property startDate                                  Start Date.
 * @property endDate                                    End Date.
 * @property participantItemList                        List of participants handles.
 * @property enabledAllowAddParticipantsOption          True if is enabled the allow non-hosts to add participants option, false otherwise.
 * @property enabledMeetingLinkOption                   True if is enabled the meeting link option, false otherwise.
 * @property enabledSendCalendarInviteOption            True if is enabled the send calendar invite option, false otherwise.
 * @property enabledSendCalendarInviteOption            True if is enabled the send calendar invite option, false otherwise.
 * @property enabledWaitingRoomOption                   True if is enabled waiting room option, false otherwise.
 * @property descriptionText                            Description text
 * @property buttons                                    List of available action buttons.
 * @property snackbarMessageContent                     State to show snackbar message.
 * @property discardMeetingDialog                       True if show discard meeting alert dialog, false if not.
 * @property addParticipantsNoContactsDialog            True if show add participants no contacts dialog, false if not.
 * @property numOfParticipants                          Number of participants.
 * @property isEditingDescription                       True, if is editing description. False, if not.
 * @property descriptionText                            Description text.
 * @property isEmptyTitleError                          True, if an attempt has been made to create a meeting without a title. False, if not.
 * @property allowAddParticipants                       True, if can add participants. False, if not.
 * @property recurringMeetingDialog                     True if show recurring meeting dialog, false if not.
 * @property rulesSelected                              [ChatScheduledRules] selected.
 * @property customRecurrenceState                      [CustomRecurrenceState]
 * @property showMonthlyRecurrenceWarning               True, if the text on the monthly recurrence warning should be displayed. False, if not.
 * @property isCreatingMeeting                          True, if the meeting is being created. False, if not.
 * @property isAddingParticipants                       True, if is adding participants. False, if not.
 * @property type                                       [ScheduledMeetingType]
 * @property initialMeetingLinkOption                   True if is enabled the meeting link option, false otherwise.
 * @property initialAllowAddParticipantsOption          True if is enabled the allow non-hosts to add participants option, false otherwise.
 * @property initialSendCalendarInviteOption            True if is enabled the send calendar invite option, false otherwise.
 * @property initialWaitingRoomOption                   True if is enabled waiting room option, false otherwise.
 * @property initialParticipantsList                    List of participants handles.
 * @property participantsRemoved                        List of participants removed.
 * @property weekList                                   List of [Weekday] in the week.
 * @property meetingLink                                Meeting Link
 */
data class CreateScheduledMeetingState(
    val scheduledMeeting: ChatScheduledMeeting? = null,
    val openAddContact: Boolean? = null,
    val chatIdToOpenInfoScreen: Long? = null,
    val finish: Boolean = false,
    val buttons: List<ScheduleMeetingAction> = ScheduleMeetingAction.entries,
    val meetingTitle: String = "",
    val startDate: ZonedDateTime = ZonedDateTime.now(),
    val endDate: ZonedDateTime = startDate.plus(30, ChronoUnit.MINUTES),
    val rulesSelected: ChatScheduledRules = ChatScheduledRules(),
    val customRecurrenceState: CustomRecurrenceState = CustomRecurrenceState(),
    val participantItemList: List<ContactItem> = emptyList(),
    val enabledMeetingLinkOption: Boolean = false,
    val enabledAllowAddParticipantsOption: Boolean = false,
    val enabledSendCalendarInviteOption: Boolean = false,
    val enabledWaitingRoomOption: Boolean = false,
    val descriptionText: String = "",
    val snackbarMessageContent: StateEventWithContent<String> = consumed(),
    val discardMeetingDialog: Boolean = false,
    val addParticipantsNoContactsDialog: Boolean = false,
    val numOfParticipants: Int = 1,
    val isEditingDescription: Boolean = false,
    val isAddingParticipants: Boolean = false,
    val isEmptyTitleError: Boolean = false,
    val allowAddParticipants: Boolean = true,
    val recurringMeetingDialog: Boolean = false,
    val showMonthlyRecurrenceWarning: Boolean = false,
    val isCreatingMeeting: Boolean = false,
    val type: ScheduledMeetingType = ScheduledMeetingType.Creation,
    val initialMeetingLinkOption: Boolean = false,
    val initialAllowAddParticipantsOption: Boolean = true,
    val initialSendCalendarInviteOption: Boolean = false,
    val initialWaitingRoomOption: Boolean = false,
    val initialParticipantsList: List<ContactItem> = emptyList(),
    val participantsRemoved: List<ContactItem> = emptyList(),
    val meetingLink: String? = null,
    val weekList: List<Weekday> =
        listOf(
            Weekday.Monday,
            Weekday.Tuesday,
            Weekday.Wednesday,
            Weekday.Thursday,
            Weekday.Friday,
            Weekday.Saturday,
            Weekday.Sunday
        ),
) {
    /**
     * Check if it's valid title
     *
     * @return True if it's valid, false if not.
     */
    fun isValid(): Boolean =
        meetingTitle.isNotBlank() && isMeetingTitleRightSize() &&
                (type == ScheduledMeetingType.Creation || isValidEdition())

    /**
     * Check if it's valid edition
     *
     * @return True if it's valid, false if not.
     */
    private fun isValidEdition(): Boolean {
        scheduledMeeting?.let { schedMeet ->
            return ((schedMeet.isCanceled && schedMeet.rules?.freq != OccurrenceFrequencyType.Invalid) ||
                    schedMeet.title != meetingTitle ||
                    (schedMeet.description ?: "") != descriptionText ||
                    initialMeetingLinkOption != enabledMeetingLinkOption ||
                    initialAllowAddParticipantsOption != enabledAllowAddParticipantsOption ||
                    initialSendCalendarInviteOption != enabledSendCalendarInviteOption ||
                    initialWaitingRoomOption != enabledWaitingRoomOption ||
                    !startDate.isEqual(schedMeet.getZoneStartTime()) ||
                    !endDate.isEqual(schedMeet.getZoneEndTime()) ||
                    initialParticipantsList != participantItemList ||
                    (schedMeet.rules ?: ChatScheduledRules()) != rulesSelected
                    )
        }

        return false
    }

    /**
     * Check if meeting title has the right length
     */
    fun isMeetingTitleRightSize(): Boolean = meetingTitle.length <= Constants.MAX_TITLE_SIZE

    /**
     * Check if meeting description has the right length
     */
    fun isMeetingDescriptionTooLong(): Boolean =
        descriptionText.length > Constants.MAX_DESCRIPTION_SIZE

    /**
     * Get list of participants ids
     */
    fun getParticipantsIds(): List<Long> = mutableListOf<Long>().apply {
        participantItemList.forEach {
            add(it.handle)
        }
    }

    /**
     * Get list of participants emails
     */
    fun getParticipantsEmails(): List<String> =
        mutableListOf<String>().apply {
            participantItemList.forEach {
                add(it.email)
            }
        }

    /**
     * Check if is week days selected
     *
     * @return True if weekdays are selected. False, if not.
     */
    fun isWeekdays(): Boolean =
        (rulesSelected.freq == OccurrenceFrequencyType.Daily || rulesSelected.freq == OccurrenceFrequencyType.Weekly) && rulesSelected.weekDayList == getWeekdaysList()

    /**
     * Get Weekdays list
     *
     * @return list of [Weekday]
     */
    fun getWeekdaysList(): List<Weekday> =
        listOf(
            Weekday.Monday,
            Weekday.Tuesday,
            Weekday.Wednesday,
            Weekday.Thursday,
            Weekday.Friday
        )

    /**
     * Check if is custom recurrence
     *
     * @return True if it's custom recurrence
     */
    private fun isCustomRecurrenceDialogOption(): Boolean {
        val weekDayList = rulesSelected.weekDayList
        val monthDayList = rulesSelected.monthDayList
        val monthWeekDayList = rulesSelected.monthWeekDayList
        return when (rulesSelected.freq) {
            OccurrenceFrequencyType.Invalid -> false
            OccurrenceFrequencyType.Daily -> rulesSelected.interval > 1 || weekDayList != null
            OccurrenceFrequencyType.Weekly -> rulesSelected.interval > 1 || (weekDayList != null && (weekDayList.size > 1 || weekDayList.first() != getStartWeekDay()))
            OccurrenceFrequencyType.Monthly -> rulesSelected.interval > 1 || (monthDayList != null && (monthDayList.size > 1 || monthDayList.first() != getStartMonthDay())) || monthWeekDayList.isNotEmpty()
        }
    }

    /**
     * Get start date plus 6 months
     * @return start date time plus 6 months
     */
    fun getStartDateTimePlus6Months(): ZonedDateTime = startDate.plusMonths(6)

    /**
     * Get month day of start date
     *
     * @return month day
     */
    fun getStartMonthDay(): Int = startDate.dayOfMonth


    /**
     * Get month weekday list
     *
     * @return month weekday list
     */
    fun getDefaultMonthWeekDayList(): List<MonthWeekDayItem> =
        listOf(MonthWeekDayItem(getNumberOfWeek(), listOf(getStartWeekDay())))

    /**
     * Get month day list of start date
     *
     * @return month day list
     */
    fun getStartMonthDayList() = listOf(
        startDate.dayOfMonth
    )

    /**
     * Get initial start date
     *
     * @return [ZonedDateTime]
     */
    fun getInitialStartDate(): ZonedDateTime = startDate.truncatedTo(ChronoUnit.MINUTES)
        .minusMinutes(startDate.minute.toLong() % 5)
        .plusMinutes(5)

    /**
     * Get initial end date
     *
     * @return [ZonedDateTime]
     */
    fun getInitialEndDate(): ZonedDateTime = getInitialStartDate().plus(30, ChronoUnit.MINUTES)

    /**
     * Get weekday of start date
     *
     * @return weekday
     */
    fun getStartWeekDay(): Weekday = when (startDate.dayOfWeek) {
        DayOfWeek.MONDAY -> Weekday.Monday
        DayOfWeek.TUESDAY -> Weekday.Tuesday
        DayOfWeek.WEDNESDAY -> Weekday.Wednesday
        DayOfWeek.THURSDAY -> Weekday.Thursday
        DayOfWeek.FRIDAY -> Weekday.Friday
        DayOfWeek.SATURDAY -> Weekday.Saturday
        else -> Weekday.Sunday
    }

    /**
     * Get number of week in start date
     */
    private fun getNumberOfWeek(): WeekOfMonth {
        val weekOfMonth = LocalDate
            .from(startDate)
            .get(WeekFields.ISO.weekOfMonth())

        return when (weekOfMonth) {
            1 -> WeekOfMonth.First
            2 -> WeekOfMonth.Second
            3 -> WeekOfMonth.Third
            4 -> WeekOfMonth.Fourth
            else -> WeekOfMonth.Fifth
        }
    }

    /**
     * Get the selected option
     *
     * @return option   [RecurrenceDialogOption]
     */
    fun getRecurrenceDialogOptionSelected(): RecurrenceDialogOption =
        if (isCustomRecurrenceDialogOption()) {
            RecurrenceDialogOption.Customised
        } else {
            rulesSelected.freq.DialogOption
        }

    /**
     * Get the list of options for Recurrence dialog
     *
     * @return list of [RecurrenceDialogOption]
     */
    fun getRecurrenceDialogOptionList(): List<RecurrenceDialogOption> =
        if (isCustomRecurrenceDialogOption()) listOf(
            RecurrenceDialogOption.Never,
            RecurrenceDialogOption.Customised,
            RecurrenceDialogOption.EveryDay,
            RecurrenceDialogOption.EveryWeek,
            RecurrenceDialogOption.EveryMonth,
            RecurrenceDialogOption.Custom
        ) else listOf(
            RecurrenceDialogOption.Never,
            RecurrenceDialogOption.EveryDay,
            RecurrenceDialogOption.EveryWeek,
            RecurrenceDialogOption.EveryMonth,
            RecurrenceDialogOption.Custom
        )
}
