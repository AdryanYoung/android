package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.exception.MegaSyncException
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue

/**
 * Repository for syncing folder pairs
 *
 */
interface SyncRepository {

    /**
     * Establishes a pair between local and remote directories and starts the syncing process
     *
     * @param syncType Sync type of the folder pair
     * @param name Name of the folder pair
     * @param localPath Local path on the device
     * @param remoteFolderId MEGA folder handle
     * @return The backup ID in case of folder pair was set up successfully or null otherwise
     */
    suspend fun setupFolderPair(
        syncType: SyncType,
        name: String?,
        localPath: String,
        remoteFolderId: Long,
    ): Long?

    /**
     * Returns all setup folder pairs.
     */
    suspend fun getFolderPairs(): List<FolderPair>

    /**
     * Returns the folder pair with the given id.
     *
     * @param folderPairId The id of the folder pair to retrieve.
     */
    suspend fun removeFolderPair(folderPairId: Long)

    /**
     * Pauses the syncing process for the given folder pair.
     *
     * @param folderPairId The id of the folder pair to pause.
     */
    suspend fun pauseSync(folderPairId: Long)

    /**
     * Resumes the syncing process for the given folder pair.
     *
     * @param folderPairId The id of the folder pair to resume.
     */
    suspend fun resumeSync(folderPairId: Long)

    /**
     * Monitors the syncing process for the given folder pair.
     *
     * @return [Flow<Unit>]Returns the folder pair with the given id.
     */
    val syncChanges: Flow<MegaSyncListenerEvent>

    /**
     * Gets the list of stalled issues.
     * @return [List<StalledIssue>] Returns the stalled issues.
     */
    suspend fun getSyncStalledIssues(): List<StalledIssue>

    /**
     * Monitors the list of stalled issues.
     * @return [Flow<List<StalledIssue>>] Returns the stalled issues.
     */
    fun monitorStalledIssues(): Flow<List<StalledIssue>>

    /**
     * Monitors the list of folder pairs.
     *
     * @return [Flow<List<FolderPair>>] Returns the folder pairs.
     */
    fun monitorFolderPairChanges(): Flow<List<FolderPair>>

    /**
     * Refreshes Sync list
     */
    suspend fun refreshSync()

    /**
     * Checks if a node is syncable. If it is not, returns an error.
     *
     * @param nodeId The id of the node to check.
     */
    @Throws(MegaSyncException::class)
    suspend fun tryNodeSync(nodeId: NodeId)

    /**
     * Starts a periodic sync worker that will sync folders when the app is closed
     * with specified frequency
     *
     * @param frequencyInMinutes The frequency in minutes to run the sync worker
     * @param wifiOnly If the sync should be done only when connected to WiFi
     */
    suspend fun startSyncWorker(frequencyInMinutes: Int, wifiOnly: Boolean)

    /**
     * Stops the sync worker
     */
    suspend fun stopSyncWorker()

    /**
     * Change the local path that is being used as root for a sync.
     *
     * @param syncBackupId - id of the folder pair to change
     * @param newLocalSyncRootUri - new local uri to be used as root for the sync
     */
    suspend fun changeSyncLocalRoot(
        syncBackupId: Long,
        newLocalSyncRootUri: String,
    ): Long?
}
