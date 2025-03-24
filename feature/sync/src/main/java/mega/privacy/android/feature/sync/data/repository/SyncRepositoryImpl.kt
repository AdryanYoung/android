package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.backup.SyncErrorMapper
import mega.privacy.android.data.mapper.sync.SyncTypeMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.gateway.SyncWorkManagerGateway
import mega.privacy.android.feature.sync.data.mapper.FolderPairMapper
import mega.privacy.android.feature.sync.data.mapper.SyncByWifiToNetworkTypeMapper
import mega.privacy.android.feature.sync.data.mapper.stalledissue.StalledIssuesMapper
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaSyncList
import timber.log.Timber
import javax.inject.Inject

internal class SyncRepositoryImpl @Inject constructor(
    private val syncWorkManagerGateway: SyncWorkManagerGateway,
    private val syncGateway: SyncGateway,
    private val syncStatsCacheGateway: SyncStatsCacheGateway,
    private val megaApiGateway: MegaApiGateway,
    private val folderPairMapper: FolderPairMapper,
    private val stalledIssuesMapper: StalledIssuesMapper,
    private val syncErrorMapper: SyncErrorMapper,
    private val syncTypeMapper: SyncTypeMapper,
    private val syncByWifiToNetworkTypeMapper: SyncByWifiToNetworkTypeMapper,
    private val accountRepository: AccountRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : SyncRepository {

    private val _refreshShow = MutableSharedFlow<Unit>()

    override suspend fun setupFolderPair(
        syncType: SyncType,
        name: String?,
        localPath: String,
        remoteFolderId: Long,
    ): Boolean = withContext(ioDispatcher) {
        syncGateway.syncFolderPair(
            syncType = syncTypeMapper(syncType),
            name = name,
            localPath = localPath,
            remoteFolderId = remoteFolderId
        )
    }

    override suspend fun pauseSync(folderPairId: Long) = withContext(ioDispatcher) {
        syncGateway.pauseSync(folderPairId)
    }

    override suspend fun resumeSync(folderPairId: Long) = withContext(ioDispatcher) {
        syncGateway.resumeSync(folderPairId)
    }

    override suspend fun getFolderPairs(): List<FolderPair> = withContext(ioDispatcher) {
        runCatching {
            mapToDomain(syncGateway.getFolderPairs())
        }
            .onFailure { Timber.e("Syncs fetching error: $it") }
            .getOrElse { emptyList() }
    }

    private suspend fun mapToDomain(model: MegaSyncList): List<FolderPair> =
        (0 until model.size())
            .map { index ->
                val folderPairModel = model.get(index)
                val megaFolderName =
                    megaApiGateway.getMegaNodeByHandle(folderPairModel.megaHandle)?.name ?: ""
                val syncStats = syncStatsCacheGateway.getSyncStatsById(folderPairModel.backupId)
                val storageUsedPercentage =
                    (100 * accountRepository.getUsedStorage() / accountRepository.getMaxStorage()).toInt()
                folderPairMapper(
                    model = folderPairModel,
                    megaFolderName = megaFolderName,
                    syncStats = syncStats,
                    isStorageOverQuota = storageUsedPercentage >= FULL_STORAGE_PERCENTAGE,
                )
            }

    override suspend fun removeFolderPair(folderPairId: Long) = withContext(ioDispatcher) {
        syncGateway.removeFolderPair(folderPairId)
    }


    private val _syncChanges by lazy {
        merge(
            megaApiGateway
                .globalUpdates.filter { it is GlobalUpdate.OnGlobalSyncStateChanged }
                .map { MegaSyncListenerEvent.OnGlobalSyncStateChanged },
            syncGateway.syncUpdate
                .onEach {
                    if (it is MegaSyncListenerEvent.OnSyncStatsUpdated) {
                        syncStatsCacheGateway.setSyncStats(it.syncStats)
                    }
                },
            _refreshShow.map {
                MegaSyncListenerEvent.OnRefreshSyncState
            }
        ).flowOn(ioDispatcher)
            .shareIn(appScope, SharingStarted.Eagerly)
    }
    override val syncChanges: Flow<MegaSyncListenerEvent> = _syncChanges

    override suspend fun getSyncStalledIssues(): List<StalledIssue> = withContext(ioDispatcher) {
        runCatching {
            syncGateway.getSyncStalledIssues()?.let { stalledIssues ->
                stalledIssuesMapper(
                    syncs = monitorFolderPairChanges().first(),
                    stalledIssues = stalledIssues
                )
            }.orEmpty()
        }
            .onFailure { Timber.e("Stalled Issues fetching error: $it") }
            .getOrElse { emptyList() }
    }

    private val _syncStalledIssues by lazy {
        _syncChanges
            .onEach {
                delay(SYNC_REFRESH_DELAY)
            }
            .map { getSyncStalledIssues() }
            .flowOn(ioDispatcher)
            .shareIn(appScope, SharingStarted.Eagerly, replay = 1)
    }

    override fun monitorStalledIssues() = _syncStalledIssues

    private val _folderPair by lazy {
        _syncChanges
            .map { getFolderPairs() }
            .onStart { emit(getFolderPairs()) }
            .flowOn(ioDispatcher)
            .shareIn(appScope, SharingStarted.Eagerly, replay = 1)
    }

    override fun monitorFolderPairChanges() = _folderPair

    override suspend fun refreshSync() {
        _refreshShow.emit(Unit)
    }

    @Throws(MegaSyncException::class)
    override suspend fun tryNodeSync(nodeId: NodeId) {
        val megaNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
            ?: throw MegaSyncException(
                errorCode = MegaError.API_EARGS,
                "Node not found"
            )
        val error = syncGateway.isNodeSyncableWithError(megaNode)

        if (error.errorCode != MegaError.API_OK) {
            val syncError = syncErrorMapper(error.syncError)

            throw MegaSyncException(
                error.errorCode,
                error.errorString,
                syncError = syncError
            )
        }
    }

    override suspend fun startSyncWorker(frequencyInMinutes: Int, wifiOnly: Boolean) {
        val networkType = syncByWifiToNetworkTypeMapper(wifiOnly)
        syncWorkManagerGateway.enqueueSyncWorkerRequest(frequencyInMinutes, networkType)
    }

    override suspend fun stopSyncWorker() {
        syncWorkManagerGateway.cancelSyncWorkerRequest()
    }

    private companion object {
        /**
         * Delay to ensure two things:
         * 1. correct stalled issues are loaded on app start (without the delay
         * we would get empty issues list first before getting the correct one)
         * 2. Prevent the situation when the SDK is too fast detecting
         * issues that are later resolved by the following sync loop.
         */
        const val SYNC_REFRESH_DELAY = 5000L

        const val FULL_STORAGE_PERCENTAGE = 100
    }
}