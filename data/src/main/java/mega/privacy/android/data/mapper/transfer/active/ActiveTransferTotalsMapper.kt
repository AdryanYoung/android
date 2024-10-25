package mega.privacy.android.data.mapper.transfer.active

import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import javax.inject.Inject

/**
 * Mapper for converting a list of [ActiveTransferEntity] into [ActiveTransferTotals].
 */
internal class ActiveTransferTotalsMapper @Inject constructor() {
    operator fun invoke(
        type: TransferType,
        list: List<ActiveTransfer>,
        transferredBytes: Map<Int, Long>,
    ): ActiveTransferTotals {
        val onlyFiles = list.filter { !it.isFolderTransfer }
        return ActiveTransferTotals(
            transfersType = type,
            totalTransfers = list.size,
            totalFileTransfers = onlyFiles.size,
            pausedFileTransfers = onlyFiles.count { it.isPaused },
            totalFinishedTransfers = list.count { it.isFinished },
            totalFinishedFileTransfers = onlyFiles.count { it.isFinished },
            totalCompletedFileTransfers = onlyFiles.count { it.isFinished && transferredBytes[it.tag] == it.totalBytes },
            totalBytes = onlyFiles.sumOf { it.totalBytes },
            transferredBytes = onlyFiles.sumOf {
                //if it's finished always totalBytes as it can be cancelled or failed
                if (it.isFinished) it.totalBytes else transferredBytes[it.tag] ?: 0L
            },
            totalAlreadyTransferredFiles = onlyFiles.count { it.isAlreadyTransferred },
            totalCancelled = list.count{it.isCancelled},
        )
    }
}