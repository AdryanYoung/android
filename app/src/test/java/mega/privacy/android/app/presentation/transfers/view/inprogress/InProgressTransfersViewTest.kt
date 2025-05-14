package mega.privacy.android.app.presentation.transfers.view.inprogress

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.transfers.model.image.InProgressTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.TransferImageUiState
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.TEST_TAG_IN_PROGRESS_TRANSFER_ITEM
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class InProgressTransfersViewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val tag1 = 1
    private val tag2 = 2

    private val state =
        TransferImageUiState(fileTypeResId = iconPackR.drawable.ic_text_medium_solid)

    private val viewModel = mock<InProgressTransferImageViewModel> {
        on { getUiStateFlow(tag1) } doReturn MutableStateFlow(state)
        on { getUiStateFlow(tag2) } doReturn MutableStateFlow(state)
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(InProgressTransferImageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn viewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that view is displayed correctly if there are no active transfers`() {
        initComposeTestRule()
        with(composeTestRule) {
            val emptyText =
                activity.getString(sharedR.string.transfers_no_active_transfers_empty_text)
                    .replace("[A]", "").replace("[/A]", "")

            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW).assertIsDisplayed()
            onNodeWithText(emptyText).assertIsDisplayed()
        }
    }

    @Test
    fun `test that view is displayed correctly if there is one in progress transfer`() {
        val inProgressTransfers = listOf(getTransfer(tag = tag1)).toImmutableList()

        initComposeTestRule(inProgressTransfers = inProgressTransfers)
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag1").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag2").assertDoesNotExist()
        }
    }

    @Test
    fun `test that view is displayed correctly if there are two in progress transfers`() {
        val inProgressTransfers = listOf(
            getTransfer(tag = tag1),
            getTransfer(tag = tag2),
        ).toImmutableList()

        initComposeTestRule(inProgressTransfers = inProgressTransfers)
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag1").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag2").assertIsDisplayed()
        }
    }

    private fun initComposeTestRule(
        inProgressTransfers: ImmutableList<InProgressTransfer> = emptyList<InProgressTransfer>().toImmutableList(),
        isOverQuota: Boolean = false,
        areTransfersPaused: Boolean = false,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                InProgressTransfersView(
                    inProgressTransfers = inProgressTransfers,
                    isOverQuota = isOverQuota,
                    areTransfersPaused = areTransfersPaused,
                    onPlayPauseClicked = {}
                )
            }
        }
    }

    private fun getTransfer(tag: Int) = InProgressTransfer.Download(
        uniqueId = tag.toLong(),
        tag = tag,
        totalBytes = 100,
        isPaused = false,
        fileName = "name",
        speed = 100,
        state = TransferState.STATE_ACTIVE,
        priority = BigInteger.ONE,
        progress = Progress(0.5F),
        nodeId = NodeId(1),
    )
}