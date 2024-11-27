package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.IsProAccountUseCase
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RefreshSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MoveDeconfiguredBackupNodesUseCase
import mega.privacy.android.domain.usecase.node.RemoveDeconfiguredBackupNodesUseCase
import mega.privacy.android.feature.sync.ui.model.StopBackupOption
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncFoldersViewModel @Inject constructor(
    private val syncUiItemMapper: SyncUiItemMapper,
    private val removeFolderPairUseCase: RemoveFolderPairUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val monitorStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val setUserPausedSyncsUseCase: SetUserPausedSyncUseCase,
    private val refreshSyncUseCase: RefreshSyncUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFolderTreeInfo: GetFolderTreeInfo,
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase,
    private val isProAccountUseCase: IsProAccountUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val moveDeconfiguredBackupNodesUseCase: MoveDeconfiguredBackupNodesUseCase,
    private val removeDeconfiguredBackupNodesUseCase: RemoveDeconfiguredBackupNodesUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncFoldersState(emptyList()))
    val uiState: StateFlow<SyncFoldersState> = _uiState.asStateFlow()

    private var showSyncsPausedErrorDialogShown = false

    init {
        checkFeatureFlags()
        viewModelScope.launch {
            runCatching {
                _uiState.update { state -> state.copy(isLoading = true) }
                refreshSyncUseCase()
            }.onSuccess {
                loadSyncs()
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false) }
                Timber.e(it)
            }
        }

        checkOverQuotaStatus()

        viewModelScope.launch {
            monitorBatteryInfoUseCase().collect { batteryInfo ->
                _uiState.update { state ->
                    state.copy(isLowBatteryLevel = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging)
                }
            }
        }

        viewModelScope.launch {
            monitorAccountDetailUseCase().collect { accountDetail ->
                _uiState.update {
                    it.copy(isFreeAccount = accountDetail.levelDetail?.accountType == AccountType.FREE)
                }
                checkOverQuotaStatus()
            }
        }
    }

    private fun checkFeatureFlags() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(SyncFeatures.BackupForAndroid)
            }.onSuccess { isBackupForAndroidEnabled ->
                if (isBackupForAndroidEnabled) {
                    _uiState.update {
                        it.copy(enabledFlags = uiState.value.enabledFlags.plus(SyncFeatures.BackupForAndroid))
                    }
                }
            }.onFailure(Timber::e)
        }
    }

    private fun loadSyncs() {
        monitorSyncsUseCase().catch { Timber.e(it) }.map(syncUiItemMapper::invoke).map { syncs ->
            val stalledIssues = monitorStalledIssuesUseCase().first()
            var numOfFiles = 0
            var numOfFolders = 0
            var totalSizeInBytes = 0L
            var creationTime = 0L
            syncs.map { sync ->

                runCatching {
                    getNodeByIdUseCase(sync.megaStorageNodeId)
                }.onSuccess { node ->
                    node?.let { folder ->
                        creationTime = folder.creationTime
                        runCatching {
                            getFolderTreeInfo(folder as TypedFolderNode)
                        }.onSuccess { folderTreeInfo ->
                            with(folderTreeInfo) {
                                numOfFiles = numberOfFiles
                                numOfFolders =
                                    numberOfFolders - 1 //we don't want to count itself
                                totalSizeInBytes = totalCurrentSizeInBytes
                            }
                        }.onFailure {
                            Timber.e(it)
                        }
                    }
                }.onFailure {
                    Timber.e(it)
                }

                sync.copy(
                    hasStalledIssues = stalledIssues.any {
                        it.localPaths.firstOrNull()?.contains(sync.deviceStoragePath)
                            ?: (it.nodeNames.first().contains(sync.megaStoragePath))
                    },
                    numberOfFiles = numOfFiles,
                    numberOfFolders = numOfFolders,
                    totalSizeInBytes = totalSizeInBytes,
                    creationTime = creationTime,
                )
            }
        }
            .onEach {
                updateSyncsState(it)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun updateSyncsState(
        syncs: List<SyncUiItem>,
    ) {
        val isProAccount = runCatching { isProAccountUseCase() }.getOrNull() ?: false
        when {
            syncs.isNotEmpty() && !showSyncsPausedErrorDialogShown && isProAccount.not() -> {
                _uiState.update {
                    it.copy(
                        syncUiItems = syncs,
                        isRefreshing = false,
                        isFreeAccount = isProAccount.not(),
                        isLoading = false,
                        showSyncsPausedErrorDialog = true
                    )
                }
                showSyncsPausedErrorDialogShown = true
            }

            else -> {
                _uiState.update {
                    it.copy(
                        syncUiItems = syncs,
                        isRefreshing = false,
                        isFreeAccount = isProAccount.not(),
                        isLoading = false,
                        showSyncsPausedErrorDialog = false
                    )
                }
            }
        }
    }

    private fun checkOverQuotaStatus() {
        viewModelScope.launch {
            runCatching {
                isStorageOverQuotaUseCase()
            }.onSuccess { isStorageOverQuota ->
                _uiState.update {
                    it.copy(
                        isStorageOverQuota = isStorageOverQuota,
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun handleAction(action: SyncFoldersAction) {
        when (action) {
            is SyncFoldersAction.CardExpanded -> {
                val syncUiItem = action.syncUiItem
                val expanded = action.expanded

                _uiState.update { state ->
                    state.copy(syncUiItems = _uiState.value.syncUiItems.map {
                        if (it.id == syncUiItem.id) {
                            it.copy(expanded = expanded)
                        } else {
                            it
                        }
                    })
                }
            }

            is SyncFoldersAction.RemoveFolderClicked -> {
                _uiState.update { state ->
                    state.copy(
                        showConfirmRemoveSyncFolderDialog = true,
                        syncUiItemToRemove = action.syncUiItem
                    )
                }
            }

            is SyncFoldersAction.OnRemoveSyncFolderDialogConfirmed -> {
                viewModelScope.launch {
                    uiState.value.syncUiItemToRemove?.let { syncUiItemToRemove ->
                        runCatching {
                            removeFolderPairUseCase(syncUiItemToRemove.id)
                        }.onSuccess {
                            _uiState.update { state -> state.copy(snackbarMessage = sharedResR.string.sync_snackbar_message_confirm_sync_stopped) }
                        }.onFailure {
                            Timber.e(it)
                        }
                    }
                }
                dismissConfirmRemoveSyncFolderDialog()
            }

            is SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed -> {
                viewModelScope.launch {
                    uiState.value.syncUiItemToRemove?.let { syncUiItemToRemove ->
                        runCatching {
                            removeFolderPairUseCase(syncUiItemToRemove.id)
                        }.onSuccess {
                            syncUiItemToRemove.apply {
                                when (action.stopBackupOption) {
                                    StopBackupOption.MOVE -> {
                                        getRootNodeUseCase()?.let { rootNode ->
                                            runCatching {
                                                moveDeconfiguredBackupNodesUseCase(
                                                    deconfiguredBackupRoot = megaStorageNodeId,
                                                    backupDestination = rootNode.id,
                                                )
                                            }.onFailure {
                                                Timber.e(it)
                                            }
                                        }
                                    }

                                    StopBackupOption.DELETE -> {
                                        runCatching {
                                            removeDeconfiguredBackupNodesUseCase(
                                                deconfiguredBackupRoot = megaStorageNodeId,
                                            )
                                        }.onFailure {
                                            Timber.e(it)
                                        }
                                    }
                                }
                            }
                            _uiState.update { state -> state.copy(snackbarMessage = sharedResR.string.sync_snackbar_message_confirm_backup_stopped) }
                        }.onFailure {
                            Timber.e(it)
                        }
                    }
                }
                dismissConfirmRemoveSyncFolderDialog()
            }

            is SyncFoldersAction.OnRemoveFolderDialogDismissed -> {
                dismissConfirmRemoveSyncFolderDialog()
            }

            is SyncFoldersAction.PauseRunClicked -> {
                viewModelScope.launch {
                    if (action.syncUiItem.status != SyncStatus.PAUSED) {
                        pauseSyncUseCase(action.syncUiItem.id)
                        setUserPausedSyncsUseCase(action.syncUiItem.id, true)
                    } else {
                        resumeSyncUseCase(action.syncUiItem.id)
                        setUserPausedSyncsUseCase(action.syncUiItem.id, false)
                    }
                }
            }

            is SyncFoldersAction.OnSyncsPausedErrorDialogDismissed -> {
                _uiState.update {
                    it.copy(showSyncsPausedErrorDialog = false)
                }
            }

            is SyncFoldersAction.SnackBarShown -> {
                _uiState.update { state ->
                    state.copy(snackbarMessage = null)
                }
            }
        }
    }

    private fun dismissConfirmRemoveSyncFolderDialog() {
        _uiState.update {
            it.copy(
                showConfirmRemoveSyncFolderDialog = false,
                syncUiItemToRemove = null
            )
        }
    }

    fun onSyncRefresh() {
        viewModelScope.launch {
            refreshSyncUseCase()
        }
    }
}
