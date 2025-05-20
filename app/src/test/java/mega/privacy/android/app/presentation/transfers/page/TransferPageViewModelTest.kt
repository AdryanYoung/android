package mega.privacy.android.app.presentation.transfers.page

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteAllCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteFailedOrCancelledTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TransferPageViewModelTest {
    private val pauseTransfersQueueUseCase: PauseTransfersQueueUseCase = mock()
    private val cancelTransfersUseCase: CancelTransfersUseCase = mock()
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase = mock()
    private val deleteFailedOrCancelledTransfersUseCase: DeleteFailedOrCancelledTransfersUseCase =
        mock()
    private val deleteAllCompletedTransfersUseCase: DeleteAllCompletedTransfersUseCase = mock()
    private lateinit var underTest: TransferPageViewModel

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            pauseTransfersQueueUseCase,
            cancelTransfersUseCase,
            stopCameraUploadsUseCase,
            deleteFailedOrCancelledTransfersUseCase,
            deleteAllCompletedTransfersUseCase,
        )
    }

    private fun initTestClass() {
        underTest = TransferPageViewModel(
            pauseTransfersQueueUseCase = pauseTransfersQueueUseCase,
            cancelTransfersUseCase = cancelTransfersUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            deleteAllCompletedTransfersUseCase = deleteAllCompletedTransfersUseCase,
            deleteFailedOrCancelledTransfersUseCase = deleteFailedOrCancelledTransfersUseCase,
        )
    }

    @Test
    fun `test that cancelTransfersResult update correctly when call cancelAllTransfers successfully`() =
        runTest {
            whenever(cancelTransfersUseCase()).thenReturn(Unit)
            underTest.cancelAllTransfers()
            underTest.state.test {
                val result = awaitItem().cancelTransfersResult
                Truth.assertThat(result).isNotNull()
                Truth.assertThat(result?.isSuccess).isTrue()
            }
        }

    @Test
    fun `test that cancelTransfersResult update correctly when call cancelAllTransfers failed`() =
        runTest {
            whenever(cancelTransfersUseCase()).thenThrow(RuntimeException::class.java)
            underTest.cancelAllTransfers()
            underTest.state.test {
                val result = awaitItem().cancelTransfersResult
                Truth.assertThat(result).isNotNull()
                Truth.assertThat(result?.isFailure).isTrue()
            }
        }

    @Test
    fun `test that cancelTransfersResult is reset to null after all transfers get canceled`() =
        runTest {
            underTest.state.test {
                underTest.onCancelTransfersResultConsumed()
                val newValue = expectMostRecentItem()
                Truth.assertThat(newValue.cancelTransfersResult).isNull()
            }
        }

    @ParameterizedTest(name = "call pauseOrResumeTransfers {0}")
    @ValueSource(booleans = [true, false])
    fun `test that pauseOrResultResult update correctly when call pauseOrResumeTransfers successfully`(
        isPause: Boolean,
    ) = runTest {
        whenever(pauseTransfersQueueUseCase(isPause)).thenReturn(isPause)
        underTest.pauseOrResumeTransfers(isPause)
        underTest.state.test {
            val result = awaitItem().pauseOrResultResult
            Truth.assertThat(result).isNotNull()
            Truth.assertThat(result?.isSuccess).isTrue()
            Truth.assertThat(result?.getOrThrow()).isEqualTo(isPause)
        }
    }

    @ParameterizedTest(name = "call pauseOrResumeTransfers {0}")
    @ValueSource(booleans = [true, false])
    fun `test that pauseOrResultResult update correctly when call pauseOrResumeTransfers failed`(
        isPause: Boolean,
    ) = runTest {
        whenever(pauseTransfersQueueUseCase(isPause)).thenThrow(RuntimeException::class.java)
        underTest.pauseOrResumeTransfers(isPause)
        underTest.state.test {
            val result = awaitItem().pauseOrResultResult
            Truth.assertThat(result).isNotNull()
            Truth.assertThat(result?.isFailure).isTrue()
        }
    }

    @Test
    fun `test that deleteFailedOrCancelledTransfersResult update correctly when call deleteFailedOrCancelledTransfers successfully`() =
        runTest {
            val transfer = mock<CompletedTransfer>()
            whenever(deleteFailedOrCancelledTransfersUseCase()).thenReturn(listOf(transfer))
            underTest.deleteFailedOrCancelledTransfers()
            underTest.state.test {
                val result = awaitItem().deleteFailedOrCancelledTransfersResult
                Truth.assertThat(result).isNotNull()
                Truth.assertThat(result?.isSuccess).isTrue()
            }
        }

    @Test
    fun `test that deleteFailedOrCancelledTransfersResult update correctly when call deleteFailedOrCancelledTransfers failed`() =
        runTest {
            whenever(deleteFailedOrCancelledTransfersUseCase()).thenThrow(RuntimeException::class.java)
            underTest.deleteFailedOrCancelledTransfers()
            underTest.state.test {
                val result = awaitItem().deleteFailedOrCancelledTransfersResult
                Truth.assertThat(result).isNotNull()
                Truth.assertThat(result?.isFailure).isTrue()
            }
        }

    @Test
    fun `test that deleteFailedOrCancelledTransfersResult null after call markDeleteFailedOrCancelledTransferResultConsumed`() =
        runTest {
            underTest.markDeleteFailedOrCancelledTransferResultConsumed()
            underTest.state.test {
                val newValue = awaitItem()
                Truth.assertThat(newValue.deleteFailedOrCancelledTransfersResult).isNull()
            }
        }

    @Test
    fun `test that stopCameraUploads calls stopCameraUploadsUseCase with restartMode StopAndDisable`() =
        runTest {
            underTest.stopCameraUploads()
            verify(stopCameraUploadsUseCase).invoke(restartMode = CameraUploadsRestartMode.StopAndDisable)
        }

    @Test
    fun `test that stopCameraUploads catch the error if stopCameraUploadsUseCase throws an error`() =
        runTest {
            whenever(stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable))
                .thenThrow(RuntimeException::class.java)

            assertDoesNotThrow { underTest.stopCameraUploads() }
        }
}
