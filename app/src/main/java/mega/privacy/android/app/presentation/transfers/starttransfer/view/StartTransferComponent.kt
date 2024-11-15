package mega.privacy.android.app.presentation.transfers.starttransfer.view

import mega.privacy.android.shared.resources.R as sharedR
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.permissions.NotificationsPermissionActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.TargetPreference
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog.ResumeTransfersDialog
import mega.privacy.android.app.presentation.transfers.starttransfer.view.filespermission.FilesPermissionDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.NotEnoughSpaceForUploadDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.TransferInProgressDialog
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import timber.log.Timber

/**
 * Helper compose view to show UI related to starting a download transfer
 * (scanning in progress dialog, not enough space snackbar, start download snackbar, quota exceeded, etc.)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun StartTransferComponent(
    event: StateEventWithContent<TransferTriggerEvent>,
    onConsumeEvent: () -> Unit,
    snackBarHostState: SnackbarHostState,
    onScanningFinished: (StartTransferEvent) -> Unit = {},
    viewModel: StartTransfersComponentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFilesPermissionRequest by rememberSaveable { mutableStateOf(false) }
    var showStorageOverQuotaWarning by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    val mediaReadPermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
            if (granted) {
                viewModel.startTransferAfterPermissionRequest()
            } else {
                viewModel.consumeRequestPermission()
            }
        }
    } else {
        null
    }

    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            lifecycleOwner.lifecycle.addObserver(viewModel)
        }
    }

    EventEffect(
        event = event,
        onConsumed = onConsumeEvent,
        action = { triggerEvent ->
            if (uiState.isStorageOverQuota) {
                showStorageOverQuotaWarning = true
                return@EventEffect
            }

            notificationPermission?.status?.let { permissionStatus ->
                if (permissionStatus.shouldShowRationale) {
                    context.startActivity(
                        Intent(context, NotificationsPermissionActivity::class.java)
                    )
                } else if (!permissionStatus.isGranted) {
                    notificationPermission.launchPermissionRequest()
                }
            }
            when {
                shouldAskForFilesPermission(uiState.requestFilesPermissionDenied, triggerEvent) -> {
                    viewModel.transferEventWaitingForPermissionRequest(triggerEvent)
                    showFilesPermissionRequest = true
                }

                triggerEvent !is TransferTriggerEvent.StartUpload.TextFile
                        && mediaReadPermission?.status?.isGranted == false -> {
                    viewModel.transferEventWaitingForPermissionRequest(triggerEvent)
                    mediaReadPermission.launchPermissionRequest()
                }

                else -> {
                    viewModel.startTransfer(triggerEvent)
                }
            }
        })

    if (showFilesPermissionRequest) {
        FilesPermissionDialog(
            onDoNotShowAgainClick = { viewModel.setRequestFilesPermissionDenied() },
            onStartTransferAndDismiss = {
                viewModel.startTransferAfterPermissionRequest()
                showFilesPermissionRequest = false
            }
        )
    }

    if (showStorageOverQuotaWarning) {
        NotEnoughSpaceForUploadDialog(onCancel = { showStorageOverQuotaWarning = false })
    }

    StartTransferComponent(
        uiState = uiState,
        onOneOffEventConsumed = viewModel::consumeOneOffEvent,
        onCancelled = viewModel::cancelCurrentTransfersJob,
        onLargeDownloadAnswered = viewModel::largeDownloadAnswered,
        onDestinationSet = viewModel::startDownloadWithDestination,
        onPromptSaveDestinationConsumed = viewModel::consumePromptSaveDestination,
        onSaveDestination = viewModel::saveDestination,
        onDoNotPromptToSaveDestinationAgain = viewModel::doNotPromptToSaveDestinationAgain,
        onResumeTransfers = viewModel::resumeTransfers,
        onAskedResumeTransfers = viewModel::setAskedResumeTransfers,
        snackBarHostState = snackBarHostState,
        onScanningFinished = onScanningFinished,
    )
}

/**
 * Helper function to wrap [StartTransferComponent] into a [ComposeView] so it can be used in screens using View system
 * @param activity the parent activity where this view will be added, it should implement [SnackbarShower] to show the generated Snackbars
 * @param transferEventState flow that usually comes from the view model and triggers the download Transfer events
 * @param onConsumeEvent lambda to consume the download event, typically it will launch the corresponding consume event in the view model,
 * @param onScanningFinished lambda to be called when the scanning process is finished.
 */
fun createStartTransferView(
    activity: Activity,
    transferEventState: Flow<StateEventWithContent<TransferTriggerEvent>>,
    onConsumeEvent: () -> Unit,
    onScanningFinished: (StartTransferEvent) -> Unit = {},
): View = ComposeView(activity).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        val downloadEvent by transferEventState.collectAsStateWithLifecycle(
            (transferEventState as? StateFlow)?.value ?: consumed()
        )
        OriginalTempTheme(isDark = isSystemInDarkTheme()) {
            val snackbarHostState = remember { SnackbarHostState() }
            //if we need this view is because we are not using compose views, so we don't have a scaffold to show snack bars and need to launch a View snackbar
            LegacySnackBarWrapper(snackbarHostState = snackbarHostState, activity)
            StartTransferComponent(
                downloadEvent,
                onConsumeEvent,
                snackBarHostState = snackbarHostState,
                onScanningFinished,
            )
        }
    }
}


@Composable
private fun StartTransferComponent(
    uiState: StartTransferViewState,
    onOneOffEventConsumed: () -> Unit,
    onCancelled: () -> Unit,
    onLargeDownloadAnswered: (TransferTriggerEvent.DownloadTriggerEvent?, saveDoNotAskAgain: Boolean) -> Unit,
    onDestinationSet: (destination: Uri?) -> Unit,
    onPromptSaveDestinationConsumed: () -> Unit,
    onSaveDestination: (String) -> Unit,
    onDoNotPromptToSaveDestinationAgain: () -> Unit,
    onResumeTransfers: () -> Unit,
    onAskedResumeTransfers: () -> Unit,
    snackBarHostState: SnackbarHostState,
    onScanningFinished: (StartTransferEvent) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showOfflineAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showResumeTransfersAlertDialog by rememberSaveable { mutableStateOf(false) }
    val showQuotaExceededDialog = rememberSaveable(stateSaver = storageStateSaver) {
        mutableStateOf(null)
    }
    var launchFolderPickerForDownloadDestination by rememberSaveable(uiState.askDestinationForDownload != null) {
        mutableStateOf(uiState.askDestinationForDownload != null)
    }

    val folderPicker = launchFolderPicker(
        onCancel = {
            onDestinationSet(null)
        },
        onFolderSelected = { uri ->
            onDestinationSet(uri)
        },
    )

    EventEffect(
        event = uiState.oneOffViewEvent,
        onConsumed = onOneOffEventConsumed,
        action = {
            when (it) {
                is StartTransferEvent.FinishDownloadProcessing -> {
                    consumeFinishProcessing(
                        event = it,
                        snackBarHostState = snackBarHostState,
                        showQuotaExceededDialog = showQuotaExceededDialog,
                        context = context,
                        transferTriggerEvent = it.triggerEvent,
                    )
                    onScanningFinished(it)
                }

                is StartTransferEvent.FinishUploadProcessing -> {
                    val message = context.resources.getQuantityString(
                        R.plurals.upload_began,
                        it.totalFiles,
                        it.totalFiles,
                    )
                    snackBarHostState.showAutoDurationSnackbar(message)
                    onScanningFinished(it)
                }

                is StartTransferEvent.Message ->
                    consumeMessage(it, snackBarHostState, context)

                StartTransferEvent.NotConnected -> {
                    showOfflineAlertDialog = true
                }

                StartTransferEvent.PausedTransfers -> {
                    showResumeTransfersAlertDialog = true
                }
            }
        })

    var showPromptSaveDestinationDialog by rememberSaveable { mutableStateOf<String?>(null) }
    EventEffect(
        event = uiState.promptSaveDestination,
        onConsumed = onPromptSaveDestinationConsumed,
        action = {
            showPromptSaveDestinationDialog = it
        }
    )

    TransferInProgressDialog(
        uiState.jobInProgressState,
        onCancel = onCancelled,
    )

    if (showOfflineAlertDialog) {
        MegaAlertDialog(
            text = stringResource(id = R.string.error_server_connection_problem),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = { showOfflineAlertDialog = false },
            onDismiss = { showOfflineAlertDialog = false },
        )
    }
    showQuotaExceededDialog.value?.let {
        StorageStatusDialogView(
            storageState = it,
            preWarning = it != StorageState.Red,
            overQuotaAlert = true,
            onUpgradeClick = {
                context.startActivity(Intent(context, UpgradeAccountActivity::class.java))
            },
            onCustomizedPlanClick = { email, accountType ->
                AlertsAndWarnings.askForCustomizedPlan(context, email, accountType)
            },
            onAchievementsClick = {
                context.startActivity(
                    Intent(context, MyAccountActivity::class.java)
                        .setAction(IntentConstants.ACTION_OPEN_ACHIEVEMENTS)
                )
            },
            onClose = { showQuotaExceededDialog.value = null },
        )
    }
    uiState.confirmLargeDownload?.let {
        ConfirmationDialog(
            title = stringResource(id = R.string.transfers_confirm_large_download_title),
            text = stringResource(id = R.string.alert_larger_file, it.sizeString),
            buttonOption1Text = stringResource(id = R.string.transfers_confirm_large_download_button_start),
            buttonOption2Text = stringResource(id = R.string.transfers_confirm_large_download_button_start_always),
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onOption1 = {
                onLargeDownloadAnswered(it.transferTriggerEvent, false)
            },
            onOption2 = {
                onLargeDownloadAnswered(it.transferTriggerEvent, true)
            },
            onDismiss = { onLargeDownloadAnswered(null, false) },
        )
    }
    if (launchFolderPickerForDownloadDestination) {
        launchFolderPickerForDownloadDestination = false
        runCatching {
            folderPicker.launch(null)
        }.onFailure {
            coroutineScope.launch {
                snackBarHostState.showAutoDurationSnackbar(context.getString(R.string.general_warning_no_picker))
            }
        }
    }
    showPromptSaveDestinationDialog?.let { destination ->
        //this dialog will be updated once we have a dialog defined for this case that follows our DS
        ConfirmationDialog(
            title = null,
            text = stringResource(id = sharedR.string.transfers_dialog_save_download_location_title),
            buttonOption1Text = stringResource(id = sharedR.string.transfers_dialog_save_download_location_only_this_time_option),
            buttonOption2Text = stringResource(id = sharedR.string.transfers_dialog_save_download_location_always_here_option),
            cancelButtonText = stringResource(id = sharedR.string.transfers_dialog_save_download_location_always_ask_option),
            onOption1 = {
                //nothing in this case, just dismiss
                showPromptSaveDestinationDialog = null
            },
            onOption2 = {
                onSaveDestination(destination)
                showPromptSaveDestinationDialog = null
            },
            onCancel = {
                onDoNotPromptToSaveDestinationAgain()
                showPromptSaveDestinationDialog = null
            },
            onDismiss = {
                showPromptSaveDestinationDialog = null
            }
        )
    }
    if (showResumeTransfersAlertDialog) {
        ResumeTransfersDialog(onResume = {
            onResumeTransfers()
            showResumeTransfersAlertDialog = false
        }, onDismiss = {
            onAskedResumeTransfers()
            showResumeTransfersAlertDialog = false
        })
    }
}

private val storageStateSaver = Saver<StorageState?, Int>(
    save = { it?.ordinal },
    restore = { StorageState.entries.getOrNull(it) }
)

private suspend fun consumeFinishProcessing(
    event: StartTransferEvent.FinishDownloadProcessing,
    snackBarHostState: SnackbarHostState,
    showQuotaExceededDialog: MutableState<StorageState?>,
    context: Context,
    transferTriggerEvent: TransferTriggerEvent?,
) {
    var delayed = false
    when (event.exception) {
        null -> {
            val message = when {
                transferTriggerEvent is TransferTriggerEvent.StartDownloadForPreview -> {
                    context.resources.getString(R.string.cloud_drive_snackbar_preparing_file_for_preview_context)
                }

                event.totalAlreadyDownloaded == 0 -> {
                    // Delayed to avoid showing the snackbar and the transfers widget just at the same time because it sometimes causes a flick animation
                    delayed = true
                    context.resources.getQuantityString(
                        R.plurals.download_started,
                        event.totalNodes,
                        event.totalNodes,
                    )
                }

                event.filesToDownload == 0 -> {
                    context.resources.getQuantityString(
                        R.plurals.already_downloaded_service,
                        event.totalFiles,
                        event.totalFiles,
                    )
                }

                event.totalAlreadyDownloaded == 1 -> {
                    context.resources.getQuantityString(
                        R.plurals.file_already_downloaded_and_files_pending_download,
                        event.filesToDownload,
                        event.filesToDownload
                    )
                }

                event.filesToDownload == 1 -> {
                    context.resources.getQuantityString(
                        R.plurals.files_already_downloaded_and_file_pending_download,
                        event.totalAlreadyDownloaded,
                        event.totalAlreadyDownloaded
                    )
                }

                else -> {
                    StringBuilder().append(
                        context.resources.getQuantityString(
                            R.plurals.file_already_downloaded,
                            event.totalAlreadyDownloaded,
                            event.totalAlreadyDownloaded
                        )
                    ).append(" ").append(
                        context.resources.getQuantityString(
                            R.plurals.file_pending_download,
                            event.filesToDownload,
                            event.filesToDownload
                        )
                    ).toString()
                }
            }
            if (delayed) {
                coroutineScope {
                    launch {
                        delay(100)
                        snackBarHostState.showAutoDurationSnackbar(message)
                    }
                }
            } else {
                snackBarHostState.showAutoDurationSnackbar(message)
            }
        }

        is QuotaExceededMegaException -> {
            showQuotaExceededDialog.value = StorageState.Red
        }

        is NotEnoughQuotaMegaException -> {
            showQuotaExceededDialog.value = StorageState.Orange
        }

        else -> {
            Timber.e(event.exception)
            snackBarHostState.showAutoDurationSnackbar(context.getString(R.string.general_error))
        }
    }
}

private suspend fun consumeMessage(
    event: StartTransferEvent.Message,
    snackBarHostState: SnackbarHostState,
    context: Context,
) {
    //show snack bar with an optional action
    val result = snackBarHostState.showAutoDurationSnackbar(
        context.getString(event.message),
        event.action?.let { context.getString(it) }
    )
    if (result == SnackbarResult.ActionPerformed && event.actionEvent != null) {
        consumeMessageAction(
            event.actionEvent,
            context
        )
    }
}

private fun consumeMessageAction(
    actionEvent: StartTransferEvent.Message.ActionEvent,
    context: Context,
) = when (actionEvent) {
    StartTransferEvent.Message.ActionEvent.GoToFileManagement -> {
        ContextCompat.startActivity(
            context,
            SettingsActivity.getIntent(context, TargetPreference.Storage),
            null
        )
    }
}

private fun shouldAskForFilesPermission(
    requestFilesPermissionDenied: Boolean,
    transferTriggerEvent: TransferTriggerEvent,
) = (transferTriggerEvent is TransferTriggerEvent.StartChatUpload
        || (transferTriggerEvent is TransferTriggerEvent.StartUpload
        && transferTriggerEvent !is TransferTriggerEvent.StartUpload.TextFile))
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        && requestFilesPermissionDenied.not()
        && !Environment.isExternalStorageManager()

