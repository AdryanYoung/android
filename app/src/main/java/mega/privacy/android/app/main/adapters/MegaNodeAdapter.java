package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER;
import static mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER;
import static mega.privacy.android.app.utils.Constants.CONTACT_SHARED_FOLDER_ADAPTER;
import static mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER;
import static mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER;
import static mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.LINKS_ADAPTER;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_CONTACT_NAME_LAND;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_CONTACT_NAME_PORT;
import static mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER;
import static mega.privacy.android.app.utils.ContactUtil.getContactNameDB;
import static mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.isVideoFile;
import static mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo;
import static mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderLinkInfo;
import static mega.privacy.android.app.utils.MegaNodeUtil.getFolderIcon;
import static mega.privacy.android.app.utils.MegaNodeUtil.getNumberOfFolders;
import static mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownDialog;
import static mega.privacy.android.app.utils.OfflineUtils.availableOffline;
import static mega.privacy.android.app.utils.TextUtil.getFileInfo;
import static mega.privacy.android.app.utils.TimeUtils.formatLongDateTime;
import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.isOffline;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import coil.Coil;
import coil.request.ImageRequest;
import coil.request.SuccessResult;
import coil.transform.RoundedCornersTransformation;
import coil.util.CoilUtils;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.dragger.DragThumbnailGetter;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.databinding.SortByHeaderBinding;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel;
import mega.privacy.android.app.main.ContactFileListActivity;
import mega.privacy.android.app.main.ContactFileListFragment;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.contactSharedFolder.ContactSharedFolderFragment;
import mega.privacy.android.app.presentation.backups.BackupsFragment;
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.NodeTakenDownDialogListener;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.domain.entity.Contact;
import mega.privacy.android.domain.entity.ShareData;
import mega.privacy.android.domain.entity.SortOrder;
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

public class MegaNodeAdapter extends RecyclerView.Adapter<MegaNodeAdapter.ViewHolderBrowser> implements OnClickListener, View.OnLongClickListener, SectionTitleProvider, RotatableAdapter, NodeTakenDownDialogListener, DragThumbnailGetter {

    public static final int ITEM_VIEW_TYPE_LIST = 0;
    public static final int ITEM_VIEW_TYPE_GRID = 1;
    public static final int ITEM_VIEW_TYPE_HEADER = 2;

    private Context context;
    private MegaApiAndroid megaApi;

    private List<MegaNode> nodes;

    /**
     * List of shareData associated to the List of MegaNode
     * This list is used to carry additional information for incoming shares and outgoing shares
     * Each ShareData element at a specific position is associated to the MegaNode element
     * at the same position of the nodes attributes
     * The element is null if the node associated is already verified
     */
    private List<ShareData> shareData;

    private Object fragment;
    private long parentHandle = -1;
    private DisplayMetrics outMetrics;

    private int placeholderCount;

    @NotNull
    private SparseBooleanArray selectedItems = new SparseBooleanArray();

    /**
     * the flag to store the node position where still remained unhandled
     */
    private int unHandledItem = -1;

    /**
     * the dialog to show taken down message
     */
    private AlertDialog takenDownDialog;

    private RecyclerView listFragment;
    private DatabaseHandler dbH = null;
    private boolean multipleSelect;
    private int type = FILE_BROWSER_ADAPTER;
    private int adapterType;

    private boolean isContactVerificationOn;

    private SortByHeaderViewModel sortByViewModel;

    public static class ViewHolderBrowser extends RecyclerView.ViewHolder {

        private ViewHolderBrowser(View v) {
            super(v);
        }

        public ImageView savedOffline;
        public ImageView publicLinkImage;
        public ImageView takenDownImage;
        public TextView textViewFileName;
        public ImageView imageFavourite;
        public ImageView imageLabel;
        public EmojiTextView textViewFileSize;
        public long document;
        public RelativeLayout itemLayout;
    }

    public static class ViewHolderBrowserList extends MegaNodeAdapter.ViewHolderBrowser {

        public ViewHolderBrowserList(View v) {
            super(v);
        }

        public ImageView imageView;
        public ImageView permissionsIcon;
        public ImageView versionsIcon;
        public RelativeLayout threeDotsLayout;
    }

    public static class ViewHolderBrowserGrid extends MegaNodeAdapter.ViewHolderBrowser {

        public ViewHolderBrowserGrid(View v) {
            super(v);
        }

        public ImageView imageViewThumb;
        public ImageView imageViewIcon;
        public ConstraintLayout thumbLayout;
        public ImageView imageViewVideoIcon;
        public TextView videoDuration;
        public RelativeLayout videoInfoLayout;
        public ConstraintLayout bottomContainer;
        public ImageButton imageButtonThreeDots;

        public View folderLayout;
        public View fileLayout;
        public RelativeLayout thumbLayoutForFile;
        public ImageView fileGridIconForFile;
        public ImageButton imageButtonThreeDotsForFile;
        public TextView textViewFileNameForFile;
        public ImageView takenDownImageForFile;
        public ImageView fileGridSelected;
        public ImageView folderGridSelected;
    }

    public class ViewHolderSortBy extends ViewHolderBrowser {

        private final SortByHeaderBinding binding;

        private ViewHolderSortBy(SortByHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind() {
            binding.setSortByHeaderViewModel(sortByViewModel);

            SortOrder orderType = sortByViewModel.getOrder().getCloudSortOrder();

            // Root of incoming shares tab, display sort options OTHERS
            if (type == INCOMING_SHARES_ADAPTER
                    && ((ManagerActivity) context).getDeepBrowserTreeIncoming() == 0) {
                orderType = sortByViewModel.getOrder().getOthersSortOrder();
            }
            // Root of outgoing shares tab, display sort options OTHERS
            else if (type == OUTGOING_SHARES_ADAPTER
                    && ((ManagerActivity) context).getDeepBrowserTreeOutgoing() == 0) {
                orderType = sortByViewModel.getOrder().getOthersSortOrder();
            }

            binding.setOrderNameStringId(SortByHeaderViewModel.getOrderNameMap().get(orderType));
            binding.setIsFromFolderLink(type == FOLDER_LINK_ADAPTER);

            if (type == FOLDER_LINK_ADAPTER) {
                binding.sortByLayout.setVisibility(View.GONE);
            } else {
                binding.sortByLayout.setVisibility(View.VISIBLE);
            }

            binding.listModeSwitch.setVisibility(type == LINKS_ADAPTER
                    ? View.GONE
                    : View.VISIBLE);
        }
    }

    @Override
    public int getNodePosition(long handle) {
        for (int i = 0; i < nodes.size(); i++) {
            MegaNode node = nodes.get(i);
            if (node != null && node.getHandle() == handle) {
                return i;
            }
        }

        return INVALID_POSITION;
    }

    @Nullable
    @Override
    public View getThumbnail(@NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ViewHolderBrowserList) {
            return ((ViewHolderBrowserList) viewHolder).imageView;
        } else if (viewHolder instanceof ViewHolderBrowserGrid) {
            return ((ViewHolderBrowserGrid) viewHolder).imageViewThumb;
        }

        return null;
    }

    @Override
    public int getPlaceholderCount() {
        return placeholderCount;
    }

    @Override
    public int getUnhandledItem() {
        return unHandledItem;
    }

    public void toggleAllSelection(int pos) {
        Timber.d("Position: %s", pos);
        startAnimation(pos, putOrDeletePostion(pos));
    }

    public void toggleSelection(int pos) {
        Timber.d("Position: %s", pos);
        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }

        hideMultipleSelect();
        notifyItemChanged(pos);
    }

    boolean putOrDeletePostion(int pos) {
        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
            return true;
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
            return false;
        }
    }

    void startAnimation(final int pos, final boolean delete) {

        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            Timber.d("Adapter type is LIST");
            ViewHolderBrowserList view = (ViewHolderBrowserList) listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                Timber.d("Start animation: %s", pos);
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (!delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        hideMultipleSelect();
                        if (delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.imageView.startAnimation(flipAnimation);
            } else {
                Timber.d("View is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        } else {
            Timber.d("Adapter type is GRID");
            MegaNode node = getItem(pos);
            boolean isFile = false;
            if (node != null) {
                if (node.isFolder()) {
                    isFile = false;
                } else {
                    isFile = true;
                }
            }
            ViewHolderBrowserGrid view = (ViewHolderBrowserGrid) listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                Timber.d("Start animation: %s", pos);
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                if (!delete && isFile) {
                    notifyItemChanged(pos);
                    flipAnimation.setDuration(250);
                }
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (!delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        hideMultipleSelect();
                        notifyItemChanged(pos);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                if (isFile) {
                    view.fileGridSelected.startAnimation(flipAnimation);
                } else {
                    view.imageViewIcon.startAnimation(flipAnimation);
                }
            } else {
                Timber.d("View is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        }
    }

    void hideMultipleSelect() {
        if (selectedItems.size() <= 0) {
            if (type == BACKUPS_ADAPTER) {
                ((BackupsFragment) fragment).hideMultipleSelect();
            } else if (type == CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragment) fragment).hideMultipleSelect();
            } else if (type == CONTACT_SHARED_FOLDER_ADAPTER) {
                ((ContactSharedFolderFragment) fragment).hideMultipleSelect();
            }
        }
    }

    public void selectAll() {
        for (int i = 0; i < nodes.size(); i++) {
            selectedItems.put(i, true);
            notifyItemChanged(i);
        }
    }

    public void clearSelections() {
        Timber.d("clearSelections");
        if (nodes == null) {
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            selectedItems.delete(i);
            notifyItemChanged(i);
        }
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    @Override
    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    /*
     * Get list of all selected nodes
     */
    public List<MegaNode> getSelectedNodes() {
        ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {
                MegaNode document = getNodeAt(selectedItems.keyAt(i));
                if (document != null) {
                    nodes.add(document);
                }
            }
        }
        return nodes;
    }

    public ArrayList<MegaNode> getArrayListSelectedNodes() {
        ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {
                MegaNode document = getNodeAt(selectedItems.keyAt(i));
                if (document != null) {
                    nodes.add(document);
                }
            }
        }
        return nodes;
    }

    /*
     * The method to return how many folders in this adapter
     */
    @Override
    public int getFolderCount() {
        return getNumberOfFolders(nodes);
    }

    /**
     * In grid view.
     * For folder count is odd. Insert null element as placeholder.
     *
     * @param nodes Origin nodes to show.
     * @return Nodes list with placeholder.
     */
    private List<MegaNode> insertPlaceHolderNode(List<MegaNode> nodes) {
        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            if (shouldShowSortByHeader(nodes)) {
                placeholderCount = 1;
                nodes.add(0, null);
            } else {
                placeholderCount = 0;
            }

            return nodes;
        }

        int folderCount = getNumberOfFolders(nodes);
        int spanCount = 2;

        if (listFragment instanceof NewGridRecyclerView) {
            spanCount = ((NewGridRecyclerView) listFragment).getSpanCount();
        }

        placeholderCount = (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);

        if (folderCount > 0 && placeholderCount != 0 && adapterType == ITEM_VIEW_TYPE_GRID) {
            //Add placeholder at folders' end.
            for (int i = 0; i < placeholderCount; i++) {
                try {
                    nodes.add(folderCount + i, null);
                } catch (IndexOutOfBoundsException e) {
                    Timber.e(e, "Inserting placeholders [nodes.size]: %d [folderCount+i]: %d", nodes.size(), folderCount + i);
                }
            }
        }

        if (shouldShowSortByHeader(nodes)) {
            placeholderCount++;
            nodes.add(0, null);
        }

        return nodes;
    }

    /**
     * Checks if should show sort by header.
     * It should show the header if the list of nodes is not empty and if the adapter is not:
     * CONTACT_SHARED_FOLDER_ADAPTER or CONTACT_FILE_ADAPTER.
     *
     * @param nodes List of nodes to check if is empty or not.
     * @return True if should show the sort by header, false otherwise.
     */
    private boolean shouldShowSortByHeader(List<MegaNode> nodes) {
        return !nodes.isEmpty() && type != CONTACT_SHARED_FOLDER_ADAPTER && type != CONTACT_FILE_ADAPTER;
    }

    @NotNull
    public final GridLayoutManager.SpanSizeLookup getSpanSizeLookup(final int spanCount) {
        return (GridLayoutManager.SpanSizeLookup) (new GridLayoutManager.SpanSizeLookup() {
            public int getSpanSize(int position) {
                return getItemViewType(position) == ITEM_VIEW_TYPE_HEADER ? spanCount : 1;
            }
        });
    }

    public MegaNodeAdapter(Context context, Object fragment, List<MegaNode> nodes,
                           long parentHandle, RecyclerView recyclerView, int type, int adapterType) {
        initAdapter(context, fragment, nodes, parentHandle, recyclerView, type, adapterType);
    }

    public MegaNodeAdapter(Context context, Object fragment, List<MegaNode> nodes,
                           long parentHandle, RecyclerView recyclerView, int type, int adapterType,
                           SortByHeaderViewModel sortByHeaderViewModel) {
        initAdapter(context, fragment, nodes, parentHandle, recyclerView, type, adapterType);
        this.sortByViewModel = sortByHeaderViewModel;
    }

    /**
     * Initializes the principal properties of the adapter.
     *
     * @param context      Current Context.
     * @param fragment     Current Fragment.
     * @param nodes        List of nodes.
     * @param parentHandle Current parent handle.
     * @param recyclerView View in which the adapter will be set.
     * @param type         Fragment adapter type.
     * @param adapterType  List or grid adapter type.
     */
    private void initAdapter(Context context, Object fragment, List<MegaNode> nodes,
                             long parentHandle, RecyclerView recyclerView, int type, int adapterType) {

        this.context = context;
        this.nodes = nodes;
        this.parentHandle = parentHandle;
        this.type = type;
        this.adapterType = adapterType;
        this.fragment = fragment;

        dbH = DbHandlerModuleKt.getDbHandler();

        switch (type) {
            case CONTACT_FILE_ADAPTER: {
                ((ContactFileListActivity) context).setParentHandle(parentHandle);
                break;
            }
            case FOLDER_LINK_ADAPTER: {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApiFolder();
                break;
            }
            case BACKUPS_ADAPTER: {
                Timber.d("onCreate BACKUPS_ADAPTER");
                ((ManagerActivity) context).setParentHandleBackups(parentHandle);
                break;
            }
            default: {
                break;
            }
        }

        this.listFragment = recyclerView;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication())
                    .getMegaApi();
        }

    }

    public void setNodes(List<MegaNode> nodes) {
        this.nodes = insertPlaceHolderNode(nodes);
        Timber.d("setNodes size: %s", this.nodes.size());
        notifyDataSetChanged();
    }

    /**
     * Set the nodes list and shareData list
     * This function is used to populate the list of incoming and outgoing shares
     *
     * @param nodes     the list of nodes, whether verified or unverified
     * @param shareData the list of shares data associated to the node
     */
    public void setNodesWithShareData(List<MegaNode> nodes, List<ShareData> shareData) {
        this.nodes = insertPlaceHolderNode(nodes);
        // need to add extra elements to sharedata too, so that the element at a specific position
        // corresponds exactly to the node in the nodes list
        for (int i = 0; i < this.nodes.size(); i++) {
            if (this.nodes.get(i) == null)
                shareData.add(i, null);
        }
        this.shareData = shareData;
        Timber.d("setNodes size: %s", this.nodes.size());
        notifyDataSetChanged();
    }

    /**
     * Method to update an item when some contact information has changed.
     *
     * @param contactHandle Contact ID.
     */
    public void updateItem(long contactHandle) {
        for (MegaNode node : nodes) {
            if (node == null || !node.isFolder()
                    || (type != INCOMING_SHARES_ADAPTER && type != OUTGOING_SHARES_ADAPTER))
                continue;

            ArrayList<MegaShare> shares = type == INCOMING_SHARES_ADAPTER
                    ? megaApi.getInSharesList()
                    : megaApi.getOutShares(node);

            if (shares != null && !shares.isEmpty()) {
                for (MegaShare share : shares) {
                    MegaUser user = megaApi.getContact(share.getUser());

                    if (user != null && user.getHandle() == contactHandle) {
                        notifyItemChanged(nodes.indexOf(node));
                    }
                }
            }
        }
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    public int getAdapterType() {
        return adapterType;
    }

    @Override
    public ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (viewType == ITEM_VIEW_TYPE_LIST) {
            Timber.d("type: ITEM_VIEW_TYPE_LIST");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
            ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
            holderList.itemLayout = v.findViewById(R.id.file_list_item_layout);
            holderList.imageView = v.findViewById(R.id.file_list_thumbnail);
            holderList.savedOffline = v.findViewById(R.id.file_list_saved_offline);

            holderList.publicLinkImage = v.findViewById(R.id.file_list_public_link);
            holderList.takenDownImage = v.findViewById(R.id.file_list_taken_down);
            holderList.permissionsIcon = v.findViewById(R.id.file_list_incoming_permissions);

            holderList.versionsIcon = v.findViewById(R.id.file_list_versions_icon);

            holderList.textViewFileName = v.findViewById(R.id.file_list_filename);

            holderList.imageLabel = v.findViewById(R.id.img_label);
            holderList.imageFavourite = v.findViewById(R.id.img_favourite);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holderList.textViewFileName.setMaxWidth(scaleWidthPx(275, outMetrics));
            } else {
                holderList.textViewFileName.setMaxWidth(scaleWidthPx(190, outMetrics));
            }

            holderList.textViewFileSize = v.findViewById(R.id.file_list_filesize);
            if (isScreenInPortrait(context)) {
                holderList.textViewFileSize.setMaxWidthEmojis(dp2px(MAX_WIDTH_CONTACT_NAME_PORT, outMetrics));
            } else {
                holderList.textViewFileSize.setMaxWidthEmojis(dp2px(MAX_WIDTH_CONTACT_NAME_LAND, outMetrics));
            }

            holderList.threeDotsLayout = v.findViewById(R.id.file_list_three_dots_layout);

            holderList.savedOffline.setVisibility(View.INVISIBLE);

            holderList.publicLinkImage.setVisibility(View.INVISIBLE);

            holderList.takenDownImage.setVisibility(View.GONE);

            holderList.textViewFileSize.setVisibility(View.VISIBLE);

            holderList.itemLayout.setTag(holderList);
            holderList.itemLayout.setOnClickListener(this);
            holderList.itemLayout.setOnLongClickListener(this);

            holderList.threeDotsLayout.setTag(holderList);
            holderList.threeDotsLayout.setOnClickListener(this);

            v.setTag(holderList);
            return holderList;
        } else if (viewType == ITEM_VIEW_TYPE_GRID) {
            Timber.d("type: ITEM_VIEW_TYPE_GRID");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_grid, parent, false);
            ViewHolderBrowserGrid holderGrid = new ViewHolderBrowserGrid(v);

            holderGrid.folderLayout = v.findViewById(R.id.item_file_grid_folder);
            holderGrid.fileLayout = v.findViewById(R.id.item_file_grid_file);
            holderGrid.itemLayout = v.findViewById(R.id.file_grid_item_layout);
            holderGrid.imageViewThumb = v.findViewById(R.id.file_grid_thumbnail);
            holderGrid.imageViewIcon = v.findViewById(R.id.file_grid_icon);
            holderGrid.fileGridIconForFile = v.findViewById(R.id.file_grid_icon_for_file);
            holderGrid.thumbLayout = v.findViewById(R.id.file_grid_thumbnail_layout);
            holderGrid.thumbLayoutForFile = v.findViewById(R.id.file_grid_thumbnail_layout_for_file);
            holderGrid.textViewFileName = v.findViewById(R.id.file_grid_filename);
            holderGrid.textViewFileNameForFile = v.findViewById(R.id.file_grid_filename_for_file);
            holderGrid.imageButtonThreeDotsForFile = v.findViewById(R.id.file_grid_three_dots_for_file);
            holderGrid.imageButtonThreeDots = v.findViewById(R.id.file_grid_three_dots);
            holderGrid.takenDownImage = v.findViewById(R.id.file_grid_taken_down);
            holderGrid.takenDownImageForFile = v.findViewById(R.id.file_grid_taken_down_for_file);
            holderGrid.imageViewVideoIcon = v.findViewById(R.id.file_grid_video_icon);
            holderGrid.videoDuration = v.findViewById(R.id.file_grid_title_video_duration);
            holderGrid.videoInfoLayout = v.findViewById(R.id.item_file_videoinfo_layout);
            holderGrid.fileGridSelected = v.findViewById(R.id.file_grid_check_icon);
            holderGrid.folderGridSelected = v.findViewById(R.id.folder_grid_check_icon);
            holderGrid.bottomContainer = v.findViewById(R.id.grid_bottom_container);
            holderGrid.bottomContainer.setTag(holderGrid);
            holderGrid.bottomContainer.setOnClickListener(this);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holderGrid.textViewFileNameForFile.setMaxWidth(scaleWidthPx(70, outMetrics));
            } else {
                holderGrid.textViewFileNameForFile.setMaxWidth(scaleWidthPx(140, outMetrics));
            }

            holderGrid.takenDownImage.setVisibility(View.GONE);
            holderGrid.takenDownImageForFile.setVisibility(View.GONE);

            holderGrid.itemLayout.setTag(holderGrid);
            holderGrid.itemLayout.setOnClickListener(this);
            holderGrid.itemLayout.setOnLongClickListener(this);

            holderGrid.imageButtonThreeDots.setTag(holderGrid);
            holderGrid.imageButtonThreeDots.setOnClickListener(this);
            holderGrid.imageButtonThreeDotsForFile.setTag(holderGrid);
            holderGrid.imageButtonThreeDotsForFile.setOnClickListener(this);
            v.setTag(holderGrid);

            return holderGrid;
        } else {
            SortByHeaderBinding binding = SortByHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolderSortBy(binding);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolderBrowser holder, int position) {
        Timber.d("Position: %s", position);

        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_HEADER:
                ((ViewHolderSortBy) holder).bind();
                break;

            case ITEM_VIEW_TYPE_LIST:
                ViewHolderBrowserList holderList = (ViewHolderBrowserList) holder;
                onBindViewHolderList(holderList, position);
                break;

            case ITEM_VIEW_TYPE_GRID:
                ViewHolderBrowserGrid holderGrid = (ViewHolderBrowserGrid) holder;
                onBindViewHolderGrid(holderGrid, position);
                break;
        }

        reSelectUnhandledNode();
    }

    public void onBindViewHolderGrid(ViewHolderBrowserGrid holder, int position) {
        Timber.d("Position: %s", position);
        MegaNode node = getItem(position);
        //Placeholder for folder when folder count is odd.
        if (node == null) {
            holder.folderLayout.setVisibility(View.GONE);
            holder.fileLayout.setVisibility(View.GONE);
            holder.itemLayout.setVisibility(View.GONE);
            return;
        }

        holder.document = node.getHandle();
        Timber.d("Node : %d %d", position, node.getHandle());

        holder.textViewFileName.setText(node.getName());
        holder.videoInfoLayout.setVisibility(View.GONE);

        CoilUtils.dispose(holder.imageViewThumb);
        if (node.isTakenDown()) {
            holder.textViewFileNameForFile.setTextColor(ContextCompat.getColor(context, R.color.red_800_red_400));
            holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.red_800_red_400));
            holder.takenDownImage.setVisibility(View.VISIBLE);
            holder.takenDownImageForFile.setVisibility(View.VISIBLE);
        } else {
            holder.textViewFileNameForFile.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorPrimary));
            holder.textViewFileName.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorPrimary));
            holder.takenDownImage.setVisibility(View.GONE);
            holder.takenDownImageForFile.setVisibility(View.GONE);
        }

        if (node.isFolder()) {
            holder.itemLayout.setVisibility(View.VISIBLE);
            holder.folderLayout.setVisibility(View.VISIBLE);
            holder.fileLayout.setVisibility(View.GONE);

            setFolderGridSelected(holder, position);

            holder.imageViewIcon.setVisibility(View.VISIBLE);
            holder.imageViewIcon.setImageResource(getFolderIcon(node, type == OUTGOING_SHARES_ADAPTER ? DrawerItem.SHARED_ITEMS : DrawerItem.CLOUD_DRIVE));
            holder.imageViewThumb.setVisibility(View.GONE);
            holder.thumbLayout.setBackgroundColor(Color.TRANSPARENT);

        } else if (node.isFile()) {
            holder.itemLayout.setVisibility(View.VISIBLE);
            holder.folderLayout.setVisibility(View.GONE);
            holder.imageViewThumb.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            holder.imageViewThumb.setVisibility(View.GONE);
            holder.fileLayout.setVisibility(View.VISIBLE);
            holder.textViewFileName.setVisibility(View.VISIBLE);

            holder.textViewFileNameForFile.setText(node.getName());
            holder.fileGridIconForFile.setVisibility(View.VISIBLE);
            holder.fileGridIconForFile.setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());

            if (isVideoFile(node.getName())) {
                holder.videoInfoLayout.setVisibility(View.VISIBLE);
                holder.videoDuration.setVisibility(View.GONE);

                String duration = getVideoDuration(node.getDuration());
                if (!duration.isEmpty()) {
                    holder.videoDuration.setText(duration);
                    holder.videoDuration.setVisibility(node.getDuration() <= 0 ? View.GONE : View.VISIBLE);
                }
            }

            if (node.hasThumbnail()) {
                holder.imageViewThumb.setVisibility(View.VISIBLE);
                Coil.imageLoader(context).enqueue(
                        new ImageRequest.Builder(context)
                                .data(ThumbnailRequest.fromHandle(node.getHandle()))
                                .target(holder.imageViewThumb)
                                .crossfade(true)
                                .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                                .listener(new ImageRequest.Listener() {
                                    @Override
                                    public void onSuccess(@NonNull ImageRequest request, @NonNull SuccessResult result) {
                                        holder.fileGridIconForFile.setVisibility(View.GONE);
                                    }
                                })
                                .build()
                );
            }

            if (isMultipleSelect()) {
                holder.imageButtonThreeDotsForFile.setVisibility(View.GONE);
                holder.fileGridSelected.setVisibility(isItemChecked(position) ? View.VISIBLE : View.INVISIBLE);
            } else {
                holder.fileGridSelected.setVisibility(View.GONE);
                holder.imageButtonThreeDotsForFile.setVisibility(View.VISIBLE);
            }

            holder.itemLayout.setBackground(ContextCompat.getDrawable(context,
                    isMultipleSelect() && isItemChecked(position) ? R.drawable.background_item_grid_selected : R.drawable.background_item_grid));
        }
    }

    private void setFolderGridSelected(ViewHolderBrowserGrid holder, int position) {
        if (isMultipleSelect()) {
            holder.imageButtonThreeDots.setVisibility(View.GONE);
            holder.folderGridSelected.setVisibility(isItemChecked(position) ? View.VISIBLE : View.INVISIBLE);
        } else {
            holder.folderGridSelected.setVisibility(View.GONE);
            holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
        }

        holder.itemLayout.setBackground(ContextCompat.getDrawable(context,
                isMultipleSelect() && isItemChecked(position) ? R.drawable.background_item_grid_selected : R.drawable.background_item_grid));
    }

    private void setFolderListSelected(ViewHolderBrowserList holder, int position, int folderDrawableResId) {
        if (isMultipleSelect() && isItemChecked(position)) {
            RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
            paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            paramsMultiselect.setMargins(0, 0, 0, 0);
            holder.imageView.setLayoutParams(paramsMultiselect);
            holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder);
        } else {
            holder.itemLayout.setBackground(null);
            holder.imageView.setImageResource(folderDrawableResId);
        }
    }

    public void onBindViewHolderList(ViewHolderBrowserList holder, int position) {
        Timber.d("Position: %s", position);

        holder.textViewFileSize.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        MegaNode node = getItem(position);
        if (node == null) {
            return;
        }
        holder.document = node.getHandle();

        holder.textViewFileName.setText(node.getName());
        holder.textViewFileSize.setText("");

        holder.imageFavourite.setVisibility(type != INCOMING_SHARES_ADAPTER
                && type != FOLDER_LINK_ADAPTER
                && node.isFavourite() ? View.VISIBLE : View.GONE);

        if (type != FOLDER_LINK_ADAPTER && node.getLabel() != MegaNode.NODE_LBL_UNKNOWN) {
            Drawable drawable = MegaNodeUtil.getNodeLabelDrawable(node.getLabel(), holder.itemView.getResources());
            holder.imageLabel.setImageDrawable(drawable);
            holder.imageLabel.setVisibility(View.VISIBLE);
        } else {
            holder.imageLabel.setVisibility(View.GONE);
        }

        holder.publicLinkImage.setVisibility(View.INVISIBLE);
        holder.permissionsIcon.setVisibility(View.GONE);

        if (node.isExported() && type != LINKS_ADAPTER) {
            //Node has public link
            holder.publicLinkImage.setVisibility(View.VISIBLE);
            if (node.isExpired()) {
                Timber.w("Node exported but expired!!");
            }
        } else {
            holder.publicLinkImage.setVisibility(View.INVISIBLE);
        }

        if (node.isTakenDown()) {
            holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.red_800_red_400));
            holder.takenDownImage.setVisibility(View.VISIBLE);
        } else {
            holder.textViewFileName.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorPrimary));
            holder.takenDownImage.setVisibility(View.GONE);
        }

        holder.imageView.setVisibility(View.VISIBLE);

        CoilUtils.dispose(holder.imageView);
        if (node.isFolder()) {

            Timber.d("Node is folder");
            holder.itemLayout.setBackground(null);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            params.setMargins(0, 0, 0, 0);
            holder.imageView.setLayoutParams(params);

            holder.textViewFileSize.setVisibility(View.VISIBLE);
            holder.textViewFileSize.setText(type == FOLDER_LINK_ADAPTER
                    ? getMegaNodeFolderLinkInfo(node, context)
                    : getMegaNodeFolderInfo(node, context));
            holder.versionsIcon.setVisibility(View.GONE);

            setFolderListSelected(holder, position, getFolderIcon(node, type == OUTGOING_SHARES_ADAPTER ? DrawerItem.SHARED_ITEMS : DrawerItem.CLOUD_DRIVE));
            if (isMultipleSelect()) {
                holder.threeDotsLayout.setVisibility(View.INVISIBLE);
            } else {
                holder.threeDotsLayout.setVisibility(View.VISIBLE);
            }
            if (type == CONTACT_FILE_ADAPTER || type == CONTACT_SHARED_FOLDER_ADAPTER) {
                boolean firstLevel;
                if (type == CONTACT_FILE_ADAPTER) {
                    firstLevel = ((ContactFileListFragment) fragment).isEmptyParentHandleStack();
                } else {
                    firstLevel = true;
                }

                if (firstLevel) {
                    int accessLevel = megaApi.getAccess(node);

                    if (accessLevel == MegaShare.ACCESS_FULL) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                    } else if (accessLevel == MegaShare.ACCESS_READWRITE) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
                    } else {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read);
                    }
                    holder.permissionsIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.permissionsIcon.setVisibility(View.GONE);
                }
            } else if (type == INCOMING_SHARES_ADAPTER) {
                holder.publicLinkImage.setVisibility(View.INVISIBLE);

                if (node.isTakenDown()) {
                    holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.red_800_red_400));
                    holder.takenDownImage.setVisibility(View.VISIBLE);
                } else {
                    holder.textViewFileName.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorPrimary));
                    holder.takenDownImage.setVisibility(View.GONE);
                }

                //Show the owner of the shared folder
                ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                for (int j = 0; j < sharesIncoming.size(); j++) {
                    MegaShare mS = sharesIncoming.get(j);
                    if (mS.getNodeHandle() == node.getHandle()) {
                        MegaUser user = megaApi.getContact(mS.getUser());
                        boolean isContactVerifiedByMega = megaApi.areCredentialsVerified(user);
                        if (user != null) {
                            holder.textViewFileSize.setText(getMegaUserNameDB(user));
                        } else {
                            holder.textViewFileSize.setText(mS.getUser());
                        }
                        if (isContactVerificationOn && isContactVerifiedByMega) {
                            holder.textViewFileSize.setCompoundDrawablesWithIntrinsicBounds(0, 0, mega.privacy.android.icon.pack.R.drawable.ic_contact_verified, 0);
                        } else {
                            holder.textViewFileSize.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        }
                    }
                }
                if (((ManagerActivity) context).getDeepBrowserTreeIncoming() == 0) {
                    int accessLevel = megaApi.getAccess(node);

                    if (accessLevel == MegaShare.ACCESS_FULL) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                    } else if (accessLevel == MegaShare.ACCESS_READWRITE) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
                    } else {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read);
                    }
                    boolean hasUnverifiedNodes = shareData != null && shareData.get(position) != null;
                    if (hasUnverifiedNodes) {
                        showUnverifiedNodeUi(holder, true, node, null);
                    }
                    holder.permissionsIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.permissionsIcon.setVisibility(View.GONE);
                }

            } else if (type == OUTGOING_SHARES_ADAPTER) {
                //Show the number of contacts who shared the folder if more than one contact and name of contact if that is not the case
                holder.textViewFileSize.setText(getOutgoingSubtitle(holder.textViewFileSize.getText().toString(), node));
                boolean hasUnverifiedNodes = shareData != null && shareData.get(position) != null;
                if (hasUnverifiedNodes) {
                    showUnverifiedNodeUi(holder, false, node, shareData.get(position));
                }
            }
        } else {
            Timber.d("Node is file");
            boolean isLinksRoot = type == LINKS_ADAPTER && ((ManagerActivity) context).getHandleFromLinksViewModel() == -1L;
            holder.textViewFileSize.setText(getFileInfo(getSizeString(node.getSize(), context),
                    formatLongDateTime(isLinksRoot ? node.getPublicLinkCreationTime() : node.getModificationTime())));

            if (megaApi.hasVersions(node)) {
                holder.versionsIcon.setVisibility(View.VISIBLE);
            } else {
                holder.versionsIcon.setVisibility(View.GONE);
            }

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            params.setMargins(0, 0, 0, 0);
            holder.imageView.setLayoutParams(params);

            if (!isMultipleSelect()) {
                Timber.d("Not multiselect");
                holder.itemLayout.setBackground(null);

                Timber.d("Check the thumb");

                if (node.hasThumbnail()) {
                    Timber.d("Node has thumbnail");
                    loadThumbnail(node, holder.imageView);
                } else {
                    Timber.d("Node NOT thumbnail");
                    holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                }
                holder.threeDotsLayout.setVisibility(View.VISIBLE);
            } else {
                Timber.d("Multiselection ON");
                if (this.isItemChecked(position)) {
                    holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder);
                } else {
                    holder.itemLayout.setBackground(null);
                    Timber.d("Check the thumb");

                    if (node.hasThumbnail()) {
                        Timber.d("Node has thumbnail");
                        loadThumbnail(node, holder.imageView);
                    } else {
                        Timber.d("Node NOT thumbnail");
                        holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                    }
                }
                holder.threeDotsLayout.setVisibility(View.INVISIBLE);
            }
        }

        //Check if is an offline file to show the red arrow
        if (availableOffline(context, node)) {
            holder.savedOffline.setVisibility(View.VISIBLE);
        } else {
            holder.savedOffline.setVisibility(View.INVISIBLE);
        }
    }

    private void loadThumbnail(MegaNode node, ImageView target) {
        Coil.imageLoader(context).enqueue(
                new ImageRequest.Builder(context)
                        .placeholder(MimeTypeList.typeForName(node.getName()).getIconResourceId())
                        .data(ThumbnailRequest.fromHandle(node.getHandle()))
                        .target(target)
                        .crossfade(true)
                        .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                        .listener(new ImageRequest.Listener() {
                            @Override
                            public void onSuccess(@NonNull ImageRequest request, @NonNull SuccessResult result) {
                                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) target.getLayoutParams();
                                params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
                                params.setMargins(left, 0, 0, 0);

                                target.setLayoutParams(params);
                            }
                        })
                        .build()
        );
    }

    private String getItemNode(int position) {
        if (nodes.get(position) != null) {
            return nodes.get(position).getName();
        }
        return null;
    }


    @Override
    public int getItemCount() {
        if (nodes != null) {
            return nodes.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return !nodes.isEmpty() && position == 0
                && type != CONTACT_SHARED_FOLDER_ADAPTER
                && type != CONTACT_FILE_ADAPTER
                ? ITEM_VIEW_TYPE_HEADER
                : adapterType;
    }

    public MegaNode getItem(int position) {
        if (nodes != null) {
            return nodes.get(position);
        }

        return null;
    }

    public ShareData getShareData(int position) {
        if (shareData != null) {
            return shareData.get(position);
        }
        return null;
    }

    @Override
    public String getSectionTitle(int position, Context context) {
        if (getItemNode(position) != null && !getItemNode(position).equals("")) {
            return getItemNode(position).substring(0, 1);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");

        ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();
        int currentPosition = holder.getAdapterPosition();

        Timber.d("Current position: %s", currentPosition);

        if (currentPosition < 0) {
            Timber.e("Current position error - not valid value");
            return;
        }

        final MegaNode n = getItem(currentPosition);
        ShareData sd = null;
        if (shareData != null) {
            sd = shareData.get(currentPosition);
        }
        if (n == null) {
            return;
        }

        int id = v.getId();
        if (id == R.id.grid_bottom_container || id == R.id.file_list_three_dots_layout || id == R.id.file_grid_three_dots || id == R.id.file_grid_three_dots_for_file) {
            threeDotsClicked(currentPosition, n, sd);
        } else if (id == R.id.file_list_item_layout || id == R.id.file_grid_item_layout) {
            if (n.isTakenDown() && !isMultipleSelect()) {
                takenDownDialog = showTakenDownDialog(n.isFolder(), this, context);
                unHandledItem = currentPosition;
            } else if (n.isFile() && !isOnline(context) && getLocalFile(n) == null) {
                if (isOffline(context)) {
                }
            } else {
                fileClicked(currentPosition);
            }
        }
    }

    public void reselectUnHandledSingleItem(int currentPosition) {
        notifyItemChanged(currentPosition);
        unHandledItem = currentPosition;
    }

    private void fileClicked(int currentPosition) {
        if (type == BACKUPS_ADAPTER) {
            ((BackupsFragment) fragment).onNodeSelected(currentPosition);
        } else if (type == CONTACT_FILE_ADAPTER) {
            ((ContactFileListFragment) fragment).itemClick(currentPosition);
        } else if (type == CONTACT_SHARED_FOLDER_ADAPTER) {
            ((ContactSharedFolderFragment) fragment).itemClick(currentPosition);
        }
    }

    private void threeDotsClicked(int currentPosition, MegaNode n, ShareData sd) {
        Timber.d("onClick: file_list_three_dots: %s", currentPosition);
        if (isOffline(context)) {
            return;
        }

        if (isMultipleSelect()) {
            if (type == BACKUPS_ADAPTER) {
                ((BackupsFragment) fragment).onNodeSelected(currentPosition);
            } else if (type == CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragment) fragment).itemClick(currentPosition);
            } else if (type == CONTACT_SHARED_FOLDER_ADAPTER) {
                ((ContactSharedFolderFragment) fragment).itemClick(currentPosition);
            }
        } else {
            if (type == CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragment) fragment).showOptionsPanel(n);
            } else if (type == CONTACT_SHARED_FOLDER_ADAPTER) {
                ((ContactSharedFolderFragment) fragment).showOptionsPanel(n);
            } else {
                ((ManagerActivity) context).showNodeOptionsPanel(n, NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE, sd);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        Timber.d("OnLongCLick");

        if (isOffline(context)) {
            return true;
        }

        ViewHolderBrowser holder = (ViewHolderBrowser) view.getTag();
        int currentPosition = holder.getAdapterPosition();
        if (type == BACKUPS_ADAPTER) {
            ((BackupsFragment) fragment).activateActionMode();
            ((BackupsFragment) fragment).onNodeSelected(currentPosition);
        } else if (type == CONTACT_SHARED_FOLDER_ADAPTER) {
            ((ContactSharedFolderFragment) fragment).activateActionMode();
            ((ContactSharedFolderFragment) fragment).itemClick(currentPosition);
        } else if (type == CONTACT_FILE_ADAPTER) {
            ((ContactFileListFragment) fragment).activateActionMode();
            ((ContactFileListFragment) fragment).itemClick(currentPosition);
        }

        return true;
    }

    /*
     * Get document at specified position
     */
    private MegaNode getNodeAt(int position) {
        try {
            if (nodes != null) {
                return nodes.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    public long getParentHandle() {
        return parentHandle;
    }

    public void setParentHandle(long parentHandle) {
        this.parentHandle = parentHandle;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        Timber.d("multipleSelect: %s", multipleSelect);
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        selectedItems.clear();
    }

    public void setListFragment(RecyclerView listFragment) {
        this.listFragment = listFragment;
    }

    /**
     * Gets the subtitle of a Outgoing item.
     * If it is shared with only one contact it should return the name or email of it.
     * If it is shared with more than one contact it should return the number of contacts.
     * If it is not a root outgoing folder it should return the content of the folder.
     *
     * @param currentSubtitle the current content of the folder (number of files and folders).
     * @param node            outgoing folder.
     * @return the string to show in the subtitle of an outgoing item.
     */
    private String getOutgoingSubtitle(String currentSubtitle, MegaNode node) {
        String subtitle = currentSubtitle;

        // only count the outgoing shares that has been verified
        List<MegaShare> sl = megaApi
                .getOutShares(node)
                .stream()
                .filter(MegaShare::isVerified)
                .collect(Collectors.toList());
        if (sl.size() != 0) {
            if (sl.size() == 1 && sl.get(0).getUser() != null) {
                subtitle = sl.get(0).getUser();
                Contact contact = dbH.findContactByEmail(subtitle);
                if (contact != null) {
                    String fullName = getContactNameDB(contact);
                    if (fullName != null) {
                        subtitle = fullName;
                    }
                }
            } else {
                subtitle = context.getResources().getQuantityString(R.plurals.general_num_shared_with, sl.size(), sl.size());
            }
        }

        return subtitle;
    }

    /**
     * This is the method to click unhandled taken down dialog again,
     * after the recycler view finish binding adapter
     */
    private void reSelectUnhandledNode() {
        // if there is no un handled item
        if (unHandledItem == -1) {
            return;
        }

        listFragment.postDelayed(
                () -> {
                    if (takenDownDialog != null && takenDownDialog.isShowing()) {
                        return;
                    }

                    try {
                        listFragment.scrollToPosition(unHandledItem);
                        listFragment.findViewHolderForAdapterPosition(unHandledItem).itemView.performClick();
                    } catch (Exception ex) {
                        Timber.e(ex);
                    }
                }, 100
        );
    }

    /**
     * This is the method to clear existence dialog to prevent window leak,
     * after the rotation of the screen
     */
    public void clearTakenDownDialog() {
        if (takenDownDialog != null) {
            takenDownDialog.dismiss();
        }
    }

    @Override
    public void onDisputeClicked() {
        unHandledItem = -1;
    }

    @Override
    public void onCancelClicked() {
        unHandledItem = -1;
    }

    /**
     * Function to show Unverified node UI items accordingly
     *
     * @param holder         [ViewHolderBrowserList]
     * @param isIncomingNode boolean to indicate if the node is incoming so that
     *                       "Undecrypted folder" is displayed instead of node name
     */
    private void showUnverifiedNodeUi(ViewHolderBrowserList holder, Boolean isIncomingNode, MegaNode node, ShareData shareData) {
        if (isIncomingNode) {
            if (node.isNodeKeyDecrypted()) {
                holder.textViewFileName.setText(node.getName());
            } else {
                holder.textViewFileName.setText(context.getString(R.string.shared_items_verify_credentials_undecrypted_folder));
            }
        } else {
            MegaUser user = megaApi.getContact(shareData.getUser());
            if (user != null) {
                holder.textViewFileSize.setText(getMegaUserNameDB(user));
            } else {
                holder.textViewFileSize.setText(shareData.getUser());
            }
        }
        holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.red_600));
        holder.permissionsIcon.setVisibility(View.VISIBLE);
        holder.permissionsIcon.setImageResource(R.drawable.serious_warning);
    }

    /**
     * Sets contact verification value
     *
     * @param isContactVerificationOn boolean value of contact verification info
     */
    public void setContactVerificationOn(boolean isContactVerificationOn) {
        this.isContactVerificationOn = isContactVerificationOn;
    }
}
