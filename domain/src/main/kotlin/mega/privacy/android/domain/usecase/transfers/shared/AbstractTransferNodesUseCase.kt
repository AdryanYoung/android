package mega.privacy.android.domain.usecase.transfers.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.isAlreadyTransferredEvent
import mega.privacy.android.domain.entity.transfer.isFileTransfer
import mega.privacy.android.domain.entity.transfer.isFinishScanningEvent
import mega.privacy.android.domain.entity.transfer.isTransferUpdated
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase

/**
 * Helper class to implement common logic for transfer multiple items (upload or download)
 * @param T type of the items to be transferred
 * @param R type of the items key to match the item with the related transfer
 */
abstract class AbstractTransferNodesUseCase<T, R>(
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    private val handleTransferEventUseCase: HandleTransferEventUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
) {

    internal abstract fun generateIdFromItem(item: T): R
    internal abstract fun generateIdFromTransferEvent(transferEvent: TransferEvent): R

    internal fun commonInvoke(
        items: List<T>,
        beforeStartTransfer: (suspend () -> Unit)?,
        doTransfer: suspend (T) -> Flow<TransferEvent>,
    ): Flow<MultiTransferEvent> {
        val itemsScanned =
            mutableSetOf<R>() //to check if all [items] have been scanned (children not needed here)
        val itemsUpdated =
            mutableSetOf<R>() // to check if all [items] have been updated (children not needed here)
        val filesStarted =
            mutableSetOf<R>() //to count the number of files that have been started (no folders but including children)
        val alreadyTransferredFiles = mutableSetOf<R>()
        val alreadyTransferredNodeIds = mutableSetOf<NodeId>()
        val allIds = items.map(::generateIdFromItem)
        var scanningFinished = false
        var allTransfersUpdated = false
        return channelFlow {
            monitorTransferEvents()
            //start all transfers in parallel
            items.map { item ->
                launch {
                    doTransfer(item)
                        .catch { cause ->
                            val id = generateIdFromItem(item)
                            if (cause is NodeDoesNotExistsException) {
                                send(MultiTransferEvent.TransferNotStarted(id, cause))
                            }
                            itemsScanned.add(id)
                            itemsUpdated.add(id)
                        }
                        .buffer(capacity = Channel.UNLIMITED)
                        .collect { transferEvent ->
                            totalBytesMap[transferEvent.transfer.tag] =
                                transferEvent.transfer.totalBytes
                            transferredBytesMap[transferEvent.transfer.tag] =
                                transferEvent.transfer.transferredBytes

                            if (transferEvent is TransferEvent.TransferStartEvent) {
                                rootTags += transferEvent.transfer.tag
                            }
                            //update active transfers db, sd transfers, etc.
                            handleTransferEventUseCase(transferEvent)

                            //keep track of file counters
                            if (transferEvent.isFileTransfer) {
                                val id = generateIdFromTransferEvent(transferEvent)
                                filesStarted.add(id)
                                if (transferEvent.isAlreadyTransferredEvent) {
                                    alreadyTransferredFiles.add(id)
                                    alreadyTransferredNodeIds.add(NodeId(transferEvent.transfer.nodeHandle))
                                }
                            }
                            //check if is a single node scanning finish event
                            if (!scanningFinished && transferEvent.isFinishScanningEvent) {
                                val id = generateIdFromTransferEvent(transferEvent)
                                if (!itemsScanned.contains(id)) {
                                    //this node is already scanned: save it and emit the event
                                    itemsScanned.add(id)

                                    //check if all nodes have been scanned
                                    if (itemsScanned.containsAll(allIds)) {
                                        scanningFinished = true
                                        invalidateCancelTokenUseCase() //we need to avoid a future cancellation from now on
                                    }
                                }
                            }
                            //check if is a single node update event, at this point we can know if the transfer will be skipped by the sdk because it has ben already downloaded.
                            if (!allTransfersUpdated && transferEvent.isTransferUpdated) {
                                val id = generateIdFromTransferEvent(transferEvent)
                                if (!itemsUpdated.contains(id)) {
                                    itemsUpdated.add(id)
                                    if (itemsUpdated.containsAll(allIds)) {
                                        allTransfersUpdated = true
                                    }
                                }
                            }

                            send(
                                MultiTransferEvent.SingleTransferEvent(
                                    transferEvent = transferEvent,
                                    totalBytesTransferred = transferredBytes,
                                    totalBytesToTransfer = totalBytes,
                                    startedFiles = filesStarted.size,
                                    alreadyTransferred = alreadyTransferredFiles.size,
                                    alreadyTransferredIds = alreadyTransferredNodeIds,
                                    scanningFinished = scanningFinished || allTransfersUpdated,
                                    allTransfersUpdated = allTransfersUpdated,
                                )
                            )
                        }
                }
            }.joinAll()
            close()
        }
            .onStart {
                beforeStartTransfer?.invoke()
            }
            .onCompletion {
                runCatching { cancelCancelTokenUseCase() }
            }.cancellable()
    }

    /**
     * tags of the transfers directly initiated by a sdk call, so we can check children transfers of all nodes
     */
    private val rootTags = mutableListOf<Int>()

    /**
     * total bytes for each transfer directly initiated by a sdk call, so we can compute the sum of all nodes
     */
    private val totalBytesMap = mutableMapOf<Int, Long>()
    private val totalBytes get() = totalBytesMap.values.sum()

    /**
     * total transferredBytes for each transfer directly initiated by a sdk call, so we can compute the sum of all nodes
     */
    private val transferredBytesMap = mutableMapOf<Int, Long>()
    private val transferredBytes get() = transferredBytesMap.values.sum()

    /**
     * Monitors download child transfer global events and update the related active transfers
     */
    private fun CoroutineScope.monitorTransferEvents() =
        this.launch {
            monitorTransferEventsUseCase()
                .filter { event ->
                    //only children as events of the related nodes are already handled
                    event.transfer.folderTransferTag?.let { rootTags.contains(it) } == true
                }
                .collect { transferEvent ->
                    withContext(NonCancellable) {
                        handleTransferEventUseCase(transferEvent)
                    }
                }
        }

}