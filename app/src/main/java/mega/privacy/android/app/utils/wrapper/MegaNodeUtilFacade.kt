package mega.privacy.android.app.utils.wrapper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.utils.Constants.URL_INDICATOR
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.NodeTakenDownDialogListener
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mega node util facade
 */
@Singleton
class MegaNodeUtilFacade @Inject constructor(
    private val streamingGateway: StreamingGateway,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) : MegaNodeUtilWrapper {
    override fun getMyChatFilesFolder() = MegaNodeUtil.myChatFilesFolder

    override fun getCloudRootHandle() = MegaNodeUtil.cloudRootHandle

    override fun getNumberOfFolders(nodes: List<MegaNode?>?) =
        MegaNodeUtil.getNumberOfFolders(nodes)

    override fun showTakenDownNodeActionNotAvailableDialog(
        node: MegaNode?,
        context: Context,
    ) = MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(node, context)

    override fun shareNode(context: Context, node: MegaNode?) {
        MegaNodeUtil.shareNode(context, node)
    }

    override fun shareNode(
        context: Context,
        node: MegaNode?,
        onExportFinishedListener: (() -> Unit)?,
    ) {
        MegaNodeUtil.shareNode(context, node, onExportFinishedListener)
    }

    override fun areAllNodesDownloaded(context: Context, listNodes: List<MegaNode>) =
        MegaNodeUtil.areAllNodesDownloaded(context, listNodes)

    override fun getExportNodesLink(listNodes: List<MegaNode>) =
        MegaNodeUtil.getExportNodesLink(listNodes)

    override fun shareNodes(context: Context, nodes: List<MegaNode>) {
        MegaNodeUtil.shareNodes(context, nodes)
    }

    override fun shouldContinueWithoutError(context: Context, node: MegaNode?) =
        MegaNodeUtil.shouldContinueWithoutError(context, node)

    override fun shouldContinueWithoutError(context: Context, nodes: List<MegaNode>?) =
        MegaNodeUtil.shouldContinueWithoutError(context, nodes)

    override fun existsMyChatFilesFolder() = MegaNodeUtil.existsMyChatFilesFolder()

    override suspend fun isOutShare(node: MegaNode) = MegaNodeUtil.isOutShare(node)

    override fun getFolderIcon(node: MegaNode, drawerItem: DrawerItem) =
        MegaNodeUtil.getFolderIcon(node, drawerItem)

    override fun isInRootLinksLevel(adapterType: Int, parentHandle: Long) =
        MegaNodeUtil.isInRootLinksLevel(adapterType, parentHandle)

    override fun showShareOption(adapterType: Int, isFolderLink: Boolean, handle: Long) =
        MegaNodeUtil.showShareOption(adapterType, isFolderLink, handle)

    override fun isNodeInRubbishOrDeleted(handle: Long) =
        MegaNodeUtil.isNodeInRubbishOrDeleted(handle)

    override fun areAllFileNodesAndNotTakenDown(nodes: List<MegaNode>) =
        MegaNodeUtil.areAllFileNodesAndNotTakenDown(nodes)

    override fun allHaveFullAccess(nodes: List<MegaNode?>) = MegaNodeUtil.allHaveFullAccess(nodes)

    override fun allHaveOwnerAccessAndNotTakenDown(nodes: List<MegaNode?>) =
        MegaNodeUtil.allHaveOwnerAccessAndNotTakenDown(nodes)

    override fun isEmptyFolder(node: MegaNode?) = MegaNodeUtil.isEmptyFolder(node)

    override fun getDlList(
        megaApi: MegaApiAndroid,
        dlFiles: MutableMap<MegaNode, String>,
        parent: MegaNode?,
        folder: File,
    ) {
        MegaNodeUtil.getDlList(megaApi, dlFiles, parent, folder)
    }

    override fun getNodeLabelDrawable(nodeLabel: Int, resources: Resources) =
        MegaNodeUtil.getNodeLabelDrawable(nodeLabel, resources)

    override fun getNodeLabelText(nodeLabel: Int, context: Context) =
        MegaNodeUtil.getNodeLabelText(nodeLabel, context)

    override fun getNodeLabelColor(nodeLabel: Int) = MegaNodeUtil.getNodeLabelColor(nodeLabel)

    override fun stopStreamingServerIfNeeded(shouldStopServer: Boolean, megaApi: MegaApiAndroid) {
        MegaNodeUtil.stopStreamingServerIfNeeded(shouldStopServer, megaApi)
    }

    override fun showTakenDownDialog(
        isFolder: Boolean,
        listener: NodeTakenDownDialogListener?,
        context: Context,
    ) = MegaNodeUtil.showTakenDownDialog(isFolder, listener, context)

    override fun selectFolderToMove(activity: Activity, handles: LongArray) {
        MegaNodeUtil.selectFolderToMove(activity, handles)
    }

    override fun selectFolderToCopy(activity: Activity, handles: LongArray) {
        MegaNodeUtil.selectFolderToCopy(activity, handles)
    }

    override fun getNodeLocationInfo(
        adapterType: Int,
        fromIncomingShare: Boolean,
        handle: Long,
    ) = MegaNodeUtil.getNodeLocationInfo(adapterType, fromIncomingShare, handle)

    override fun handleLocationClick(activity: Activity, adapterType: Int, location: LocationInfo) {
        MegaNodeUtil.handleLocationClick(activity, adapterType, location)
    }

    override fun openZip(
        context: Context,
        activityLauncher: ActivityLauncher,
        zipFilePath: String,
        snackbarShower: SnackbarShower,
        nodeHandle: Long,
    ) {
        MegaNodeUtil.openZip(context, activityLauncher, zipFilePath, snackbarShower, nodeHandle)
    }

    override fun launchActionView(
        context: Context,
        nodeName: String,
        localPath: String,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    ) {
        MegaNodeUtil.launchActionView(
            context,
            nodeName,
            localPath,
            activityLauncher,
            snackbarShower
        )
    }

    override fun getFileInfo(node: MegaNode, context: Context) =
        MegaNodeUtil.getFileInfo(node, context)

    override fun manageTextFileIntent(context: Context, node: MegaNode, adapterType: Int) {
        MegaNodeUtil.manageTextFileIntent(context, node, adapterType)
    }

    override fun manageEditTextFileIntent(context: Context, node: MegaNode, adapterType: Int) {
        MegaNodeUtil.manageEditTextFileIntent(context, node, adapterType)
    }

    override fun manageTextFileIntent(
        context: Context,
        node: MegaNode,
        adapterType: Int,
        urlFileLink: String?,
    ) {
        MegaNodeUtil.manageTextFileIntent(context, node, adapterType, urlFileLink)
    }

    override fun manageTextFileIntent(
        context: Context,
        node: MegaNode,
        adapterType: Int,
        urlFileLink: String?,
        mode: String,
    ) {
        MegaNodeUtil.manageTextFileIntent(context, node, adapterType, urlFileLink, mode)
    }

    override fun manageURLNode(context: Context, megaApi: MegaApiAndroid, node: MegaNode) {

        val progressDialog = android.app.ProgressDialog(context)
        progressDialog.apply {
            setMessage(context.getString(R.string.link_request_status))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }

        coroutineScope.launch {
            var shouldStopServer = false
            try {
                val localPath = getLocalFile(node)
                val bufferedReader = if (localPath != null) {
                    //Read local file
                    val localFile = File(localPath)
                    BufferedReader(FileReader(localFile))
                } else {
                    //Read streaming file
                    shouldStopServer = launchStreamingServer()
                    val uri = megaApi.httpServerGetLocalLink(node)

                    if (uri.isNullOrEmpty()) {
                        showSnackbar(context, context.getString(R.string.error_open_file_with))
                        throw Throwable("Error getting URL")
                    }
                    val nodeURL = URL(uri)
                    val connection = nodeURL.openConnection() as HttpURLConnection
                    BufferedReader(InputStreamReader(connection.inputStream))
                }

                var line = bufferedReader.readLine()

                if (line != null) {
                    line = bufferedReader.readLine()
                    val url = line.replace(URL_INDICATOR, "")
                    val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))

                    if (isIntentAvailable(context, intent)) {
                        context.startActivity(intent)
                    } else {
                        showSnackbar(
                            context,
                            context.getString(R.string.intent_not_available_file)
                        )
                    }
                    return@launch
                } else {
                    Timber.d("Not expected format: Exception on processing url file")
                }

            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                if (shouldStopServer) stopStreamingServer()
                progressDialog.dismiss()
            }
        }
    }

    override fun onNodeTapped(
        context: Context,
        node: MegaNode,
        nodeDownloader: (node: MegaNode) -> Unit,
        activityLauncher: ActivityLauncher,
        snackbarShower: SnackbarShower,
    ) {
        MegaNodeUtil.onNodeTapped(context, node, nodeDownloader, activityLauncher, snackbarShower)
    }

    override fun getBackupRootNodeByHandle(
        megaApi: MegaApiAndroid,
        handleList: ArrayList<Long>?,
    ) = MegaNodeUtil.getBackupRootNodeByHandle(megaApi, handleList)

    override fun checkBackupNodeTypeInList(megaApi: MegaApiAndroid, handleList: List<Long>?) =
        MegaNodeUtil.checkBackupNodeTypeInList(megaApi, handleList)

    override fun checkBackupNodeTypeByHandle(megaApi: MegaApiAndroid, node: MegaNode?) =
        MegaNodeUtil.checkBackupNodeTypeByHandle(megaApi, node)

    override fun setupStreamingServer() {
        runBlocking { launchStreamingServer() }
    }

    private suspend fun launchStreamingServer() = streamingGateway.startServer()
    private suspend fun stopStreamingServer() = streamingGateway.stopServer()
}
