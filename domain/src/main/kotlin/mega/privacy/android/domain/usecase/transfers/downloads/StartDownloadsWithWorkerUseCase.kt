package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.exception.LocalStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsExternalStorageContentUriUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractStartTransfersWithWorkerUseCase
import java.io.File
import javax.inject.Inject


/**
 * Start downloading a list of nodes to the specified path and returns a Flow to monitor the progress until the nodes are scanned.
 * While the returned flow is not completed the app should be blocked to avoid other interaction with the sdk to avoid issues
 * Once the flow is completed the sdk will keep downloading and a DownloadWorker will monitor updates globally.
 * If cancelled before completion the processing of the nodes will be cancelled
 */
class StartDownloadsWithWorkerUseCase @Inject constructor(
    private val doesPathHaveSufficientSpaceForNodesUseCase: DoesPathHaveSufficientSpaceForNodesUseCase,
    private val downloadNodesUseCase: DownloadNodesUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val transferRepository: TransferRepository,
    private val startDownloadsWorkerAndWaitUntilIsStartedUseCase: StartDownloadsWorkerAndWaitUntilIsStartedUseCase,
    private val getExternalPathByContentUriUseCase: GetExternalPathByContentUriUseCase,
    private val isExternalStorageContentUriUseCase: IsExternalStorageContentUriUseCase,
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
) : AbstractStartTransfersWithWorkerUseCase(
    cancelCancelTokenUseCase,
) {
    /**
     * Invoke
     * @param nodes The desired nodes to download
     * @param destinationPathOrUri Full path to the destination folder of [nodes]. If this path does not exist it will try to create it.
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [MultiTransferEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        nodes: List<TypedNode>,
        destinationPathOrUri: String,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> {
        if (destinationPathOrUri.isEmpty()) {
            return nodes.asFlow().map { MultiTransferEvent.TransferNotStarted(it.id, null) }
        }
        //wrap the downloadNodesUseCase flow to be able to execute suspended functions
        return flow {
            val appData: TransferAppData?
            val destinationPathForSdk: String?
            when {
                fileSystemRepository.isSDCardPath(destinationPathOrUri)
                        || fileSystemRepository.isContentUri(destinationPathOrUri) -> {
                    destinationPathForSdk =
                        transferRepository.getOrCreateSDCardTransfersCacheFolder()?.path?.ensureEndsWithFileSeparator()
                    appData =
                        TransferAppData.SdCardDownload(destinationPathOrUri, destinationPathOrUri)
                }

                isExternalStorageContentUriUseCase(destinationPathOrUri) -> {
                    appData = null
                    destinationPathForSdk =
                        getExternalPathByContentUriUseCase(destinationPathOrUri)?.ensureEndsWithFileSeparator()
                }

                else -> {
                    appData = null
                    destinationPathForSdk = destinationPathOrUri.ensureEndsWithFileSeparator()
                }
            }
            if (destinationPathForSdk == null) {
                nodes.forEach {
                    emit(
                        MultiTransferEvent.TransferNotStarted(
                            it.id,
                            LocalStorageException(null, null)
                        )
                    )
                }
                return@flow
            }
            fileSystemRepository.createDirectory(destinationPathForSdk)
            if (!doesPathHaveSufficientSpaceForNodesUseCase(destinationPathForSdk, nodes)) {
                emit(MultiTransferEvent.InsufficientSpace)
            } else {
                emitAll(
                    startTransfersAndThenWorkerFlow(
                        doTransfers = {
                            downloadNodesUseCase(
                                nodes,
                                destinationPathForSdk,
                                appData = listOfNotNull(appData),
                                isHighPriority = isHighPriority
                            )
                        },
                        startWorker = {
                            startDownloadsWorkerAndWaitUntilIsStartedUseCase()
                        },
                    )
                )
            }
        }
    }

    private fun String.ensureEndsWithFileSeparator() =
        if (this.endsWith(File.separator)) {
            this
        } else {
            this.plus(File.separator)
        }
}