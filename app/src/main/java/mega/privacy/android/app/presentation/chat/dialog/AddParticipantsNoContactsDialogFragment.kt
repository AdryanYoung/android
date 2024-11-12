package mega.privacy.android.app.presentation.chat.dialog

import mega.privacy.android.shared.resources.R as sharedR
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Fragment to display a custom two buttons alert dialog when user is trying to add participants
 * to a chat/meeting but has no contacts
 */
@AndroidEntryPoint
class AddParticipantsNoContactsDialogFragment : DialogFragment() {

    @Inject
    /** Current theme */
    lateinit var getThemeMode: GetThemeMode

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var navigator: MegaNavigator

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        )
            .apply {
                setTitle(getString(R.string.chat_add_participants_no_contacts_title))
                setMessage(getString(R.string.chat_add_participants_no_contacts_message))
                setNegativeButton(getString(sharedR.string.general_dialog_cancel_button)) { _, _ ->
                    dismiss()
                }
                setPositiveButton(getString(R.string.contact_invite)) { _, _ ->
                    navigator.openInviteContactActivity(
                        requireContext(),
                        false
                    )
                    dismiss()
                }
            }.create()

    companion object {
        /**
         * Creates an instance of this class
         *
         * @return AddParticipantsNoContactsDialogFragment new instance
         */
        @JvmStatic
        fun newInstance() = AddParticipantsNoContactsDialogFragment()
    }
}