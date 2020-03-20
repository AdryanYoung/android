package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;

import java.util.ArrayList;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.managerSections.RotatableFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ContactFileBaseFragment extends RotatableFragment {
    
    public static int REQUEST_CODE_GET = 1000;
    public static int REQUEST_CODE_GET_LOCAL = 1003;
    public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
    
    protected MegaApiAndroid megaApi;
    protected ActionBar aB;
    protected Context context;
    protected String userEmail;
    protected MegaUser contact;
    protected long parentHandle = -1;
    protected Stack<Integer> lastPositionStack;
    protected ArrayList<MegaNode> contactNodes;
    protected int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
    protected DatabaseHandler dbH = null;
    protected MegaPreferences prefs = null;
    protected String downloadLocationDefaultPath;
    protected DisplayMetrics outMetrics;

    protected MegaNodeAdapter adapter;
    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("ContactFileBaseFragment onCreate");
        super.onCreate(savedInstanceState);
        
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    
        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }
        
        dbH = DatabaseHandler.getDbHandler(context);
        prefs = dbH.getPreferences();

        downloadLocationDefaultPath = getDownloadLocation(context);
        
        lastPositionStack = new Stack<>();
    
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
        if (aB != null){
            aB.show();
            ((AppCompatActivity) context).invalidateOptionsMenu();
        }
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
        if (aB != null){
            aB.show();
            ((AppCompatActivity) context).invalidateOptionsMenu();
        }
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
    }
    
    public void setUserEmail(String userEmail){
        this.userEmail = userEmail;
    }
    
    public String getUserEmail(){
        return this.userEmail;
    }
    
    public String getDescription(ArrayList<MegaNode> nodes) {
        int numFolders = 0;
        int numFiles = 0;
        
        for (int i = 0; i < nodes.size(); i++) {
            MegaNode c = nodes.get(i);
            if (c.isFolder()) {
                numFolders++;
            } else {
                numFiles++;
            }
        }
        
        String info;
        if (numFolders > 0) {
            info = numFolders
                    + " "
                    + getResources().getQuantityString(
                    R.plurals.general_num_folders, numFolders);
            if (numFiles > 0) {
                info = info
                        + ", "
                        + numFiles
                        + " "
                        + getResources().getQuantityString(
                        R.plurals.general_num_files, numFiles);
            }
        } else {
            if (numFiles == 0) {
                info = numFiles
                        + " "
                        + getResources().getQuantityString(
                        R.plurals.general_num_folders, numFolders);
            } else {
                info = numFiles
                        + " "
                        + getResources().getQuantityString(
                        R.plurals.general_num_files, numFiles);
            }
        }
        
        return info;
    }

    @Override
    protected RotatableAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void activateActionMode() {
        logDebug("activateActionMode");
    }

    @Override
    public void multipleItemClick(int position) {
        adapter.toggleSelection(position);
    }

    @Override
    public void reselectUnHandledSingleItem(int position) {
    }

    @Override
    protected void updateActionModeTitle() {
    }
}
