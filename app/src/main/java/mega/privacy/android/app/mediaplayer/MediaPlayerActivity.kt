package mega.privacy.android.app.mediaplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.exoplayer2.util.Util.startForegroundService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityAudioPlayerBinding
import mega.privacy.android.app.databinding.ActivityVideoPlayerBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.mediaplayer.service.VideoPlayerService
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragment
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoFragmentArgs
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.shareUri
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.MegaNodeUtil.*
import mega.privacy.android.app.utils.MegaNodeUtilKt.Companion.selectCopyFolder
import mega.privacy.android.app.utils.MegaNodeUtilKt.Companion.selectMoveFolder
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerActivity : BaseActivity(), SnackbarShower, ActivityLauncher {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private lateinit var rootLayout: ViewGroup
    private lateinit var toolbar: Toolbar
    private val viewModel: MediaPlayerViewModel by viewModels()

    private lateinit var actionBar: ActionBar
    private lateinit var navController: NavController

    private var optionsMenu: Menu? = null
    private var searchMenuItem: MenuItem? = null

    private var viewingTrackInfo: TrackInfoFragmentArgs? = null

    private var serviceBound = false
    private var playerService: MediaPlayerService? = null

    private val nodeAttacher = MegaAttacher(this)
    private val nodeSaver = NodeSaver(this, this, this, showSaveToDeviceConfirmDialog(this))

    private val dragToExit = DragToExitSupport(this, this::onDragActivated) {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        /**
         * Called after a successful bind with our AudioPlayerService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MediaPlayerServiceBinder) {
                playerService = service.service

                service.service.viewModel.playlist.observe(this@MediaPlayerActivity) {
                    if (service.service.viewModel.playlistSearchQuery != null) {
                        return@observe
                    }

                    if (it.first.isEmpty()) {
                        stopPlayer()
                    } else {
                        val currentFragment = navController.currentDestination?.id ?: return@observe
                        refreshMenuOptionsVisibility(currentFragment)
                    }
                }

                service.service.metadata.observe(this@MediaPlayerActivity) {
                    dragToExit.nodeChanged(service.service.viewModel.playingHandle)
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }

        val rebuildPlaylist = intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        if (adapterType == INVALID_VALUE && rebuildPlaylist) {
            finish()
            return
        }

        if (savedInstanceState != null) {
            nodeAttacher.restoreState(savedInstanceState)
            nodeSaver.restoreState(savedInstanceState)
        }

        val isAudioPlayer = isAudioPlayer(intent)

        if (isAudioPlayer) {
            val binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
            setContentView(binding.root)

            rootLayout = binding.rootLayout
            toolbar = binding.toolbar
        } else {
            val binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
            setContentView(dragToExit.wrapContentView(binding.root))

            rootLayout = binding.rootLayout
            toolbar = binding.toolbar

            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_alpha_020))
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white_alpha_087))

            MediaPlayerService.pauseAudioPlayer(this)

            dragToExit.observeThumbnailLocation(this)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupToolbar()
        setupNavDestListener()

        val playerServiceIntent = Intent(
            this,
            if (isAudioPlayer) AudioPlayerService::class.java else VideoPlayerService::class.java
        )

        playerServiceIntent.putExtras(extras)

        if (rebuildPlaylist) {
            playerServiceIntent.setDataAndType(intent.data, intent.type)
            if (isAudioPlayer) {
                startForegroundService(this, playerServiceIntent)
            } else {
                startService(playerServiceIntent)
            }
        }

        bindService(playerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true

        viewModel.itemToRemove.observe(this) {
            playerService?.viewModel?.removeItem(it)
        }

        if (savedInstanceState == null && !isAudioPlayer) {
            // post to next UI cycle so that MediaPlayerFragment's onCreateView is called
            post {
                getFragmentFromNavHost(R.id.nav_host_fragment, MediaPlayerFragment::class.java)
                    ?.runEnterAnimation(dragToExit)
            }
        }

        if (isAudioPlayer) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    private fun stopPlayer() {
        playerService?.stopAudioPlayer()
        finish()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        actionBar = supportActionBar!!
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (!navController.navigateUp()) {
            playerService?.mainPlayerUIClosed()
            finish()
        }
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, args ->
            if (isAudioPlayer()) {
                toolbar.elevation = 0F

                val color = ContextCompat.getColor(
                    this,
                    if (dest.id == R.id.main_player) R.color.grey_020_grey_800 else R.color.white_dark_grey
                )

                window.statusBarColor = color
                toolbar.setBackgroundColor(color)
            } else {
                window.statusBarColor = Color.BLACK
                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_alpha_020))
            }

            when (dest.id) {
                R.id.main_player -> {
                    actionBar.title = ""
                    viewingTrackInfo = null
                }
                R.id.playlist -> {
                    viewingTrackInfo = null
                }
                R.id.track_info -> {
                    actionBar.setTitle(R.string.audio_track_info)

                    if (args != null) {
                        viewingTrackInfo = TrackInfoFragmentArgs.fromBundle(args)
                    }
                }
            }

            refreshMenuOptionsVisibility(dest.id)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        playerService = null
        if (serviceBound) {
            unbindService(connection)
        }

        nodeSaver.destroy()

        if (!isAudioPlayer(intent)) {
            MediaPlayerService.resumeAudioPlayer(this)
        }

        dragToExit.showPreviousHiddenThumbnail()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu

        menuInflater.inflate(R.menu.audio_player, menu)

        searchMenuItem = menu.findItem(R.id.action_search)

        val searchView = searchMenuItem?.actionView
        if (searchView is SearchView) {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    playerService?.viewModel?.playlistSearchQuery = newText
                    return true
                }

            })
        }

        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                playerService?.viewModel?.playlistSearchQuery = null
                return true
            }
        })

        val currentFragment = navController.currentDestination?.id
        if (currentFragment != null) {
            refreshMenuOptionsVisibility(currentFragment)
        }

        return true
    }

    private fun toggleAllMenuItemsVisibility(visible: Boolean) {
        val menu = optionsMenu ?: return
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = visible
        }
    }

    private fun refreshMenuOptionsVisibility(currentFragment: Int) {
        val adapterType = playerService?.viewModel?.currentIntent
            ?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE) ?: return

        when (currentFragment) {
            R.id.playlist -> {
                toggleAllMenuItemsVisibility(false)
                searchMenuItem?.isVisible = true
            }
            R.id.main_player, R.id.track_info -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    toggleAllMenuItemsVisibility(false)

                    optionsMenu?.findItem(R.id.properties)?.isVisible =
                        currentFragment == R.id.main_player

                    optionsMenu?.findItem(R.id.share)?.isVisible =
                        currentFragment == R.id.main_player

                    return
                }

                if (adapterType == FILE_LINK_ADAPTER || adapterType == ZIP_ADAPTER) {
                    toggleAllMenuItemsVisibility(false)
                }

                val service = playerService
                if (service == null) {
                    toggleAllMenuItemsVisibility(false)
                    return
                }

                val node = megaApi.getNodeByHandle(service.viewModel.playingHandle)
                if (node == null) {
                    toggleAllMenuItemsVisibility(false)
                    return
                }

                toggleAllMenuItemsVisibility(true)
                searchMenuItem?.isVisible = false

                optionsMenu?.findItem(R.id.save_to_device)?.isVisible = true

                optionsMenu?.findItem(R.id.properties)?.isVisible =
                    currentFragment == R.id.main_player

                optionsMenu?.findItem(R.id.share)?.isVisible =
                    currentFragment == R.id.main_player && showShareOption(
                        adapterType, adapterType == FOLDER_LINK_ADAPTER, node.handle
                    )

                optionsMenu?.findItem(R.id.send_to_chat)?.isVisible = adapterType != FROM_CHAT

                if (megaApi.getAccess(node) == MegaShare.ACCESS_OWNER) {
                    if (node.isExported) {
                        optionsMenu?.findItem(R.id.get_link)?.isVisible = false
                        optionsMenu?.findItem(R.id.remove_link)?.isVisible = true
                    } else {
                        optionsMenu?.findItem(R.id.get_link)?.isVisible = true
                        optionsMenu?.findItem(R.id.remove_link)?.isVisible = false
                    }
                } else {
                    optionsMenu?.findItem(R.id.get_link)?.isVisible = false
                    optionsMenu?.findItem(R.id.remove_link)?.isVisible = false
                }

                val access = megaApi.getAccess(node)
                when (access) {
                    MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_READ, MegaShare.ACCESS_UNKNOWN -> {
                        optionsMenu?.findItem(R.id.rename)?.isVisible = false
                        optionsMenu?.findItem(R.id.move)?.isVisible = false
                    }
                    MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                        optionsMenu?.findItem(R.id.rename)?.isVisible = true
                        optionsMenu?.findItem(R.id.move)?.isVisible = true
                    }
                }

                optionsMenu?.findItem(R.id.move_to_trash)?.isVisible =
                    node.parentHandle != megaApi.rubbishNode.handle
                            && (access == MegaShare.ACCESS_FULL || access == MegaShare.ACCESS_OWNER)

                optionsMenu?.findItem(R.id.copy)?.isVisible =
                    adapterType != FOLDER_LINK_ADAPTER && adapterType != FROM_CHAT
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val service = playerService ?: return false
        val launchIntent = service.viewModel.currentIntent ?: return false
        val playingHandle = service.viewModel.playingHandle
        val adapterType = launchIntent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val isFolderLink = adapterType == FOLDER_LINK_ADAPTER

        when (item.itemId) {
            R.id.save_to_device -> {
                if (adapterType == OFFLINE_ADAPTER) {
                    nodeSaver.saveOfflineNode(playingHandle, true)
                } else {
                    nodeSaver.saveHandle(
                        playingHandle,
                        isFolderLink = isFolderLink,
                        fromMediaViewer = true
                    )
                }
                return true
            }
            R.id.properties -> {
                if (isAudioPlayer()) {
                    val uri =
                        service.exoPlayer.currentMediaItem?.playbackProperties?.uri ?: return true
                    navController.navigate(
                        MediaPlayerFragmentDirections.actionPlayerToTrackInfo(
                            adapterType, adapterType == INCOMING_SHARES_ADAPTER, playingHandle, uri
                        )
                    )
                } else {
                    val nodeName =
                        service.viewModel.getPlaylistItem(service.exoPlayer.currentMediaItem?.mediaId)?.nodeName
                            ?: return false

                    val intent: Intent

                    if (adapterType == OFFLINE_ADAPTER) {
                        intent = Intent(this, OfflineFileInfoActivity::class.java)
                    } else {
                        intent = Intent(this, FileInfoActivityLollipop::class.java)
                        intent.putExtra(NAME, nodeName)
                    }

                    intent.putExtra(HANDLE, playingHandle)
                    startActivity(intent)
                }
                return true
            }
            R.id.share -> {
                when (adapterType) {
                    OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                        val nodeName =
                            service.viewModel.getPlaylistItem(service.exoPlayer.currentMediaItem?.mediaId)?.nodeName
                                ?: return false
                        val uri = service.exoPlayer.currentMediaItem?.playbackProperties?.uri
                            ?: return false

                        shareUri(this, nodeName, uri)
                    }
                    FILE_LINK_ADAPTER -> {
                        shareLink(this, launchIntent.getStringExtra(URL_FILE_LINK))
                    }
                    else -> {
                        shareNode(this, megaApi.getNodeByHandle(service.viewModel.playingHandle))
                    }
                }
                return true
            }
            R.id.send_to_chat -> {
                nodeAttacher.attachNode(playingHandle)
                return true
            }
            R.id.get_link -> {
                if (showTakenDownNodeActionNotAvailableDialog(
                        megaApi.getNodeByHandle(playingHandle), this
                    )
                ) {
                    return true
                }
                LinksUtil.showGetLinkActivity(this, playingHandle)
                return true
            }
            R.id.remove_link -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
                    return true
                }

                AlertsAndWarnings.showConfirmRemoveLinkDialog(this) {
                    megaApi.disableExport(node)
                }
                return true
            }
            R.id.rename -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                AlertsAndWarnings.showRenameDialog(this, node.name, node.isFolder) {
                    playerService?.viewModel?.updateItemName(node.handle, it)
                    updateTrackInfoNodeNameIfNeeded(node.handle, it)
                    MegaNodeUtilKt.renameNode(node, it, this)
                }
                return true
            }
            R.id.move -> {
                selectMoveFolder(this, longArrayOf(playingHandle))
                return true
            }
            R.id.copy -> {
                selectCopyFolder(this, longArrayOf(playingHandle))
                return true
            }
            R.id.move_to_trash -> {
                val node = megaApi.getNodeByHandle(playingHandle) ?: return true
                moveToRubbishBin(node)
                return true
            }
        }
        return false
    }

    /**
     * Update node name if current displayed fragment is TrackInfoFragment.
     *
     * @param handle node handle
     * @param newName new node name
     */
    private fun updateTrackInfoNodeNameIfNeeded(handle: Long, newName: String) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) ?: return
        val firstChild = navHostFragment.childFragmentManager.fragments.firstOrNull() ?: return
        if (firstChild is TrackInfoFragment) {
            firstChild.updateNodeNameIfNeeded(handle, newName)
        }
    }

    /**
     * Shows a confirmation warning before moves a node to rubbish bin.
     *
     * @param node node to be moved to rubbish bin
     */
    private fun moveToRubbishBin(node: MegaNode) {
        logDebug("moveToRubbishBin")
        if (!isOnline(this)) {
            showSnackbar(
                SNACKBAR_TYPE, getString(R.string.error_server_connection_problem),
                MEGACHAT_INVALID_HANDLE
            )
            return
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setMessage(R.string.confirmation_move_to_rubbish)
            .setPositiveButton(R.string.general_move) { _, _ ->
                playerService?.viewModel?.removeItem(node.handle)
                MegaNodeUtilKt.moveNodeToRubbishBin(node, this)
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (nodeAttacher.handleActivityResult(requestCode, resultCode, data, this)) {
            return
        }

        if (nodeSaver.handleActivityResult(requestCode, resultCode, data)) {
            return
        }

        viewModel.handleActivityResult(requestCode, resultCode, data, this, this)
    }

    fun setToolbarTitle(title: String) {
        toolbar.title = title
    }

    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    fun hideToolbar(animate: Boolean = true) {
        if (animate) {
            toolbar.animate()
                .translationY(-toolbar.measuredHeight.toFloat())
                .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            toolbar.animate().cancel()
            toolbar.translationY = -toolbar.measuredHeight.toFloat()
        }

        if (!isAudioPlayer()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    fun showToolbar(animate: Boolean = true) {
        if (animate) {
            toolbar.animate()
                .translationY(0F)
                .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                .start()
        } else {
            toolbar.animate().cancel()
            toolbar.translationY = 0F
        }

        if (!isAudioPlayer()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    fun showToolbarElevation(withElevation: Boolean) {
        // This is the actual color when using Util.changeToolBarElevation, but video player
        // use different toolbar theme (to force dark theme), which breaks
        // Util.changeToolBarElevation, so we just use the actual color here.
        val darkElevationColor = Color.parseColor("#282828")

        if (!isAudioPlayer() || Util.isDarkMode(this)) {
            toolbar.setBackgroundColor(
                when {
                    withElevation -> darkElevationColor
                    isAudioPlayer() -> Color.TRANSPARENT
                    else -> ContextCompat.getColor(this, R.color.dark_grey)
                }
            )

            post {
                window.statusBarColor = if (withElevation) darkElevationColor else Color.BLACK
            }
        } else {
            toolbar.elevation =
                if (withElevation) resources.getDimension(R.dimen.toolbar_elevation) else 0F
        }
    }

    fun setDraggable(draggable: Boolean) {
        dragToExit.setDraggable(draggable)
    }

    private fun onDragActivated(activated: Boolean) {
        getFragmentFromNavHost(R.id.nav_host_fragment, MediaPlayerFragment::class.java)
            ?.onDragActivated(dragToExit, activated)
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, rootLayout, content, chatId)
    }

    override fun launchActivity(intent: Intent) {
        startActivity(intent)
        stopPlayer()
    }

    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    fun isAudioPlayer() = isAudioPlayer(intent)

    companion object {
        fun isAudioPlayer(intent: Intent?): Boolean {
            val nodeName = intent?.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: return true

            return MimeTypeList.typeForName(nodeName).isAudio
        }
    }
}
