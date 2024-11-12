package mega.privacy.android.app.activities.settingsActivities

import mega.privacy.android.shared.resources.R as sharedR
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsFragment
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsViewModel

@AndroidEntryPoint
class CookiePreferencesActivity : PreferencesBaseActivity() {

    private val viewModel by viewModels<CookieSettingsViewModel>()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showUnsavedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setTitle(R.string.settings_about_cookie_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        replaceFragment(CookieSettingsFragment())

        showSaveButton {
            saveCookieSettings()
        }

        viewModel.onEnableBackPressedHandler().observe(this) { isEnable ->
            onBackPressedCallback.isEnabled = isEnable
        }
    }

    private fun showUnsavedDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.dialog_cookie_alert_unsaved))
            .setPositiveButton(getString(R.string.save_action)) { _: DialogInterface, _: Int ->
                saveCookieSettings()
            }
            .setNegativeButton(getString(sharedR.string.general_dialog_cancel_button), null)
            .create()
            .show()
    }

    private fun saveCookieSettings() {
        viewModel.saveCookieSettings()
        finish()
    }
}
