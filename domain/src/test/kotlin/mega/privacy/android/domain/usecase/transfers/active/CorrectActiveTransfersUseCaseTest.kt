package mega.privacy.android.domain.usecase.transfers.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CorrectActiveTransfersUseCaseTest {
    private lateinit var underTest: CorrectActiveTransfersUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getInProgressTransfersUseCase = mock<GetInProgressTransfersUseCase>()
    private val mockedActiveTransfers = (0..10).map { mock<ActiveTransfer>() }
    private val mockedTransfers = (0..10).map { mock<Transfer>() }

    @BeforeAll
    fun setUp() {
        underTest = CorrectActiveTransfersUseCase(
            getInProgressTransfersUseCase = getInProgressTransfersUseCase,
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository,
            getInProgressTransfersUseCase,
            *mockedActiveTransfers.toTypedArray(),
            *mockedTransfers.toTypedArray(),
        )
    }

    private fun stubActiveTransfers(areFinished: Boolean) {
        mockedActiveTransfers.forEachIndexed { index, activeTransfer ->
            whenever(activeTransfer.tag).thenReturn(index)
            whenever(activeTransfer.isFinished).thenReturn(areFinished)
        }
    }

    private fun stubTransfers() {
        mockedTransfers.forEachIndexed { index, transfer ->
            whenever(transfer.tag).thenReturn(index)
        }
    }

    private fun subSetTransfers() = mockedTransfers.filter { it.tag.mod(2) == 0 }
    private fun subSetActiveTransfers() = mockedActiveTransfers.filter { it.tag.mod(3) == 0 }

    @Test
    fun `test that active transfers not finished and not in progress are set as cancelled`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            val inProgress = subSetTransfers()
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            val expected =
                mockedActiveTransfers.map { it.tag } - inProgress.map { it.tag }.toSet()
            Truth.assertThat(expected).isNotEmpty()
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository).setActiveTransferAsCancelledByTag(expected)
        }

    @Test
    fun `test that active transfers finished and not in progress are not set as cancelled`() =
        runTest {
            stubActiveTransfers(true)
            stubTransfers()
            val inProgress = subSetTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository, never()).setActiveTransferAsCancelledByTag(anyOrNull())
        }

    @Test
    fun `test that in progress transfers not in active transfers are added`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            val alreadyInDataBase = subSetActiveTransfers()
            val notInDataBase = mockedActiveTransfers - alreadyInDataBase.toSet()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(alreadyInDataBase)
            whenever(getInProgressTransfersUseCase()).thenReturn(mockedTransfers)
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository).insertOrUpdateActiveTransfers(argThat { it ->
                it.map { it.tag } == notInDataBase.map { it.tag }
            })
        }

    @Test
    fun `test that active transfers not finished and not in progress are removed as in progress transfers`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            val inProgress = subSetTransfers()
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            val expected = mockedActiveTransfers.filter { transfer ->
                !inProgress.map { it.tag }.contains(transfer.tag)
            }
            Truth.assertThat(expected).isNotEmpty()
            underTest(TransferType.GENERAL_UPLOAD)

            verify(transferRepository).removeInProgressTransfers(expected.map { it.tag }.toSet())
        }

    @Test
    fun `test that in progress transfers not in active transfers are updated in in progress transfers`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            val alreadyInDataBase = subSetActiveTransfers()
            val notInDataBase = mockedActiveTransfers - alreadyInDataBase.toSet()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(alreadyInDataBase)
            whenever(getInProgressTransfersUseCase()).thenReturn(mockedTransfers)
            underTest(TransferType.GENERAL_UPLOAD)

            verify(transferRepository).updateInProgressTransfers(
                argThat { this.map { it.tag } == notInDataBase.map { it.tag } }
            )
        }
}