package mega.privacy.android.app.main;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_MANAGE_SHARE;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE;
import static mega.privacy.android.app.listeners.ShareListener.CHANGE_PERMISSIONS_LISTENER;
import static mega.privacy.android.app.listeners.ShareListener.REMOVE_SHARE_LISTENER;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_BOTH;
import static mega.privacy.android.app.utils.Constants.NAME;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CONTACT;
import static mega.privacy.android.app.utils.ContactUtil.openContactInfoActivity;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE;
import static mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog;
import static mega.privacy.android.app.utils.Util.changeViewElevation;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaShare.ACCESS_READ;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.arch.extensions.ViewExtensionsKt;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.main.adapters.MegaSharedFolderAdapter;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.main.legacycontact.AddContactActivity;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.FileContactsListBottomSheetDialogListener;
import mega.privacy.android.app.presentation.contact.FileContactListActivityExtensionKt;
import mega.privacy.android.app.presentation.contact.FileContactListViewModel;
import mega.privacy.android.app.psa.PsaWebBrowser;
import mega.privacy.android.app.sync.fileBackups.FileBackupManager;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaSet;
import nz.mega.sdk.MegaSetElement;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;
import timber.log.Timber;

@AndroidEntryPoint
public class FileContactListActivity extends PasscodeActivity implements OnClickListener, MegaGlobalListenerInterface, FileContactsListBottomSheetDialogListener {

    public FileContactListViewModel viewModel;

    private ContactController contactController;
    public NodeController nodeController;
    ActionBar aB;
    Toolbar tB;
    FileContactListActivity fileContactListActivity = this;
    MegaShare selectedShare;

    CoordinatorLayout coordinatorLayout;
    RelativeLayout container;
    RecyclerView listView;
    LinearLayoutManager mLayoutManager;
    ImageView emptyImage;
    TextView emptyText;
    private TextView warningText;
    FloatingActionButton fab;

    ArrayList<MegaShare> listContacts;
    List<MegaShare> tempListContacts;


    long nodeHandle;
    MegaNode node;
    ArrayList<MegaNode> contactNodes;

    MegaSharedFolderAdapter adapter;

    long parentHandle = -1;

    Stack<Long> parentHandleStack = new Stack<>();

    private ActionMode actionMode;

    AlertDialog statusDialog;
    AlertDialog permissionsDialog;

    MenuItem addSharingContact;
    MenuItem selectMenuItem;
    MenuItem unSelectMenuItem;

    Handler handler;
    DisplayMetrics outMetrics;

    private MaterialAlertDialogBuilder dialogBuilder;

    private FileContactsListBottomSheetDialogFragment bottomSheetDialogFragment;

    private FileBackupManager fileBackupManager;

    private BroadcastReceiver manageShareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (adapter != null) {
                if (adapter.isMultipleSelect()) {
                    adapter.clearSelections();
                    hideMultipleSelect();
                }
                adapter.setShareList(listContacts);
            }

            if (permissionsDialog != null) {
                permissionsDialog.dismiss();
            }

            if (statusDialog != null) {
                statusDialog.dismiss();
            }
        }
    };

    private BroadcastReceiver contactUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            if (intent.getAction().equals(ACTION_UPDATE_NICKNAME)
                    || intent.getAction().equals(ACTION_UPDATE_CREDENTIALS)
                    || intent.getAction().equals(ACTION_UPDATE_FIRST_NAME)
                    || intent.getAction().equals(ACTION_UPDATE_LAST_NAME)) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE));
            }
        }
    };

    public void activateActionMode() {
        Timber.d("activateActionMode");
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new ActionBarCallBack());
        }
    }

    @Override
    public void onGlobalSyncStateChanged(@NonNull MegaApiJava api) {}
    private class ActionBarCallBack implements ActionMode.Callback {

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Timber.d("onActionItemClicked");
            final ArrayList<MegaShare> shares = adapter.getSelectedShares();

            int itemId = item.getItemId();
            if (itemId == R.id.action_file_contact_list_permissions) {//Change permissions
                dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));

                final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                dialogBuilder.setSingleChoiceItems(items, -1, (dialog, item1) -> {
                    clearSelections();
                    if (permissionsDialog != null) {
                        permissionsDialog.dismiss();
                    }
                    statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_permissions_changing_folder));
                    contactController.changePermissions(contactController.getEmailShares(shares), item1, node);
                });

                permissionsDialog = dialogBuilder.create();
                permissionsDialog.show();
            } else if (itemId == R.id.action_file_contact_list_delete) {
                if (shares != null && !shares.isEmpty()) {
                    if (shares.size() > 1) {
                        Timber.d("Remove multiple contacts");
                        showConfirmationRemoveMultipleContactFromShare(shares);
                    } else {
                        Timber.d("Remove one contact");
                        showConfirmationRemoveContactFromShare(shares.get(0).getUser());
                    }
                }
            } else if (itemId == R.id.cab_menu_select_all) {
                selectAll();
            } else if (itemId == R.id.cab_menu_unselect_all) {
                clearSelections();
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_contact_shared_browser_action, menu);
            fab.setVisibility(View.GONE);
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            Timber.d("onDestroyActionMode");
            adapter.clearSelections();
            adapter.setMultipleSelect(false);
            fab.setVisibility(View.VISIBLE);
            checkScroll();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("onPrepareActionMode");
            ArrayList<MegaShare> selected = adapter.getSelectedShares();
            boolean deleteShare = false;
            boolean permissions = false;

            if (selected.size() != 0) {
                permissions = true;
                deleteShare = true;

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
            } else {
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }

            MenuItem changePermissionsMenuItem = menu.findItem(R.id.action_file_contact_list_permissions);
            if (node != null && megaApi.isInInbox(node)) {
                // If the node came from Backups, hide the Change Permissions option from the Action Bar
                changePermissionsMenuItem.setVisible(false);
                changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            } else {
                // Otherwise, change Change Permissions visibility depending on whether there are
                // selected contacts or none
                changePermissionsMenuItem.setVisible(permissions);
                if (permissions) {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                } else {
                    changePermissionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }

            menu.findItem(R.id.action_file_contact_list_delete).setVisible(deleteShare);
            if (deleteShare) {
                menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else {
                menu.findItem(R.id.action_file_contact_list_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            }

            return false;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FileContactListViewModel.class);

        initFileBackupManager();

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        dialogBuilder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        megaApi.addGlobalListener(this);

        handler = new Handler();

        listContacts = new ArrayList<>();

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nodeHandle = extras.getLong(NAME);
            node = megaApi.getNodeByHandle(nodeHandle);

            setContentView(R.layout.activity_file_contact_list);

            //Set toolbar
            tB = findViewById(R.id.toolbar_file_contact_list);
            setSupportActionBar(tB);
            aB = getSupportActionBar();
            if (aB != null) {
                aB.setDisplayHomeAsUpEnabled(true);
                aB.setDisplayShowHomeEnabled(true);
                aB.setTitle(node.getName());
                aB.setSubtitle(R.string.file_properties_shared_folder_select_contact);
            }

            coordinatorLayout = findViewById(R.id.coordinator_layout_file_contact_list);
            container = findViewById(R.id.file_contact_list);

            fab = findViewById(R.id.floating_button_file_contact_list);
            fab.setOnClickListener(this);

            warningText = findViewById(R.id.file_contact_list_text_warning_message);

            listView = findViewById(R.id.file_contact_list_view_browser);
            listView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
            listView.setClipToPadding(false);
            listView.addItemDecoration(new SimpleDividerItemDecoration(this));
            mLayoutManager = new LinearLayoutManager(this);
            listView.setLayoutManager(mLayoutManager);
            listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkScroll();
                }
            });

            emptyImage = findViewById(R.id.file_contact_list_empty_image);
            emptyText = findViewById(R.id.file_contact_list_empty_text);
            emptyImage.setImageResource(R.drawable.ic_empty_contacts);
            emptyText.setText(R.string.contacts_list_empty_text);
        }

        contactController = new ContactController(this);
        nodeController = new NodeController(this);

        viewModel.getMegaShares(node);

        registerReceiver(manageShareReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE));

        IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS);
        registerReceiver(contactUpdateReceiver, contactUpdateFilter);
        collectFlows();
    }

    private void collectFlows() {
        ViewExtensionsKt.collectFlow(this, viewModel.getShowNotVerifiedContactBanner(), Lifecycle.State.STARTED, showBanner -> {
            warningText.setVisibility(showBanner ? View.VISIBLE : View.GONE);
            return Unit.INSTANCE;
        });
        ViewExtensionsKt.collectFlow(this, viewModel.getMegaShare(), Lifecycle.State.STARTED, shares -> {
            tempListContacts = shares;
            updateListView();
            return Unit.INSTANCE;
        });
    }

    /**
     * Initializes the FileBackupManager
     */
    private void initFileBackupManager() {
        fileBackupManager = new FileBackupManager(this, (actionType, operationType, result, handle) -> {
            if (actionType == ACTION_BACKUP_SHARE_FOLDER && operationType == OPERATION_EXECUTE) {
                shareFolder();
            }
        });
    }

    public void checkScroll() {
        if (listView != null) {
            changeViewElevation(aB, (listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE) || (adapter != null && adapter.isMultipleSelect()), outMetrics);
        }
    }


    public void showOptionsPanel(MegaShare sShare) {
        Timber.d("showNodeOptionsPanel");

        if (node == null || sShare == null || isBottomSheetDialogShown(bottomSheetDialogFragment))
            return;

        selectedShare = sShare;
        bottomSheetDialogFragment = new FileContactsListBottomSheetDialogFragment(selectedShare, getSelectedContact(), node, this);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (megaApi != null) {
            megaApi.removeGlobalListener(this);
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        unregisterReceiver(manageShareReceiver);
        unregisterReceiver(contactUpdateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_folder_contact_list, menu);

        menu.findItem(R.id.action_delete_version_history).setVisible(false);

        selectMenuItem = menu.findItem(R.id.action_select);
        unSelectMenuItem = menu.findItem(R.id.action_unselect);

        selectMenuItem.setVisible(true);
        unSelectMenuItem.setVisible(false);

        addSharingContact = menu.findItem(R.id.action_folder_contacts_list_share_folder);
        addSharingContact.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
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
        } else if (itemId == R.id.action_folder_contacts_list_share_folder) {
            handleShareFolder();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle the process of sharing the Folder to contacts
     * <p>
     * If the Folder is a Backup folder, a warning dialog is displayed and the shared folder can only be
     * configured in read-only mode.
     * <p>
     * Otherwise, no warning dialog is displayed and the shared folder can be configured in different
     * access modes (read-only, read and write, full access)
     */
    private void handleShareFolder() {
        int nodeType = checkBackupNodeTypeByHandle(megaApi, node);
        if (nodeType != BACKUP_NONE) {
            fileBackupManager.shareBackupsFolder(
                    nodeController,
                    node,
                    nodeType,
                    fileBackupManager.getDefaultActionBackupNodeCallback()
            );
        } else {
            shareFolder();
        }
    }

    /**
     * Starts a new Intent to share the folder to different contacts
     */
    private void shareFolder() {
        Intent intent = new Intent();
        intent.setClass(this, AddContactActivity.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);
        intent.putExtra("MULTISELECT", 0);
        intent.putExtra(AddContactActivity.EXTRA_NODE_HANDLE, node.getHandle());
        startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
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
            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    public boolean showSelectMenuItem() {
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }

        return false;
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);

        if (adapter.isMultipleSelect()) {
            adapter.toggleSelection(position);
            updateActionModeTitle();
        } else {
            MegaUser contact = megaApi.getContact(listContacts.get(position).getUser());
            if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                openContactInfoActivity(this, listContacts.get(position).getUser());
            }
        }
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed");
        PsaWebBrowser psaWebBrowser = getPsaWebBrowser();
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        retryConnectionsAndSignalPresence();

        if (adapter.getPositionClicked() != -1) {
            adapter.setPositionClicked(-1);
            adapter.notifyDataSetChanged();
        } else {
            if (parentHandleStack.isEmpty()) {
                super.onBackPressed();
            } else {
                parentHandle = parentHandleStack.pop();
                listView.setVisibility(View.VISIBLE);
                emptyImage.setVisibility(View.GONE);
                emptyText.setVisibility(View.GONE);
                if (parentHandle == -1) {
                    aB.setTitle(getString(R.string.file_properties_shared_folder_select_contact));
                    aB.setLogo(R.drawable.ic_action_navigation_accept_white);
                } else {
                    contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
                    aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
                    aB.setLogo(R.drawable.ic_action_navigation_previous_item);
                }
                supportInvalidateOptionsMenu();
                adapter.setShareList(listContacts);
                listView.scrollToPosition(0);
            }
        }
    }

    private void updateActionModeTitle() {
        Timber.d("updateActionModeTitle");
        if (actionMode == null) {
            return;
        }
        ArrayList<MegaShare> contacts = adapter.getSelectedShares();
        if (contacts != null) {
            Timber.d("Contacts selected: %s", contacts.size());
        }

        actionMode.setTitle(getResources().getQuantityString(R.plurals.general_selection_num_contacts,
                contacts.size(), contacts.size()));
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.floating_button_file_contact_list) {
            handleShareFolder();
        }
    }

    @Override
    public void removeFileContactShare(String userEmail) {
        notifyDataSetChanged();

        showConfirmationRemoveContactFromShare(selectedShare.getUser());
    }

    @Override
    public void fileContactsDialogDismissed() {
    }

    public void changePermissions(String userEmail) {
        Timber.d("changePermissions");
        notifyDataSetChanged();
        int nodeType = checkBackupNodeTypeByHandle(megaApi, node);
        if (nodeType != BACKUP_NONE) {
            showWarningDialog();
            return;
        }
        dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
        final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
        dialogBuilder.setSingleChoiceItems(items, selectedShare.getAccess(), (dialog, item) -> {
            statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_permissions_changing_folder));
            permissionsDialog.dismiss();
            contactController.changePermission(selectedShare.getUser(), item, node, new ShareListener(this, CHANGE_PERMISSIONS_LISTENER, 1));
        });
        permissionsDialog = dialogBuilder.create();
        permissionsDialog.show();
    }

    /**
     * Show the warning dialog when change the permissions of this folder
     *
     * @return The dialog
     */
    private AlertDialog showWarningDialog() {
        DialogInterface.OnClickListener dialogClickListener =
                (dialog, which) -> {
                    dialog.dismiss();
                };
        LayoutInflater layout = this.getLayoutInflater();
        View view = layout.inflate(R.layout.dialog_backup_operate_tip, null);
        TextView tvTitle = view.findViewById(R.id.title);
        TextView tvContent = view.findViewById(R.id.backup_tip_content);
        tvTitle.setText(R.string.backup_share_permission_title);
        tvContent.setText(R.string.backup_share_permission_text);
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                .setView(view);
        builder.setPositiveButton(
                getString(R.string.button_permission_info),
                dialogClickListener
        );
        AlertDialog dialog = builder.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public void setPositionClicked(int positionClicked) {
        if (adapter != null) {
            adapter.setPositionClicked(positionClicked);
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if (intent == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                showSnackbar(getString(R.string.error_server_connection_problem));
                return;
            }

            final ArrayList<String> emails = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);
            final long nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1);

            if (nodeHandle != -1) {
                node = megaApi.getNodeByHandle(nodeHandle);
            }

            if (fileBackupManager.shareFolder(nodeController, new long[]{nodeHandle}, emails, ACCESS_READ)) {
                return;
            }

            if (node != null) {
                if (node.isFolder()) {
                    dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
                    final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                    dialogBuilder.setSingleChoiceItems(items, -1, (dialog, item) -> {
                        permissionsDialog.dismiss();

                        statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_sharing_folder));
                        statusDialog.show();
                        FileContactListActivityExtensionKt.shareFolder(this, node, emails, item);
                    });
                    permissionsDialog = dialogBuilder.create();
                    permissionsDialog.show();
                }
            }
        }
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
        Timber.d("onUserupdate");

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        Timber.d("onUserAlertsUpdate");
    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    @Override
    public void onSetsUpdate(MegaApiJava api, ArrayList<MegaSet> sets) {

    }

    @Override
    public void onSetElementsUpdate(MegaApiJava api, ArrayList<MegaSetElement> elements) {

    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
        Timber.d("onNodesUpdate");

        try {
            statusDialog.dismiss();
        } catch (Exception ex) {
            Timber.e(ex, "Error dismiss status dialog");
        }

        if (node.isFolder()) {
            viewModel.getMegaShares(node);
        }
    }

    private void updateListView() {
        listContacts.clear();
        if (tempListContacts != null && !tempListContacts.isEmpty()) {
            listContacts.addAll(tempListContacts);
        }
        if (listContacts.size() > 0) {
            listView.setVisibility(View.VISIBLE);
            emptyImage.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);

            if (adapter != null) {
                adapter.setNode(node);
                adapter.setContext(this);
                adapter.setShareList(listContacts);
                adapter.setListFragment(listView);
            } else {
                adapter = new MegaSharedFolderAdapter(this, node, listContacts, listView);
                adapter.setMultipleSelect(false);
            }
            listView.setAdapter(adapter);
        } else {
            listView.setVisibility(View.GONE);
            emptyImage.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.VISIBLE);
        }
        listView.invalidate();
    }

    public void showConfirmationRemoveContactFromShare(final String email) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        String message = getResources().getString(R.string.remove_contact_shared_folder, email);
        builder.setMessage(message)
                .setPositiveButton(R.string.general_remove, (dialog, which) -> {
                    statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_removing_contact_folder));
                    nodeController.removeShare(new ShareListener(this, REMOVE_SHARE_LISTENER, 1), node, email);
                })
                .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, (dialog, which) -> {
                })
                .show();
    }

    public void showConfirmationRemoveMultipleContactFromShare(final ArrayList<MegaShare> contacts) {
        Timber.d("showConfirmationRemoveMultipleContactFromShare");

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    removeMultipleShares(contacts);
                    break;
                }
                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        String message = getResources().getQuantityString(R.plurals.remove_multiple_contacts_shared_folder, contacts.size(), contacts.size());
        builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, dialogClickListener).show();
    }

    public void removeMultipleShares(ArrayList<MegaShare> shares) {
        Timber.d("Number of shared to remove: %s", shares.size());

        statusDialog = createProgressDialog(fileContactListActivity, getString(R.string.context_removing_contact_folder));
        nodeController.removeShares(shares, node);
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {
    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {
    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
    }

    public void showSnackbar(String s) {
        showSnackbar(container, s);
    }

    public MegaUser getSelectedContact() {
        String email = selectedShare.getUser();
        return megaApi.getContact(email);
    }

    private void updateAdapter(long handleReceived) {
        if (listContacts == null || listContacts.isEmpty()) return;

        for (int i = 0; i < listContacts.size(); i++) {
            String email = listContacts.get(i).getUser();
            MegaUser contact = megaApi.getContact(email);
            long handleUser = contact.getHandle();
            if (handleUser == handleReceived) {
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    public static Intent launchIntent(Context context, Long handle){
        Intent intent = new Intent(context, FileContactListActivity.class);
        intent.putExtra(NAME, handle);
        return intent;
    }
}

