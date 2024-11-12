package mega.privacy.android.app.main.megachat;

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_ERROR_COPYING_NODES;
import static mega.privacy.android.app.constants.BroadcastConstants.ERROR_MESSAGE_TEXT;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponentKt.createStartTransferView;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.ChatUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.ColorUtils.getColorHexString;
import static mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER;
import static mega.privacy.android.app.utils.Constants.FORWARD_ONLY_OPTION;
import static mega.privacy.android.app.utils.Constants.FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.ID_MESSAGES;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CHAT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER;
import static mega.privacy.android.app.utils.Constants.SELECTED_CHATS;
import static mega.privacy.android.app.utils.Constants.SELECTED_USERS;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.primitives.Longs;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract;
import mega.privacy.android.app.arch.extensions.ViewExtensionsKt;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.extensions.EdgeToEdgeExtensionsKt;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.interfaces.StoreDataBeforeForward;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.MultipleForwardChatProcessor;
import mega.privacy.android.app.main.megachat.chatAdapters.NodeAttachmentHistoryAdapter;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.NodeAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.presentation.chat.NodeAttachmentHistoryViewModel;
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper;
import mega.privacy.android.app.presentation.extensions.StorageStateExtensionsKt;
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity;
import mega.privacy.android.app.presentation.imagepreview.fetcher.ChatImageNodeFetcher;
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource;
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource;
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity;
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.utils.permission.PermissionUtils;
import mega.privacy.android.domain.entity.StorageState;
import mega.privacy.android.domain.entity.node.NameCollision;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatNodeHistoryListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@AndroidEntryPoint
public class NodeAttachmentHistoryActivity extends PasscodeActivity implements
        MegaChatRequestListenerInterface, MegaChatNodeHistoryListenerInterface,
        StoreDataBeforeForward<ArrayList<MegaChatMessage>>, SnackbarShower {

    @Inject
    CopyRequestMessageMapper copyRequestMessageMapper;

    private NodeAttachmentHistoryViewModel viewModel;
    private StartDownloadViewModel startDownloadViewModel;

    public static int NUMBER_MESSAGES_TO_LOAD = 20;
    public static int NUMBER_MESSAGES_BEFORE_LOAD = 8;

    ActionBar aB;
    MaterialToolbar tB;
    NodeAttachmentHistoryActivity nodeAttachmentHistoryActivity = this;

    RelativeLayout container;
    RecyclerView listView;
    LinearLayoutManager mLayoutManager;
    RelativeLayout emptyLayout;
    TextView emptyTextView;
    ImageView emptyImageView;

    MenuItem importIcon;
    private MenuItem thumbViewMenuItem;

    ArrayList<MegaChatMessage> messages;
    ArrayList<MegaChatMessage> bufferMessages;

    public MegaChatRoom chatRoom;

    NodeAttachmentHistoryAdapter adapter;
    boolean scrollingUp = false;
    boolean getMoreHistory = false;
    boolean isLoadingHistory = false;

    private ActionMode actionMode;
    DisplayMetrics outMetrics;

    AlertDialog statusDialog;

    MenuItem selectMenuItem;
    MenuItem unSelectMenuItem;

    Handler handler;
    int stateHistory;
    public long chatId = -1;
    public long selectedMessageId = -1;

    ChatController chatC;

    private MegaNode myChatFilesFolder;
    private ArrayList<MegaChatMessage> preservedMessagesSelected;
    private ArrayList<MegaChatMessage> preservedMessagesToImport;

    private NodeAttachmentBottomSheetDialogFragment bottomSheetDialogFragment;

    private final ActivityResultLauncher<ArrayList<NameCollision>> nameCollisionActivityLauncher = registerForActivityResult(
            new NameCollisionActivityContract(),
            result -> {
                if (result != null) {
                    showSnackbar(SNACKBAR_TYPE, result, INVALID_HANDLE);
                }
            }
    );

    private final BroadcastReceiver errorCopyingNodesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !BROADCAST_ACTION_ERROR_COPYING_NODES.equals(intent.getAction())) {
                return;
            }

            removeProgressDialog();
            showSnackbar(SNACKBAR_TYPE, intent.getStringExtra(ERROR_MESSAGE_TEXT));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NodeAttachmentHistoryViewModel.class);
        startDownloadViewModel = new ViewModelProvider(this).get(StartDownloadViewModel.class);

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        chatC = new ChatController(this);

        megaChatApi.addNodeHistoryListener(chatId, this);

        handler = new Handler();

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        registerReceiver(errorCopyingNodesReceiver,
                new IntentFilter(BROADCAST_ACTION_ERROR_COPYING_NODES));
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_node_history);
        tB = findViewById(R.id.toolbar_node_history);
        EdgeToEdgeExtensionsKt.consumeInsetsWithToolbar(this, tB);
        addStartDownloadTransferView();

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong("chatId", -1);
        }

        //Set toolbar
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);

        aB.setTitle(getString(R.string.title_chat_shared_files_info));

        container = findViewById(R.id.node_history_main_layout);
        emptyLayout = findViewById(R.id.empty_layout_node_history);
        emptyTextView = findViewById(R.id.empty_text_node_history);
        emptyImageView = findViewById(R.id.empty_image_view_node_history);

        ColorUtils.setImageViewAlphaIfDark(this, emptyImageView, ColorUtils.DARK_IMAGE_ALPHA);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
        } else {
            emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
        }

        String textToShow = String.format(getString(R.string.context_empty_shared_files));
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'" + getColorHexString(this, R.color.grey_900_grey_100) + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'" + getColorHexString(this, R.color.grey_300_grey_600) + "\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception ignored) {
        }
        Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        emptyTextView.setText(result);

        listView = findViewById(R.id.node_history_list_view);
        listView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(mLayoutManager);
        listView.setItemAnimator(noChangeRecyclerViewItemAnimator());

        listView.setClipToPadding(false);
        listView.setHasFixedSize(true);

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                if (stateHistory != MegaChatApi.SOURCE_NONE) {
                    if (dy > 0) {
                        // Scrolling up
                        scrollingUp = true;
                    } else {
                        // Scrolling down
                        scrollingUp = false;
                    }

                    if (scrollingUp) {
                        int pos = mLayoutManager.findFirstVisibleItemPosition();

                        if (pos <= NUMBER_MESSAGES_BEFORE_LOAD && getMoreHistory) {
                            Timber.d("DE->loadAttachments:scrolling down");
                            isLoadingHistory = true;
                            stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
                            getMoreHistory = false;
                        }
                    }
                }
                checkScroll();
            }
        });


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (chatId == -1) {
                chatId = extras.getLong("chatId");
            }

            chatRoom = megaChatApi.getChatRoom(chatId);

            if (chatRoom != null) {
                messages = new ArrayList<>();
                bufferMessages = new ArrayList<>();

                if (!messages.isEmpty()) {
                    emptyLayout.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                } else {
                    emptyLayout.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                }

                boolean resultOpen = megaChatApi.openNodeHistory(chatId, this);
                if (resultOpen) {
                    Timber.d("Node history opened correctly");

                    messages = new ArrayList<>();

                    if (adapter == null) {
                        adapter = new NodeAttachmentHistoryAdapter(this, messages, listView);
                    }

                    listView.setAdapter(adapter);
                    adapter.setMultipleSelect(false);

                    adapter.setMessages(messages);

                    isLoadingHistory = true;
                    Timber.d("A->loadAttachments");
                    stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
                }
            } else {
                Timber.e("ERROR: node is NULL");
            }
        }

        // Observe snackbar message event
        ViewExtensionsKt.collectFlow(this, viewModel.getSnackbarMessageEvent(), Lifecycle.State.STARTED, messageId -> {
            if (messageId == null) return Unit.INSTANCE;
            showSnackbar(SNACKBAR_TYPE, getString(messageId), MEGACHAT_INVALID_HANDLE);
            viewModel.onSnackbarMessageConsumed();
            return Unit.INSTANCE;
        });

        // Observe event to save chat file to offline
        ViewExtensionsKt.collectFlow(this, viewModel.getStartChatFileOfflineDownloadEvent(), Lifecycle.State.STARTED, chatFile -> {
            if (chatFile == null) return Unit.INSTANCE;
            startDownloadViewModel.onSaveOfflineClicked(chatFile);
            viewModel.onStartChatFileOfflineDownloadEventConsumed();
            return Unit.INSTANCE;
        });

        // Observe copy request result
        ViewExtensionsKt.collectFlow(this, viewModel.getCopyResultFlow(), Lifecycle.State.STARTED, copyResult -> {
            if (copyResult == null) return Unit.INSTANCE;
            dismissAlertDialogIfExists(statusDialog);

            Throwable copyThrowable = copyResult.getError();
            if (copyThrowable != null) {
                manageCopyMoveException(copyThrowable);
            }

            showSnackbar(SNACKBAR_TYPE, copyResult.getResult() != null
                            ? copyRequestMessageMapper.invoke(copyResult.getResult())
                            : getString(R.string.import_success_error),
                    MEGACHAT_INVALID_HANDLE);
            viewModel.copyResultConsumed();
            return Unit.INSTANCE;
        });

        // Observe node collision result
        ViewExtensionsKt.collectFlow(this, viewModel.getCollisionsFlow(), Lifecycle.State.STARTED, collisions -> {
            if (collisions == null) return Unit.INSTANCE;
            dismissAlertDialogIfExists(statusDialog);
            if (!collisions.isEmpty() && nameCollisionActivityLauncher != null) {
                nameCollisionActivityLauncher.launch(new ArrayList<>(collisions));
                viewModel.nodeCollisionsConsumed();
            }
            return Unit.INSTANCE;
        });

        ViewExtensionsKt.collectFlow(this, viewModel.getMediaPlayerOpenedErrorFlow(), Lifecycle.State.STARTED, errorState -> {
            if (errorState == null) return Unit.INSTANCE;
            Timber.w("No available Intent");
            showNodeAttachmentBottomSheet(errorState.getMessage(), errorState.getPosition());
            viewModel.updateMediaPlayerOpenedError(null);
            return Unit.INSTANCE;
        });
    }

    private void addStartDownloadTransferView() {
        ViewGroup root = findViewById(R.id.node_history_main_layout);
        root.addView(
                createStartTransferView(
                        this,
                        startDownloadViewModel.getState(),
                        () -> {
                            startDownloadViewModel.consumeDownloadEvent();
                            return Unit.INSTANCE;
                        },
                        (StartTransferEvent) -> Unit.INSTANCE
                )
        );
    }

    @Override
    protected void onDestroy() {
        Timber.d("onDestroy");
        super.onDestroy();
        unregisterReceiver(errorCopyingNodesReceiver);

        if (megaChatApi != null) {
            megaChatApi.removeNodeHistoryListener(chatId, this);
            megaChatApi.closeNodeHistory(chatId, null);
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_node_history, menu);

        selectMenuItem = menu.findItem(R.id.action_select);
        unSelectMenuItem = menu.findItem(R.id.action_unselect);
        thumbViewMenuItem = menu.findItem(R.id.action_grid);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        selectMenuItem.setVisible(!messages.isEmpty());

        unSelectMenuItem.setVisible(false);
        thumbViewMenuItem.setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_select) {
            selectAll();
            return true;
        } else if (itemId == R.id.action_grid) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void activateActionMode() {
        Timber.d("activateActionMode");
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            adapter.notifyDataSetChanged();
            actionMode = startSupportActionMode(new NodeAttachmentHistoryActivity.ActionBarCallBack());
        }
    }

    // Clear all selected items
    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
    }

    public void selectAll() {
        Timber.d("selectAll");
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();

                actionMode = startSupportActionMode(new ActionBarCallBack());
            }
            new Handler(Looper.getMainLooper()).post(this::updateActionModeTitle);
        }
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);
        megaChatApi.signalPresenceActivity();

        if (position < messages.size()) {
            MegaChatMessage m = messages.get(position);

            if (adapter.isMultipleSelect()) {
                adapter.toggleSelection(position);
                if (!adapter.getSelectedMessages().isEmpty()) {
                    updateActionModeTitle();
                }
            } else {
                if (m != null) {
                    MegaNodeList nodeList = m.getMegaNodeList();
                    if (nodeList.size() == 1) {
                        MegaNode node = nodeList.get(0);

                        if (MimeTypeList.typeForName(node.getName()).isImage()) {
                            if (node.hasPreview()) {
                                Timber.d("Show full screen viewer");
                                showFullScreenViewer(m.getMsgId());
                            } else {
                                Timber.d("Image without preview - show node attachment panel for one node");
                                showNodeAttachmentBottomSheet(m, position);
                            }
                        } else if (MimeTypeList.typeForName(node.getName()).isVideoMimeType() || MimeTypeList.typeForName(node.getName()).isAudio()) {
                            viewModel.openMediaPlayer(
                                    this,
                                    node.getHandle(),
                                    m,
                                    chatId,
                                    node.getName(),
                                    position
                            );
                        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                            Timber.d("isFile:isPdf");
                            String mimeType = MimeTypeList.typeForName(node.getName()).getType();
                            Timber.d("FILE HANDLE: %d, TYPE: %s", node.getHandle(), mimeType);
                            Intent pdfIntent = new Intent(this, PdfViewerActivity.class);
                            pdfIntent.putExtra("inside", true);
                            pdfIntent.putExtra("adapterType", FROM_CHAT);
                            pdfIntent.putExtra("msgId", m.getMsgId());
                            pdfIntent.putExtra("chatId", chatId);

                            pdfIntent.putExtra("FILENAME", node.getName());

                            String localPath = getLocalFile(node);
                            if (localPath != null) {
                                File mediaFile = new File(localPath);
                                if (localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    Timber.d("File Provider Option");
                                    Uri mediaFileUri = FileProvider.getUriForFile(this, AUTHORITY_STRING_FILE_PROVIDER, mediaFile);
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR: NULL media file Uri");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    } else {
                                        pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                    }
                                } else {
                                    Uri mediaFileUri = Uri.fromFile(mediaFile);
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR: NULL media file Uri");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    } else {
                                        pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                    }
                                }
                                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else {
                                Timber.w("Local Path NULL");
                                if (viewModel.isOnline()) {
                                    if (megaApi.httpServerIsRunning() == 0) {
                                        megaApi.httpServerStart();
                                        pdfIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                                    } else {
                                        Timber.w("ERROR: HTTP server already running");
                                    }
                                    String url = megaApi.httpServerGetLocalLink(node);
                                    if (url != null) {
                                        Uri parsedUri = Uri.parse(url);
                                        if (parsedUri != null) {
                                            pdfIntent.setDataAndType(parsedUri, mimeType);
                                        } else {
                                            Timber.e("ERROR: HTTP server get local link");
                                            showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                        }
                                    } else {
                                        Timber.e("ERROR: HTTP server get local link");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    }
                                } else {
                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem) + ". " + getString(R.string.no_network_connection_on_play_file));
                                }
                            }
                            pdfIntent.putExtra("HANDLE", node.getHandle());

                            if (isIntentAvailable(this, pdfIntent)) {
                                startActivity(pdfIntent);
                            } else {
                                Timber.w("No svailable Intent");
                                showNodeAttachmentBottomSheet(m, position);
                            }
                            overridePendingTransition(0, 0);
                        } else if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
                            manageTextFileIntent(this, m.getMsgId(), chatId);
                        } else {
                            Timber.d("NOT Image, pdf, audio or video - show node attachment panel for one node");
                            showNodeAttachmentBottomSheet(m, position);
                        }
                    } else {
                        Timber.d("Show node attachment panel");
                        showNodeAttachmentBottomSheet(m, position);
                    }
                }
            }
        } else {
            Timber.w("DO NOTHING: Position (%d) is more than size in messages (size: %d)", position, messages.size());
        }
    }

    public void showFullScreenViewer(long msgId) {
        long currentNodeHandle = INVALID_HANDLE;
        List<Long> messageIds = new ArrayList<>();

        for (MegaChatMessage message : messages) {
            messageIds.add(message.getMsgId());
            if (message.getMsgId() == msgId) {
                currentNodeHandle = message.getMegaNodeList().get(0).getHandle();
            }
        }

        Map<String, Object> previewParams = new HashMap<>();
        previewParams.put(ChatImageNodeFetcher.CHAT_ROOM_ID, chatId);
        previewParams.put(ChatImageNodeFetcher.MESSAGE_IDS, Longs.toArray(messageIds));

        Intent intent = ImagePreviewActivity.Companion.createSecondaryIntent(
                this,
                ImagePreviewFetcherSource.CHAT,
                ImagePreviewMenuSource.CHAT,
                currentNodeHandle,
                previewParams,
                false
        );
        startActivity(intent);
    }

    private void updateActionModeTitle() {
        Timber.d("updateActionModeTitle");
        if (actionMode == null) {
            return;
        }

        int num = adapter.getSelectedItemCount();
        try {
            actionMode.setTitle(num + "");
            actionMode.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
            Timber.e(e, "Invalidate error");
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        adapter.setMultipleSelect(false);
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if (chatRoom != null) {
            outState.putLong("chatId", chatRoom.getChatId());
        }
    }

    @Override
    public void storedUnhandledData(ArrayList<MegaChatMessage> preservedData) {
    }

    @Override
    public void handleStoredData() {
        chatC.proceedWithForwardOrShare(this, myChatFilesFolder, preservedMessagesSelected,
                preservedMessagesToImport, chatId, FORWARD_ONLY_OPTION);
        preservedMessagesSelected = null;
        preservedMessagesToImport = null;
    }

    @Override
    public void storedUnhandledData(ArrayList<MegaChatMessage> messagesSelected, ArrayList<MegaChatMessage> messagesToImport) {
        preservedMessagesSelected = messagesSelected;
        preservedMessagesToImport = messagesToImport;
    }

    @Override
    public void showSnackbar(int type, @Nullable String content, long chatId) {
        showSnackbar(type, container, content, chatId);
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Timber.d("onActionItemClicked");
            final ArrayList<MegaChatMessage> messagesSelected = adapter.getSelectedMessages();

            if (viewModel.getStorageState() == StorageState.PayWall &&
                    item.getItemId() != R.id.cab_menu_select_all && item.getItemId() != R.id.cab_menu_unselect_all) {
                showOverDiskQuotaPaywallWarning();
                return false;
            }

            int itemId = item.getItemId();
            if (itemId == R.id.cab_menu_select_all) {
                selectAll();
            } else if (itemId == R.id.cab_menu_unselect_all) {
                clearSelections();
            } else if (itemId == R.id.chat_cab_menu_forward) {
                Timber.d("Forward message");
                clearSelections();
                hideMultipleSelect();
                forwardMessages(messagesSelected);
            } else if (itemId == R.id.chat_cab_menu_delete) {
                clearSelections();
                hideMultipleSelect();
                //Delete
                showConfirmationDeleteMessages(messagesSelected, chatRoom);
            } else if (itemId == R.id.chat_cab_menu_download) {
                clearSelections();
                hideMultipleSelect();
                ArrayList<Long> messageIds = new ArrayList<>();
                for (MegaChatMessage message : messagesSelected) {
                    Long megaNodeHandle = message.getMsgId();
                    messageIds.add(megaNodeHandle);
                }
                startDownloadViewModel.onDownloadClicked(
                        chatId,
                        messageIds);
            } else if (itemId == R.id.chat_cab_menu_import) {
                clearSelections();
                hideMultipleSelect();
                chatC.importNodesFromMessages(messagesSelected);
            } else if (itemId == R.id.chat_cab_menu_offline) {
                PermissionUtils.checkNotificationsPermission(nodeAttachmentHistoryActivity);
                clearSelections();
                hideMultipleSelect();
                long messageId = messagesSelected.get(0).getMsgId();
                if (StorageStateExtensionsKt.getStorageState() == StorageState.PayWall) {
                    AlertsAndWarnings.showOverDiskQuotaPaywallWarning();
                } else {
                    viewModel.saveChatNodeToOffline(chatId, messageId);
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.messages_node_history_action, menu);

            importIcon = menu.findItem(R.id.chat_cab_menu_import);
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            Timber.d("onDestroyActionMode");
            adapter.clearSelections();
            adapter.notifyDataSetChanged();
            adapter.setMultipleSelect(false);
            checkScroll();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("onPrepareActionMode");
            List<MegaChatMessage> selected = adapter.getSelectedMessages();
            if (!selected.isEmpty()) {
                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                if (selected.size() == adapter.getItemCount()) {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    unselect.setTitle(getString(R.string.action_unselect_all));
                    unselect.setVisible(true);
                }

                if (chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO && !chatRoom.isPreview()) {
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                } else {
                    Timber.d("Chat with permissions");
                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(viewModel.isOnline() && !chatC.isInAnonymousMode());

                    if (selected.size() == 1) {
                        if (selected.get(0).getUserHandle() == megaChatApi.getMyUserHandle() && selected.get(0).isDeletable()) {
                            Timber.d("One message - Message DELETABLE");
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                        } else {
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                        }

                        if (viewModel.isOnline()) {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
                            if (chatC.isInAnonymousMode()) {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                importIcon.setVisible(false);
                            } else {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(true);
                                importIcon.setVisible(true);
                            }
                        } else {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);
                        }

                    } else {
                        Timber.d("Many items selected");
                        boolean showDelete = true;
                        boolean allNodeAttachments = true;

                        for (int i = 0; i < selected.size(); i++) {

                            if (showDelete) {
                                if (selected.get(i).getUserHandle() == megaChatApi.getMyUserHandle()) {
                                    if (!(selected.get(i).isDeletable())) {
                                        showDelete = false;
                                    }

                                } else {
                                    showDelete = false;
                                }
                            }

                            if (allNodeAttachments) {
                                if (selected.get(i).getType() != MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                                    allNodeAttachments = false;
                                }
                            }
                        }

                        if (viewModel.isOnline()) {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
                            importIcon.setVisible(!chatC.isInAnonymousMode());
                        } else {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            importIcon.setVisible(false);
                        }

                        menu.findItem(R.id.chat_cab_menu_delete).setVisible(showDelete);
                        menu.findItem(R.id.chat_cab_menu_forward).setVisible(viewModel.isOnline() && !chatC.isInAnonymousMode());
                        // Hide available offline option when multiple attachments are selected
                        menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                    }
                }
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
            }
            return false;
        }
    }

    public void showConfirmationDeleteMessages(final ArrayList<MegaChatMessage> messages, final MegaChatRoom chat) {
        Timber.d("Chat ID: %s", chat.getChatId());

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    ChatController cC = new ChatController(nodeAttachmentHistoryActivity);
                    cC.deleteMessages(messages, chat);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        if (messages.size() == 1) {
            builder.setMessage(R.string.confirmation_delete_one_message);
        } else {
            builder.setMessage(R.string.confirmation_delete_several_messages);
        }
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, dialogClickListener).show();
    }

    public void forwardMessages(ArrayList<MegaChatMessage> messagesSelected) {
        Timber.d("forwardMessages");
        chatC.prepareMessagesToForward(messagesSelected, chatId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Timber.d("Result Code: %s", resultCode);

        if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            if (!viewModel.isOnline() || megaApi == null) {
                try {
                    statusDialog.dismiss();
                } catch (Exception ignored) {
                }
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
                return;
            }
            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);
            final long[] importMessagesHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT");

            importNodes(toHandle, importMessagesHandles);
        } else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK) {
            if (!viewModel.isOnline()) {
                try {
                    statusDialog.dismiss();
                } catch (Exception ignored) {
                }

                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
                return;
            }

            showProgressForwarding();

            long[] idMessages = intent.getLongArrayExtra(ID_MESSAGES);
            long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
            long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);

            if (chatHandles != null && chatHandles.length > 0 && idMessages != null) {
                if (contactHandles != null && contactHandles.length > 0) {
                    ArrayList<MegaUser> users = new ArrayList<>();
                    ArrayList<MegaChatRoom> chats = new ArrayList<>();

                    for (long contactHandle : contactHandles) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandle));
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    for (long chatHandle : chatHandles) {
                        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandle);
                        if (chatRoom != null) {
                            chats.add(chatRoom);
                        }
                    }

                    CreateChatListener listener = new CreateChatListener(
                            CreateChatListener.SEND_MESSAGES, chats, users, this, this, idMessages,
                            chatId);

                    for (MegaUser user : users) {
                        MegaChatPeerList peers = MegaChatPeerList.createInstance();
                        peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                        megaChatApi.createChat(false, peers, listener);
                    }
                } else {
                    int countChat = chatHandles.length;
                    Timber.d("Selected: %d chats to send", countChat);

                    MultipleForwardChatProcessor forwardChatProcessor = new MultipleForwardChatProcessor(this, chatHandles, idMessages, chatId);
                    forwardChatProcessor.forward(chatRoom);
                }
            } else {
                Timber.e("Error on sending to chat");
            }
        }
    }

    public void showProgressForwarding() {
        Timber.d("showProgressForwarding");

        statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_forwarding));
        statusDialog.show();
    }

    public void removeProgressDialog() {
        try {
            statusDialog.dismiss();
        } catch (Exception ex) {
            Timber.e(ex);
        }
    }

    public void importNodes(final long toHandle, final long[] importMessagesHandles) {
        if (importMessagesHandles == null) return;
        statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_importing));
        statusDialog.show();
        List<Long> messageIds = new ArrayList<>();
        for (long id : importMessagesHandles) messageIds.add(id);
        viewModel.importChatNodes(chatId, messageIds, toHandle);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onAttachmentLoaded(MegaChatApiJava api, MegaChatMessage msg) {
        if (msg != null) {
            Timber.d("Message ID%s", msg.getMsgId());
            if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {

                MegaNodeList nodeList = msg.getMegaNodeList();
                if (nodeList != null) {

                    if (nodeList.size() == 1) {
                        MegaNode node = nodeList.get(0);
                        Timber.d("Node Handle: %s", node.getHandle());
                        bufferMessages.add(msg);
                        Timber.d("Size of buffer: %s", bufferMessages.size());
                        Timber.d("Size of messages: %s", messages.size());
                    }
                }
            }
        } else {
            Timber.d("Message is NULL: end of history");
            if ((bufferMessages.size() + messages.size()) >= NUMBER_MESSAGES_TO_LOAD) {
                fullHistoryReceivedOnLoad();
                isLoadingHistory = false;
            } else {
                Timber.d("Less Number Received");
                if ((stateHistory != MegaChatApi.SOURCE_NONE) && (stateHistory != MegaChatApi.SOURCE_ERROR) && stateHistory != MegaChatApi.SOURCE_INVALID_CHAT) {
                    Timber.d("But more history exists --> loadAttachments");
                    isLoadingHistory = true;
                    stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
                    Timber.d("New state of history: %s", stateHistory);
                    getMoreHistory = false;
                    if (stateHistory == MegaChatApi.SOURCE_NONE || stateHistory == MegaChatApi.SOURCE_ERROR || stateHistory == MegaChatApi.SOURCE_INVALID_CHAT) {
                        fullHistoryReceivedOnLoad();
                        isLoadingHistory = false;
                    }
                } else {
                    Timber.d("New state of history: %s", stateHistory);
                    fullHistoryReceivedOnLoad();
                    isLoadingHistory = false;
                }
            }
        }
    }

    public void fullHistoryReceivedOnLoad() {
        Timber.d("Messages size: %s", messages.size());

        if (!bufferMessages.isEmpty()) {
            Timber.d("Buffer size: %s", bufferMessages.size());
            emptyLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            ListIterator<MegaChatMessage> itr = bufferMessages.listIterator();
            while (itr.hasNext()) {
                int currentIndex = itr.nextIndex();
                MegaChatMessage messageToShow = itr.next();
                messages.add(messageToShow);
            }

            if (!messages.isEmpty()) {
                if (adapter == null) {
                    adapter = new NodeAttachmentHistoryAdapter(this, messages, listView);
                    listView.setLayoutManager(mLayoutManager);
                    listView.addItemDecoration(new SimpleDividerItemDecoration(this));
                    listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);
                            checkScroll();
                        }
                    });
                    listView.setAdapter(adapter);
                    adapter.setMessages(messages);
                } else {
                    adapter.loadPreviousMessages(messages, bufferMessages.size());
                }

            }
            bufferMessages.clear();
        }

        Timber.d("getMoreHistoryTRUE");
        getMoreHistory = true;

        invalidateOptionsMenu();
    }

    @Override
    public void onAttachmentReceived(MegaChatApiJava api, MegaChatMessage msg) {
        Timber.d("STATUS: %s", msg.getStatus());
        Timber.d("TEMP ID: %s", msg.getTempId());
        Timber.d("FINAL ID: %s", msg.getMsgId());
        Timber.d("TIMESTAMP: %s", msg.getTimestamp());
        Timber.d("TYPE: %s", msg.getType());

        int lastIndex = 0;
        if (messages.isEmpty()) {
            messages.add(msg);
        } else {
            Timber.d("Status of message: %s", msg.getStatus());

            while (messages.get(lastIndex).getMsgIndex() > msg.getMsgIndex()) {
                lastIndex++;
            }

            Timber.d("Append in position: %s", lastIndex);
            messages.add(lastIndex, msg);
        }

        //Create adapter
        if (adapter == null) {
            Timber.d("Create adapter");
            adapter = new NodeAttachmentHistoryAdapter(this, messages, listView);
            listView.setLayoutManager(mLayoutManager);
            listView.addItemDecoration(new SimpleDividerItemDecoration(this));
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkScroll();
                }
            });
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        } else {
            Timber.d("Update adapter with last index: %s", lastIndex);
            if (lastIndex < 0) {
                Timber.d("Arrives the first message of the chat");
                adapter.setMessages(messages);
            } else {
                adapter.addMessage(messages, lastIndex + 1);
                adapter.notifyItemChanged(lastIndex);
            }
        }

        emptyLayout.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        invalidateOptionsMenu();
    }

    @Override
    public void onAttachmentDeleted(MegaChatApiJava api, long msgid) {
        Timber.d("Message ID: %s", msgid);

        int indexToChange = -1;

        ListIterator<MegaChatMessage> itr = messages.listIterator();
        while (itr.hasNext()) {
            MegaChatMessage messageToCheck = itr.next();
            if (messageToCheck.getTempId() == msgid) {
                indexToChange = itr.previousIndex();
                break;
            }
            if (messageToCheck.getMsgId() == msgid) {
                indexToChange = itr.previousIndex();
                break;
            }
        }

        if (indexToChange != -1) {
            messages.remove(indexToChange);
            Timber.d("Removed index: %d, Messages size: %d", indexToChange, messages.size());

            adapter.removeMessage(indexToChange, messages);

            if (messages.isEmpty()) {
                emptyLayout.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            }
        } else {
            Timber.w("Index to remove not found");
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onTruncate(MegaChatApiJava api, long msgid) {
        Timber.d("Message ID: %s", msgid);
        invalidateOptionsMenu();
        messages.clear();
        adapter.notifyDataSetChanged();
        listView.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    public void showNodeAttachmentBottomSheet(MegaChatMessage message, int position) {
        Timber.d("showNodeAttachmentBottomSheet: %s", position);

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMsgId();
        bottomSheetDialogFragment = NodeAttachmentBottomSheetDialogFragment.newInstance(chatId, selectedMessageId);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showSnackbar(int type, String s) {
        showSnackbar(type, container, s);
    }

    public void checkScroll() {
        if (listView != null) {
            boolean withElevation = listView.canScrollVertically(-1)
                    || (adapter != null && adapter.isMultipleSelect());
            float elevation = getResources().getDimension(R.dimen.toolbar_elevation);
            tB.setElevation(withElevation ? elevation : 0);
        }
    }

    public MegaChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setMyChatFilesFolder(MegaNode myChatFilesFolder) {
        this.myChatFilesFolder = myChatFilesFolder;
    }
}