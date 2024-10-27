package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.MegaNodeUtil.getFileInfo;
import static mega.privacy.android.app.utils.Util.isOnline;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.ArrayList;
import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;
import coil.request.SuccessResult;
import coil.transform.RoundedCornersTransformation;
import coil.util.CoilUtils;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.dragger.DragThumbnailGetter;
import mega.privacy.android.app.main.VersionsFileActivity;
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import timber.log.Timber;

public class VersionsFileAdapter extends RecyclerView.Adapter<VersionsFileAdapter.ViewHolderVersion> implements OnClickListener, View.OnLongClickListener, DragThumbnailGetter {

    public static final int ITEM_VIEW_TYPE_LIST = 0;
    public static final int ITEM_VIEW_TYPE_GRID = 1;

    Context context;
    MegaApiAndroid megaApi;

    ArrayList<MegaNode> nodes;

    long parentHandle = -1;
    DisplayMetrics outMetrics;

    private SparseBooleanArray selectedItems;

    RecyclerView listFragment;

    boolean multipleSelect;

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
    public View getThumbnail(@NonNull ViewHolder viewHolder) {
        return viewHolder.itemView;
    }

    /* public static view holder class */
    public static class ViewHolderVersion extends ViewHolder {

        public ViewHolderVersion(View v) {
            super(v);
        }

        public TextView textViewFileName;
        public TextView textViewFileSize;
        public long document;
        public ImageView imageView;
        public RelativeLayout itemLayout;
        public RelativeLayout threeDotsLayout;
        public RelativeLayout headerLayout;
        public TextView titleHeader;
        public TextView sizeHeader;
    }

    public void toggleAllSelection(int pos) {
        Timber.d("Position: %s", pos);
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }

        VersionsFileAdapter.ViewHolderVersion view = (VersionsFileAdapter.ViewHolderVersion) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %d multiselection state: %s", pos, isMultipleSelect());
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Timber.d("onAnimationEnd");
                    if (selectedItems.size() <= 0) {
                        Timber.d("hideMultipleSelect");
                        ((VersionsFileActivity) context).hideMultipleSelect();
                    }
                    Timber.d("notified item changed");
                    notifyItemChanged(positionToflip);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        } else {
            Timber.w("NULL view pos: %s", positionToflip);
            notifyItemChanged(pos);
        }

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
        notifyItemChanged(pos);

        VersionsFileAdapter.ViewHolderVersion view = (VersionsFileAdapter.ViewHolderVersion) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        ((VersionsFileActivity) context).hideMultipleSelect();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            view.imageView.startAnimation(flipAnimation);

        } else {
            Timber.w("View is null - not animation");
        }
    }

    /**
     * Selects all Previous Versions and de-select the Current Version
     */
    public void selectAllPreviousVersions() {
        if (isItemChecked(0)) toggleAllSelection(0);
        for (int i = 1; i < this.getItemCount(); i++) {
            if (!isItemChecked(i)) toggleAllSelection(i);
        }
    }

    /**
     * Checks whether all of the Previous Versions have been selected or not
     *
     * @return true if all Previous Versions have been selected or not, and false if otherwise. It
     * will also return false if only one Version exists (the Current Version), or the Versions list
     * is empty
     */
    public boolean areAllPreviousVersionsSelected() {
        int versionCount = getItemCount();
        if (versionCount <= 1) return false;

        for (int i = 1; i < getItemCount(); i++) {
            if (!isItemChecked(i)) return false;
        }
        return true;
    }

    public void clearSelections() {
        Timber.d("clearSelections");
        for (int i = 0; i < this.getItemCount(); i++) {
            if (isItemChecked(i)) {
                toggleAllSelection(i);
            }
        }
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    /**
     * Retrieves the selected Node Versions
     *
     * @return a Pair containing the List of selected Node Versions and whether the current Version
     * has been selected or not
     */
    @NonNull
    public Pair<List<MegaNode>, Boolean> getSelectedNodeVersions() {
        ArrayList<MegaNode> nodeVersions = new ArrayList<>();
        boolean isCurrentVersionSelected = selectedItems.get(0);

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                int position = selectedItems.keyAt(i);
                MegaNode version = getNodeAt(position);
                if (version != null) {
                    nodeVersions.add(version);
                }
            }
        }
        return new Pair<>(nodeVersions, isCurrentVersionSelected);
    }

    public VersionsFileAdapter(Context _context, ArrayList<MegaNode> _nodes, RecyclerView recyclerView) {
        this.context = _context;
        this.nodes = _nodes;

        this.listFragment = recyclerView;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication())
                    .getMegaApi();
        }
    }

    public void setNodes(ArrayList<MegaNode> nodes) {
        Timber.d("setNodes");
        this.nodes = nodes;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolderVersion onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version_file, parent, false);

        ViewHolderVersion holderList = new ViewHolderVersion(v);
        holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.version_file_item_layout);
        holderList.imageView = (ImageView) v.findViewById(R.id.version_file_thumbnail);

        holderList.textViewFileName = (TextView) v.findViewById(R.id.version_file_filename);

        holderList.textViewFileSize = (TextView) v.findViewById(R.id.version_file_filesize);

        holderList.threeDotsLayout = (RelativeLayout) v.findViewById(R.id.version_file_three_dots_layout);

        holderList.headerLayout = (RelativeLayout) v.findViewById(R.id.version_file_header_layout);
        holderList.titleHeader = (TextView) v.findViewById(R.id.version_file_header_title);
        holderList.sizeHeader = (TextView) v.findViewById(R.id.version_file_header_size);

        holderList.itemLayout.setTag(holderList);
        holderList.itemLayout.setOnClickListener(this);

        switch (((VersionsFileActivity) context).getAccessLevel()) {
            case MegaShare.ACCESS_FULL:
            case MegaShare.ACCESS_OWNER:
                holderList.itemLayout.setOnLongClickListener(this);
                break;

            default:
                holderList.itemLayout.setOnLongClickListener(null);
        }

        holderList.threeDotsLayout.setTag(holderList);
        holderList.threeDotsLayout.setOnClickListener(this);

        v.setTag(holderList);

        return holderList;
    }

    @Override
    public void onBindViewHolder(ViewHolderVersion holder, int position) {
        Timber.d("Position: %s", position);

        MegaNode node = (MegaNode) getItem(position);
        holder.document = node.getHandle();
        Bitmap thumb = null;

        if (position == 0) {
            holder.titleHeader.setText(context.getString(R.string.header_current_section_item));
            holder.sizeHeader.setVisibility(View.GONE);
            holder.headerLayout.setVisibility(View.VISIBLE);
        } else if (position == 1) {
            holder.titleHeader.setText(context.getResources().getQuantityString(R.plurals.header_previous_section_item, megaApi.getNumVersions(node)));

            if (((VersionsFileActivity) context).versionsSize != null) {
                holder.sizeHeader.setText(((VersionsFileActivity) context).versionsSize);
                holder.sizeHeader.setVisibility(View.VISIBLE);
            } else {
                holder.sizeHeader.setVisibility(View.GONE);
            }

            holder.headerLayout.setVisibility(View.VISIBLE);
        } else {
            holder.headerLayout.setVisibility(View.GONE);
        }

        holder.textViewFileName.setText(node.getName());
        holder.textViewFileSize.setText("");

        String fileInfo = getFileInfo(node, context);
        holder.textViewFileSize.setText(fileInfo);

        RelativeLayout.LayoutParams paramsLarge = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
        paramsLarge.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        paramsLarge.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        int leftLarge = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
        paramsLarge.setMargins(leftLarge, 0, 0, 0);

        if (!multipleSelect) {
            Timber.d("Not multiselect");
            holder.itemLayout.setBackground(null);
            holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            holder.imageView.setLayoutParams(paramsLarge);

            Timber.d("Check the thumb");
            CoilUtils.dispose(holder.imageView);
            if (node.hasThumbnail()) {
                Timber.d("Node has thumbnail");
                Coil.imageLoader(context).enqueue(
                        new ImageRequest.Builder(context)
                                .placeholder(MimeTypeList.typeForName(node.getName()).getIconResourceId())
                                .data(ThumbnailRequest.fromHandle(node.getHandle()))
                                .target(holder.imageView)
                                .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                                .listener(new ImageRequest.Listener() {
                                    @Override
                                    public void onSuccess(@NonNull ImageRequest request, @NonNull SuccessResult result) {
                                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
                                        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                        int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
                                        int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
                                        params.setMargins(left, 0, right, 0);
                                        holder.imageView.setLayoutParams(params);
                                    }
                                })
                                .build()
                );
            } else {
                Timber.d("Node NOT thumbnail");
                holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            }
        } else {
            Timber.d("Multiselection ON");
            if (this.isItemChecked(position)) {
                holder.imageView.setLayoutParams(paramsLarge);
                holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder);
            } else {
                holder.itemLayout.setBackground(null);

                Timber.d("Check the thumb");
                holder.imageView.setLayoutParams(paramsLarge);

                CoilUtils.dispose(holder.imageView);
                if (node.hasThumbnail()) {
                    Timber.d("Node has thumbnail");
                    Coil.imageLoader(context).enqueue(
                            new ImageRequest.Builder(context)
                                    .placeholder(MimeTypeList.typeForName(node.getName()).getIconResourceId())
                                    .data(ThumbnailRequest.fromHandle(node.getHandle()))
                                    .target(holder.imageView)
                                    .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                                    .listener(new ImageRequest.Listener() {
                                        @Override
                                        public void onSuccess(@NonNull ImageRequest request, @NonNull SuccessResult result) {
                                            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
                                            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                            int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
                                            int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
                                            params.setMargins(left, 0, right, 0);
                                            holder.imageView.setLayoutParams(params);
                                        }
                                    })
                                    .build()
                    );
                } else {
                    Timber.d("Node NOT thumbnail");
                    holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        if (nodes != null) {
            return nodes.size();
        } else {
            return 0;
        }
    }

    public Object getItem(int position) {
        if (nodes != null) {
            return nodes.get(position);
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

        ViewHolderVersion holder = (ViewHolderVersion) v.getTag();
        int currentPosition = holder.getAdapterPosition();
        Timber.d("Current position: %s", currentPosition);

        if (currentPosition < 0) {
            Timber.e("Current position error - not valid value");
            return;
        } else if (multipleSelect && currentPosition < 1) {
            Timber.e("Current Version cannot be selected when Multiple Select is activated");
            return;
        }

        final MegaNode megaNode = (MegaNode) getItem(currentPosition);
        int id = v.getId();
        if (id == R.id.version_file_three_dots_layout) {
            Timber.d("version_file_three_dots: %s", currentPosition);
            if (!isOnline(context)) {
                ((VersionsFileActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (multipleSelect) {
                ((VersionsFileActivity) context).itemClick(currentPosition);
            } else {
                ((VersionsFileActivity) context).showVersionsBottomSheetDialog(megaNode, currentPosition);

            }
        } else if (id == R.id.version_file_item_layout) {
            ((VersionsFileActivity) context).itemClick(currentPosition);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        ViewHolderVersion holder = (ViewHolderVersion) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        if (!isMultipleSelect()) {
            // The Current Version is not allowed to be long-pressed
            if (currentPosition < 1) {
                Timber.w("Position not valid: %s", currentPosition);
            } else {
                setMultipleSelect(true);
                ((VersionsFileActivity) context).startActionMode(currentPosition);
            }
        }

        return true;
    }

    /*
     * Get document at specified position
     */
    public MegaNode getNodeAt(int position) {
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
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }
}