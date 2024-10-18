package mega.privacy.android.domain.usecase.transfers.downloads

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.CancelTokenRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.internal.verification.Times
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadNodesUseCaseTest {

    private val transferRepository: TransferRepository = mock()
    private val cancelTokenRepository: CancelTokenRepository = mock()
    private val fileNode: TypedFileNode = mock()
    private val folderNode: TypedFolderNode = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()
    private val transfer: Transfer = mock()
    private val handleTransferEventUseCase: HandleTransferEventUseCase = mock()
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()

    private lateinit var underTest: DownloadNodesUseCase

    @BeforeAll
    fun setup() {
        underTest =
            DownloadNodesUseCase(
                cancelCancelTokenUseCase = cancelCancelTokenUseCase,
                invalidateCancelTokenUseCase = invalidateCancelTokenUseCase,
                handleTransferEventUseCase = handleTransferEventUseCase,
                monitorTransferEventsUseCase = monitorTransferEventsUseCase,
                transferRepository = transferRepository,
                fileSystemRepository = fileSystemRepository,
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository, cancelTokenRepository, fileSystemRepository,
            handleTransferEventUseCase, fileNode, folderNode, invalidateCancelTokenUseCase,
            cancelCancelTokenUseCase, transfer,
            monitorTransferEventsUseCase,
        )
        commonStub()
    }

    private fun commonStub() {
        whenever(monitorTransferEventsUseCase()).thenReturn(emptyFlow())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @ParameterizedTest(name = "priority: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that repository start download is called with the proper priority`(priority: Boolean) =
        runTest {
            underTest(fileNodes, DESTINATION_PATH_FOLDER, null, priority).test {
                fileNodes.forEach { nodeId ->
                    verify(transferRepository).startDownload(
                        nodeId,
                        DESTINATION_PATH_FOLDER,
                        null,
                        priority,
                    )
                }
                awaitComplete()
            }
        }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @MethodSource("provideAppData")
    fun `test that repository start download is called with the proper appData`(
        appData: List<TransferAppData>?,
    ) = runTest {
        underTest(listOf(node), DESTINATION_PATH_FOLDER, appData, false).test {
            verify(transferRepository).startDownload(
                node,
                DESTINATION_PATH_FOLDER,
                appData,
                false

            )
            awaitComplete()
        }
    }

    @Test
    fun `test that flow is closed when all downloads are finished`() = runTest {
        fileNodes.forEach { node ->
            stubFinishScanningEvent(node)
        }
        whenever(monitorTransferEventsUseCase()).thenReturn(flow {
            awaitCancellation()
        })
        underTest(fileNodes, DESTINATION_PATH_FOLDER, null, false)
            .test {
                assertThat(cancelAndConsumeRemainingEvents().last()).isInstanceOf(Event.Complete::class.java)
            }
    }

    private fun provideAppData() = listOf(
        listOf(TransferAppData.BackgroundTransfer),
        listOf(TransferAppData.SdCardDownload("target", null)),
        listOf(TransferAppData.CameraUpload),
        listOf(TransferAppData.VoiceClip),
        listOf(TransferAppData.ChatUpload(12345L)),
        listOf(TransferAppData.BackgroundTransfer, TransferAppData.CameraUpload),
    )


    @Test
    fun `test that repository start download is invoked for each nodeId when start download is invoked`() =
        runTest {
            underTest(fileNodes, DESTINATION_PATH_FOLDER, null, false).test {
                fileNodes.forEach { nodeId ->
                    verify(transferRepository).startDownload(
                        nodeId, DESTINATION_PATH_FOLDER, null, false,
                    )
                }
                awaitComplete()
            }
        }

    @Test
    fun `test that cancel token is canceled when start download flow is canceled`() =
        runTest {
            fileNodes.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(
                    flow { delay(100) }
                )
            }
            underTest(fileNodes, DESTINATION_PATH_FOLDER, null, false).test {
                cancel()
                verify(cancelCancelTokenUseCase).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not invalidated when start download flow is canceled before completion`() =
        runTest {
            fileNodes.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(
                    flow { delay(100) }
                )
            }
            underTest(fileNodes, DESTINATION_PATH_FOLDER, null, false).test {
                cancel()
                verify(invalidateCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not canceled if start download flow is not completed`() =
        runTest {
            fileNodes.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(
                    flow { delay(100) }
                )
            }
            underTest(fileNodes, DESTINATION_PATH_FOLDER, null, false).test {
                verify(cancelCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that transfer single node events are emitted when each transfer is updated`() =
        runTest {
            whenever(transfer.isFolderTransfer).thenReturn(false)
            val flow = flowOf(
                mock<TransferEvent.TransferStartEvent> { on { it.transfer }.thenReturn(transfer) },
                mock<TransferEvent.TransferUpdateEvent> { on { it.transfer }.thenReturn(transfer) },
                mock<TransferEvent.TransferFinishEvent> { on { it.transfer }.thenReturn(transfer) },
            )
            fileNodes.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(flow)
            }
            underTest(
                fileNodes,
                DESTINATION_PATH_FOLDER,
                null,
                false
            ).filterIsInstance<MultiTransferEvent.SingleTransferEvent>().test {
                repeat(nodeIds.size) {
                    assertThat(awaitItem().transferEvent)
                        .isInstanceOf(TransferEvent.TransferStartEvent::class.java)
                    assertThat(awaitItem().transferEvent)
                        .isInstanceOf(TransferEvent.TransferUpdateEvent::class.java)
                    assertThat(awaitItem().transferEvent)
                        .isInstanceOf(TransferEvent.TransferFinishEvent::class.java)
                }
                awaitComplete()
            }
        }

    @Test
    fun `test that addOrUpdateActiveTransferUseCase is invoked when each transfer is updated`() =
        runTest {
            whenever(transfer.isFolderTransfer).thenReturn(false)
            val flow = flowOf(
                mock<TransferEvent.TransferStartEvent> { on { it.transfer }.thenReturn(transfer) },
                mock<TransferEvent.TransferUpdateEvent> { on { it.transfer }.thenReturn(transfer) },
                mock<TransferEvent.TransferFinishEvent> { on { it.transfer }.thenReturn(transfer) },
            )
            fileNodes.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(flow)
            }
            underTest(
                fileNodes,
                DESTINATION_PATH_FOLDER,
                null,
                false
            ).filterIsInstance<MultiTransferEvent.SingleTransferEvent>().test {
                cancelAndConsumeRemainingEvents()
            }
            verify(
                handleTransferEventUseCase,
                Times(nodeIds.size * flow.count())
            ).invoke(anyVararg())
        }

    @Test
    fun `test that single event with is finish scanning event flag to true is emitted when each node finishes it is scanned`() =
        runTest {
            val transfers = folderNodes.associateWith { node ->
                val handle = node.id.longValue
                mock<Transfer> {
                    on { isFolderTransfer }.thenReturn(true)
                    on { nodeHandle }.thenReturn(handle)
                }
            }
            val flows = folderNodes.associateWith { nodeId ->
                flowOf(
                    mock<TransferEvent.TransferFinishEvent> {
                        on { it.transfer }.thenReturn(transfers[nodeId])
                    }
                )
            }
            transfers.keys.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(flows[it])
            }
            underTest(
                folderNodes,
                DESTINATION_PATH_FOLDER,
                null,
                false
            )
                .filterIsInstance<MultiTransferEvent.SingleTransferEvent>()
                .test {
                    nodeIds.forEach {
                        assertThat(awaitItem().transferEvent.transfer.nodeHandle).isEqualTo(it.longValue)
                    }
                    awaitComplete()
                }
        }


    @Test
    fun `test that an event with a true scanningFinished is emitted when all transfers are scanned`() =
        runTest {
            fileAndFolderNodes.forEach { node ->
                stubFinishScanningEvent(node)
            }

            underTest(fileAndFolderNodes, DESTINATION_PATH_FOLDER, null, false).test {
                assertThat(cancelAndConsumeRemainingEvents().mapNotNull { event ->
                    (event as? Event.Item)?.value?.takeIf { (it as? MultiTransferEvent.SingleTransferEvent)?.scanningFinished == true }
                }).hasSize(1)
            }
        }

    @Test
    fun `test that an event with a true allNodesUpdated is emitted when all transfers are updated`() =
        runTest {
            fileAndFolderNodes.forEach { node ->
                stubUpdatedEvent(node)
            }

            underTest(fileAndFolderNodes, DESTINATION_PATH_FOLDER, null, false).test {
                assertThat(cancelAndConsumeRemainingEvents().mapNotNull { event ->
                    (event as? Event.Item)?.value?.takeIf { (it as? MultiTransferEvent.SingleTransferEvent)?.allTransfersUpdated == true }
                }).hasSize(1)
            }
        }

    @Test
    fun `test that startDownload runs in parallel when there are more than one node`() = runTest {
        fileAndFolderNodes.forEach {
            whenever(
                transferRepository.startDownload(
                    it, DESTINATION_PATH_FOLDER, null, false,
                )
            ).thenAnswer {
                flow<TransferEvent> { delay(100) }
            }
        }
        underTest(fileAndFolderNodes, DESTINATION_PATH_FOLDER, null, false).test {
            fileAndFolderNodes.forEach {
                verify(transferRepository).startDownload(it, DESTINATION_PATH_FOLDER, null, false)
            }
        }
    }

    @Test
    fun `test that scanning folders finished is emitted when all transfers are scanned including not found nodes`() =
        runTest {
            fileAndFolderNodes.forEachIndexed { index, node ->
                if (index == 5) {
                    whenever(
                        transferRepository.startDownload(
                            node, DESTINATION_PATH_FOLDER, null, false,
                        )
                    ).thenAnswer {
                        flow<TransferEvent> { throw NodeDoesNotExistsException() }
                    }
                } else {
                    stubFinishScanningEvent(node)
                }
            }

            underTest(fileAndFolderNodes, DESTINATION_PATH_FOLDER, null, false).test {
                assertThat(cancelAndConsumeRemainingEvents().mapNotNull { event ->
                    (event as? Event.Item)?.value?.takeIf { (it as? MultiTransferEvent.SingleTransferEvent)?.scanningFinished == true }
                }).hasSize(1)
            }
        }

    @Test
    fun `test that destination folder is created`() = runTest {
        underTest(fileNodes, DESTINATION_PATH_FOLDER, null, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(fileSystemRepository).createDirectory(DESTINATION_PATH_FOLDER)
    }

    @Test
    fun `test that monitorTransferEventsUseCase is invoked before starting the download`() =
        runTest {
            val inOrder = inOrder(transferRepository, monitorTransferEventsUseCase)
            underTest(listOf(fileNode), DESTINATION_PATH_FOLDER, null, false).test {
                cancelAndIgnoreRemainingEvents()
            }
            inOrder.verify(monitorTransferEventsUseCase).invoke()
            inOrder.verify(transferRepository).startDownload(any(), any(), anyOrNull(), any())
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that monitorTransferEventsUseCase events are filtered and handled`(childTransferEvent: Boolean) =
        runTest {
            val tag = 1
            val globalEvents = MutableSharedFlow<TransferEvent>()
            val transferEvents = MutableSharedFlow<TransferEvent>()
            whenever(monitorTransferEventsUseCase()).thenReturn(globalEvents)
            whenever(transferRepository.startDownload(any(), any(), anyOrNull(), any())).thenReturn(
                transferEvents
            )
            underTest(listOf(fileNode), DESTINATION_PATH_FOLDER, null, false).test {
                //emits an event to store its tag in the use case
                val transfer = mock<Transfer> {
                    on { this.tag }.thenReturn(tag)
                }
                val event = TransferEvent.TransferStartEvent(transfer)
                transferEvents.emit(event)

                //emits a global event that will be a child transfer depending on the test parameter
                val childEventTransfer = mock<Transfer> {
                    on { folderTransferTag }.thenReturn(tag.takeIf { childTransferEvent })
                    on { transferType }.thenReturn(TransferType.DOWNLOAD)
                }
                val globalEvent = TransferEvent.TransferStartEvent(childEventTransfer)
                globalEvents.emit(globalEvent)

                //verify it's received if corresponds
                if (childTransferEvent) {
                    verify(handleTransferEventUseCase)(globalEvent)
                } else {
                    verify(handleTransferEventUseCase, times(0))(globalEvent)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is invalidated when all transfers are scanned`() = runTest {

        fileAndFolderNodes.forEach { node ->
            stubFinishScanningEvent(node)
        }

        underTest(fileAndFolderNodes, DESTINATION_PATH_FOLDER, null, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(invalidateCancelTokenUseCase).invoke()
    }

    @Test
    fun `test that scanning folders finished event is not emitted if not all transfers are scanned`() =
        runTest {

            fileAndFolderNodes.dropLast(1).forEach { node ->
                stubFinishScanningEvent(node)
            }

            underTest(fileNodes, DESTINATION_PATH_FOLDER, null, false).test {
                assertThat(cancelAndConsumeRemainingEvents().mapNotNull { event ->
                    (event as? Event.Item)?.value?.takeIf { (it as? MultiTransferEvent.SingleTransferEvent)?.scanningFinished == true }
                }).isEmpty()
            }
        }

    private fun stubFinishScanningEvent(node: TypedNode) {
        val handle = node.id.longValue
        whenever(
            transferRepository.startDownload(
                node, DESTINATION_PATH_FOLDER, null, false,
            )
        ).thenAnswer {
            flowOf(
                if (node is FolderNode) {
                    TransferEvent.FolderTransferUpdateEvent(mock {
                        on { nodeHandle }.thenReturn(handle)
                    }, TransferStage.STAGE_TRANSFERRING_FILES, 1, 1, 0, null, null)
                } else {
                    TransferEvent.TransferStartEvent(mock {
                        on { isFolderTransfer }.thenReturn(false)
                        on { nodeHandle }.thenReturn(handle)
                    })
                }
            )
        }
    }

    private fun stubUpdatedEvent(node: TypedNode) {
        val handle = node.id.longValue
        whenever(
            transferRepository.startDownload(
                node, DESTINATION_PATH_FOLDER, null, false,
            )
        ).thenAnswer {
            flowOf(
                if (node is FolderNode) {
                    TransferEvent.FolderTransferUpdateEvent(mock {
                        on { nodeHandle }.thenReturn(handle)
                    }, TransferStage.STAGE_TRANSFERRING_FILES, 1, 1, 0, null, null)
                } else {
                    TransferEvent.TransferUpdateEvent(mock {
                        on { isFolderTransfer }.thenReturn(false)
                        on { nodeHandle }.thenReturn(handle)
                    })
                }
            )
        }
    }


    companion object {
        private val nodeIds = (0L..10L).map { NodeId(it) }
        private val fileNodes = nodeIds.map { nodeId ->
            mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
            }
        }
        private val folderNodes = nodeIds.map { nodeId ->
            mock<TypedFolderNode> {
                on { id }.thenReturn(nodeId)
            }
        }
        private val fileAndFolderNodes: List<TypedNode> = nodeIds.mapIndexed { index, nodeId ->
            //even -> files, odd -> folders
            (if (index.mod(2) == 0) mock<TypedFolderNode>() else mock<TypedFileNode>())
                .also {
                    whenever(it.id).thenReturn(nodeId)
                }
        }
        private val node = fileNodes.first()
        private const val DESTINATION_PATH_FOLDER = "root/parent/destination"
    }
}