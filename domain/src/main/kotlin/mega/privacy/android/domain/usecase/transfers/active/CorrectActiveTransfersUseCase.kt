package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import javax.inject.Inject

/**
 * To ensure that the local database accurately reflects the current state of transfers by retrieving and updating transfer status information from the SDK.
 * This process is necessary to rectify any misaligned states resulting from potential event loss, such as finish, cancel, or start of a transfer,
 * we need to fix it to avoid outdated counters in [ActiveTransferTotals]
 */
class CorrectActiveTransfersUseCase @Inject constructor(
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     * @param transferType the transfer type we want to check, or null if we want to check all of them
     */
    suspend operator fun invoke(transferType: TransferType?) {
        val activeTransfers = if (transferType == null) {
            transferRepository.getCurrentActiveTransfers()
        } else {
            transferRepository.getCurrentActiveTransfersByType(transferType)
        }
        val inProgressTransfers = getInProgressTransfersUseCase()

        //update transferred bytes for each transfer
        inProgressTransfers.forEach {
            transferRepository.updateTransferredBytes(it)
        }

        //set not-in-progress active transfers as finished, this can happen if we missed a finish event from SDK
        val notInProgressActiveTransfersTags = activeTransfers
            .filter { activeTransfer ->
                !activeTransfer.isFinished && !inProgressTransfers.map { it.tag }
                    .contains(activeTransfer.tag)
            }
            .map {
                it.tag
            }
        if (notInProgressActiveTransfersTags.isNotEmpty()) {
            //we are not sure if they have been cancelled or not, but at this point it has more sense to don't show the completed status in transfer widget, so it's better to just finish it as cancelled
            transferRepository.setActiveTransferAsCancelledByTag(notInProgressActiveTransfersTags)
            transferRepository.removeInProgressTransfers(notInProgressActiveTransfersTags.toSet())
        }

        //add in-progress active transfers if they are not added, this can happen if we missed a start event from SDK
        val inProgressNotInActiveTransfers = inProgressTransfers.filterNot { transfer ->
            activeTransfers.map { it.tag }.contains(transfer.tag)
        }

        transferRepository.updateInProgressTransfers(inProgressNotInActiveTransfers)
        transferRepository.insertOrUpdateActiveTransfers(inProgressNotInActiveTransfers)
    }
}