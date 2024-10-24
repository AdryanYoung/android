package mega.privacy.android.app.presentation.documentscanner.groups

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SaveScannedDocumentsDestinationGroup]
 */
@RunWith(AndroidJUnit4::class)
internal class SaveScannedDocumentsDestinationGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the static ui components are displayed`() {
        composeTestRule.setContent {
            SaveScannedDocumentsDestinationGroup(
                originatedFromChat = false,
                selectedScanDestination = ScanDestination.CloudDrive,
                onScanDestinationSelected = {},
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER)
            .assertIsDisplayed()
    }

    @Test
    fun `test that only the chat chip is displayed if document scanning is accessed from chat`() {
        composeTestRule.setContent {
            SaveScannedDocumentsDestinationGroup(
                originatedFromChat = true,
                selectedScanDestination = ScanDestination.Chat,
                onScanDestinationSelected = {},
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT)
            .assertIsDisplayed()
    }

    @Test
    fun `test that both cloud drive and chat chips are displayed if document scanning is accessed anywhere other than chat`() {
        composeTestRule.setContent {
            SaveScannedDocumentsDestinationGroup(
                originatedFromChat = false,
                selectedScanDestination = ScanDestination.CloudDrive,
                onScanDestinationSelected = {},
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the cloud drive chip is selected`() {
        val onScanDestinationSelected = mock<(ScanDestination) -> Unit>()
        composeTestRule.setContent {
            SaveScannedDocumentsDestinationGroup(
                originatedFromChat = false,
                selectedScanDestination = ScanDestination.CloudDrive,
                onScanDestinationSelected = onScanDestinationSelected,
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE)
            .performClick()

        verify(onScanDestinationSelected).invoke(ScanDestination.CloudDrive)
    }

    @Test
    fun `test that the chat chip is selected`() {
        val onScanDestinationSelected = mock<(ScanDestination) -> Unit>()
        composeTestRule.setContent {
            SaveScannedDocumentsDestinationGroup(
                originatedFromChat = false,
                selectedScanDestination = ScanDestination.Chat,
                onScanDestinationSelected = onScanDestinationSelected,
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT)
            .performClick()

        verify(onScanDestinationSelected).invoke(ScanDestination.Chat)
    }
}