/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.Manifest;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import com.android.internal.util.XmlUtils;
import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.SearchActivity;
import com.cyanogenmod.filemanager.activities.preferences.SettingsPreferences;
import com.cyanogenmod.filemanager.adapters.MenuSettingsAdapter;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.console.VirtualConsole;
import com.cyanogenmod.filemanager.console.VirtualMountPointConsole;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.listeners.OnHistoryListener;
import com.cyanogenmod.filemanager.listeners.OnRequestRefreshListener;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.model.DiskUsage;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.History;
import com.cyanogenmod.filemanager.model.MountPoint;
import com.cyanogenmod.filemanager.model.Bookmark.BOOKMARK_TYPE;
import com.cyanogenmod.filemanager.parcelables.HistoryNavigable;
import com.cyanogenmod.filemanager.parcelables.NavigationViewInfoParcelable;
import com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable;
import com.cyanogenmod.filemanager.preferences.AccessMode;
import com.cyanogenmod.filemanager.preferences.Bookmarks;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.NavigationLayoutMode;
import com.cyanogenmod.filemanager.preferences.ObjectIdentifier;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.dialogs.ActionsDialog;
import com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog;
import com.cyanogenmod.filemanager.ui.dialogs.InitialDirectoryDialog;
import com.cyanogenmod.filemanager.ui.dialogs.FilesystemInfoDialog.OnMountListener;
import com.cyanogenmod.filemanager.ui.policy.CopyMoveActionPolicy;
import com.cyanogenmod.filemanager.ui.widgets.Breadcrumb;
import com.cyanogenmod.filemanager.ui.widgets.ButtonItem;
import com.cyanogenmod.filemanager.ui.widgets.NavigationCustomTitleView;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnNavigationRequestMenuListener;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnNavigationSelectionChangedListener;
import com.cyanogenmod.filemanager.ui.widgets.SelectionView;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.BookmarksHelper;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.ExceptionUtil.OnRelaunchCommandResult;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;
import com.cyanogenmod.filemanager.util.MountPointHelper;
import com.cyanogenmod.filemanager.util.StorageHelper;
import com.cyngn.uicommon.view.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.*;
import static com.cyanogenmod.filemanager.activities.PickerActivity.EXTRA_FOLDER_PATH;

/**
 * The main navigation activity. This activity is the center of the application.
 * From this the user can navigate, search, make actions.<br/>
 * This activity is singleTop, so when it is displayed no other activities exists in
 * the stack.<br/>
 * This cause an issue with the saved instance of this class, because if another activity
 * is displayed, and the process is killed, NavigationFragment is started and the saved
 * instance gets corrupted.<br/>
 * For this reason the methods {link {@link Activity#onSaveInstanceState(Bundle)} and
 * {@link Activity#onRestoreInstanceState(Bundle)} are not implemented, and every time
 * the app is killed, is restarted from his initial state.
 */
public class NavigationFragment extends Fragment
        implements OnHistoryListener, OnRequestRefreshListener,
        OnNavigationRequestMenuListener, OnNavigationSelectionChangedListener {

    private static final String TAG = "NavigationFragment"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    // Bookmark list XML tags
    private static final String TAG_BOOKMARKS = "Bookmarks"; //$NON-NLS-1$
    private static final String TAG_BOOKMARK = "bookmark"; //$NON-NLS-1$

    private static final String STR_USB = "usb"; // $NON-NLS-1$

    /**
     * Intent code for request a search.
     */
    public static final int INTENT_REQUEST_SEARCH = 10001;

    /**
     * Intent code for request a search.
     */
    public static final int INTENT_REQUEST_SETTINGS = 20001;

    /**
     * Intent code for request a copy.
     */
    public static final int INTENT_REQUEST_COPY = 30001;

    /**
     * Intent code for request a move.
     */
    public static final int INTENT_REQUEST_MOVE = 30002;

    /**
     * Constant for extra information about selected search entry.
     */
    public static final String EXTRA_SEARCH_ENTRY_SELECTION =
            "extra_search_entry_selection"; //$NON-NLS-1$

    /**
     * Constant for extra information about last search data.
     */
    public static final String EXTRA_SEARCH_LAST_SEARCH_DATA =
            "extra_search_last_search_data"; //$NON-NLS-1$

    /**
     * Constant for extra information for request a navigation to the passed path.
     */
    public static final String EXTRA_NAVIGATE_TO =
            "extra_navigate_to"; //$NON-NLS-1$

    /**
     * Constant for extra information for request to add navigation to the history
     */
    public static final String EXTRA_ADD_TO_HISTORY =
            "extra_add_to_history"; //$NON-NLS-1$

    // The timeout needed to reset the exit status for back button
    // After this time user need to tap 2 times the back button to
    // exit, and the toast is shown again after the first tap.
    private static final int RELEASE_EXIT_CHECK_TIMEOUT = 3500;

    private Toolbar mToolBar;
    private SearchView mSearchView;
    private NavigationCustomTitleView mCustomTitleView;
    private InputMethodManager mImm;
    private ListPopupWindow mPopupWindow;
    private ActionsDialog mActionsDialog;
    private View mTitleLayout;
    private View mStatusBar;


    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().compareTo(FileManagerSettings.INTENT_SETTING_CHANGED) == 0) {
                    // The settings has changed
                    String key = intent.getStringExtra(FileManagerSettings.
                            EXTRA_SETTING_CHANGED_KEY);
                    if (key != null) {
                        // Disk usage warning level
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_DISK_USAGE_WARNING_LEVEL.getId()) == 0) {

                            // Set the free disk space warning level of the breadcrumb widget
                            Breadcrumb breadcrumb = getCurrentNavigationView().getBreadcrumb();
                            String fds = Preferences.getSharedPreferences().getString(
                                    FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                                    (String)FileManagerSettings.
                                        SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
                            breadcrumb.setFreeDiskSpaceWarningLevel(Integer.parseInt(fds));
                            breadcrumb.updateMountPointInfo();
                            return;
                        }

                        // Case sensitive sort
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_CASE_SENSITIVE_SORT.getId()) == 0) {
                            getCurrentNavigationView().refresh();
                            return;
                        }

                        // Display thumbs
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_DISPLAY_THUMBS.getId()) == 0) {
                            // Clean the icon cache applying the current theme
                            applyTheme();
                            return;
                        }

                        // Use flinger
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_USE_FLINGER.getId()) == 0) {
                            boolean useFlinger =
                                    Preferences.getSharedPreferences().getBoolean(
                                            FileManagerSettings.SETTINGS_USE_FLINGER.getId(),
                                                ((Boolean)FileManagerSettings.
                                                        SETTINGS_USE_FLINGER.
                                                        getDefaultValue()).booleanValue());
                            getCurrentNavigationView().setUseFlinger(useFlinger);
                            return;
                        }

                        // Access mode
                        if (key.compareTo(FileManagerSettings.
                                        SETTINGS_ACCESS_MODE.getId()) == 0) {
                            // Is it necessary to create or exit of the ChRooted?
                            boolean chRooted =
                                    FileManagerApplication.
                                            getAccessMode().compareTo(AccessMode.SAFE) == 0;
                            if (chRooted != NavigationFragment.this.mChRooted) {
                                if (chRooted) {
                                    createChRooted();
                                } else {
                                    exitChRooted();
                                }
                            }
                        }

                        // Restricted access
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_RESTRICT_SECONDARY_USERS_ACCESS.getId()) == 0) {
                            if (AndroidHelper.isSecondaryUser(context)) {
                                try {
                                    Preferences.savePreference(
                                            FileManagerSettings.SETTINGS_ACCESS_MODE,
                                            AccessMode.SAFE, true);
                                } catch (Throwable ex) {
                                    Log.w(TAG, "can't save console preference", ex); //$NON-NLS-1$
                                }
                                ConsoleBuilder.changeToNonPrivilegedConsole(context);
                                createChRooted();
                            }
                        }

                        // Filetime format mode
                        if (key.compareTo(FileManagerSettings.
                                SETTINGS_FILETIME_FORMAT_MODE.getId()) == 0) {
                            // Refresh the data
                            synchronized (FileHelper.DATETIME_SYNC) {
                                FileHelper.sReloadDateTimeFormats = true;
                                getCurrentNavigationView().refresh();
                            }
                        }
                    }

                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_FILE_CHANGED) == 0) {
                    // Retrieve the file that was changed
                    String file =
                            intent.getStringExtra(FileManagerSettings.EXTRA_FILE_CHANGED_KEY);
                    try {
                        FileSystemObject fso = CommandHelper.getFileInfo(context, file, null);
                        if (fso != null) {
                            getCurrentNavigationView().refresh(fso);
                        }
                    } catch (Exception e) {
                        ExceptionUtil.translateException(context, e, true, false);
                    }

                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_THEME_CHANGED) == 0) {
                    applyTheme();

                } else if (intent.getAction().compareTo(Intent.ACTION_TIME_CHANGED) == 0 ||
                           intent.getAction().compareTo(Intent.ACTION_DATE_CHANGED) == 0 ||
                           intent.getAction().compareTo(Intent.ACTION_TIMEZONE_CHANGED) == 0) {
                    // Refresh the data
                    synchronized (FileHelper.DATETIME_SYNC) {
                        FileHelper.sReloadDateTimeFormats = true;
                        NavigationFragment.this.getCurrentNavigationView().refresh();
                    }
                } else if (intent.getAction().compareTo(
                        FileManagerSettings.INTENT_MOUNT_STATUS_CHANGED) == 0 ||
                            intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) ||
                            intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    MountPointHelper.refreshMountPoints(
                            FileManagerApplication.getBackgroundConsole());
                    onRequestBookmarksRefresh();
                    removeUnmountedHistory();
                    removeUnmountedSelection();
                }
            }
        }
    };

    private OnClickListener mOnClickDrawerTabListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.drawer_bookmarks_tab:
                    if (!mBookmarksTab.isSelected()) {
                        mBookmarksTab.setSelected(true);
                        mHistoryTab.setSelected(false);
                        mBookmarksTab.setTextAppearance(
                                getActivity(), R.style.primary_text_appearance);
                        mHistoryTab.setTextAppearance(
                                getActivity(), R.style.secondary_text_appearance);
                        mHistoryLayout.setVisibility(View.GONE);
                        mBookmarksLayout.setVisibility(View.VISIBLE);
                        applyTabTheme();

                        try {
                            Preferences.savePreference(FileManagerSettings.USER_PREF_LAST_DRAWER_TAB,
                                    Integer.valueOf(0), true);
                        } catch (Exception ex) {
                            Log.e(TAG, "Can't save last drawer tab", ex); //$NON-NLS-1$
                        }

                        mClearHistory.setVisibility(View.GONE);
                    }
                    break;
                case R.id.drawer_history_tab:
                    if (!mHistoryTab.isSelected()) {
                        mHistoryTab.setSelected(true);
                        mBookmarksTab.setSelected(false);
                        mHistoryTab.setTextAppearance(
                                getActivity(), R.style.primary_text_appearance);
                        mBookmarksTab.setTextAppearance(
                                getActivity(), R.style.secondary_text_appearance);
                        mBookmarksLayout.setVisibility(View.GONE);
                        mHistoryLayout.setVisibility(View.VISIBLE);
                        applyTabTheme();

                        try {
                            Preferences.savePreference(FileManagerSettings.
                                    USER_PREF_LAST_DRAWER_TAB, Integer.valueOf(1), true);
                        } catch (Exception ex) {
                            Log.e(TAG, "Can't save last drawer tab", ex); //$NON-NLS-1$
                        }

                        mClearHistory.setVisibility(mHistory.size() > 0 ? View.VISIBLE : View.GONE);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private OnClickListener mOnClickDrawerActionBarListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ab_settings:
                    openSettings();
                    break;
                case R.id.ab_clear_history:
                    clearHistory();
                    mClearHistory.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    };

    static String MIME_TYPE_LOCALIZED_NAMES[];
    /**
     * @hide
     */
    static Map<MimeTypeCategory, Drawable> EASY_MODE_ICONS = new
            HashMap<MimeTypeCategory, Drawable>();

    /**
     * @hide
     */
    NavigationView[] mNavigationViews;

    /**
     * Used to record the operation steps
     */
    private List<History> mHistory;

    /**
     * Used to record the items saved in database
     */
    private List<History> mHistorySaved;

    private int mCurrentNavigationView;

    private ViewGroup mActionBar;
    private SelectionView mSelectionBar;

    private LinearLayout mDrawerHistory;
    private TextView mDrawerHistoryEmpty;

    private TextView mBookmarksTab;
    private TextView mHistoryTab;
    private View mBookmarksLayout;
    private View mHistoryLayout;

    private ButtonItem mSettings;
    private ButtonItem mClearHistory;

    private List<Bookmark> mBookmarks;
    private List<Bookmark> mSdBookmarks;
    private LinearLayout mDrawerBookmarks;

    private boolean mExitFlag = false;
    private long mExitBackTimeout = -1;

    private Dialog mActiveDialog = null;

    private int mOrientation;


    /**
     * @hide
     */
    public boolean mChRooted;

    /**
     * @hide
     */
    Handler mHandler;
    View mView;
    LayoutInflater mLayoutInflater;

    private AsyncTask<Void, Void, Boolean> mBookmarksTask;
    private AsyncTask<Void, Void, Boolean> mHistoryTask;

    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mLayoutInflater = inflater;

        if (DEBUG) {
            Log.d(TAG, "NavigationFragment.onCreate"); //$NON-NLS-1$
        }

        // Set the theme before setContentView
        ThemeManager.Theme theme = ThemeManager.getCurrentTheme(getActivity());
        theme.setBaseThemeNoActionBar(getActivity());

        //Set the main layout of the activity
        mView = inflater.inflate(R.layout.nav_fragment, container, false);

        //Initialize activity
        init();

        initNavigationViews();

        return mView;
    }


    @Override
    public void onStart() {
        super.onStart();

        mToolBar = (Toolbar) mView.findViewById(R.id.material_toolbar);
        ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
        actionBarActivity.setSupportActionBar(mToolBar);
        actionBarActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        actionBarActivity.getSupportActionBar().setHomeButtonEnabled(true);
        actionBarActivity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

        //Initialize action bars
        initTitleActionBar();
        initStatusActionBar();
        initSelectionBar();
        initBookmarks();
        initHistory();

        // Apply the theme
        applyTheme();

        this.mHandler = new Handler();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Initialize console
                initConsole();

                //Initialize navigation
                int cc = NavigationFragment.this.mNavigationViews.length;
                for (int i = 0; i < cc; i++) {
                    initNavigation(i, false, getActivity().getIntent());
                }

                //Check the intent action
                checkIntent(getActivity().getIntent());
            }
        });

        // Adjust layout (only when start on landscape mode)
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onLayoutChanged();
        }
        this.mOrientation = orientation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mSearchView.getVisibility() == View.VISIBLE) {
            closeSearch();
        }

        // Check restrictions
        if (!FileManagerApplication.checkRestrictSecondaryUsersAccess(getActivity(), mChRooted)) {
            return;
        }

        // Check that the current dir is mounted (for virtual filesystems)
        String curDir = mNavigationViews[mCurrentNavigationView].getCurrentDir();
        if (curDir != null) {
            VirtualMountPointConsole vc = VirtualMountPointConsole.getVirtualConsoleForPath(
                    mNavigationViews[mCurrentNavigationView].getCurrentDir());
            if (vc != null && !vc.isMounted()) {
                onRequestBookmarksRefresh();
                removeUnmountedHistory();
                removeUnmountedSelection();

                Intent intent = new Intent();
                intent.putExtra(EXTRA_ADD_TO_HISTORY, false);
                initNavigation(NavigationFragment.this.mCurrentNavigationView, false, intent);
            }

            getCurrentNavigationView().refresh(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onLayoutChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "NavigationFragment.onDestroy"); //$NON-NLS-1$
        }

        if (mActiveDialog != null && mActiveDialog.isShowing()) {
            mActiveDialog.dismiss();
        }

        // Unregister the receiver
        try {
            getActivity().unregisterReceiver(this.mNotificationReceiver);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }

        recycle();
        //All destroy. Continue
        super.onDestroy();
    }

    /**
     * Method that returns the current navigation view.
     *
     * @return NavigationView The current navigation view
     */
    public NavigationView getCurrentNavigationView() {
        return getNavigationView(this.mCurrentNavigationView);
    }

    /**
     * Method that returns the current navigation view.
     *
     * @param viewId The view to return
     * @return NavigationView The current navigation view
     */
    public NavigationView getNavigationView(int viewId) {
        if (this.mNavigationViews == null) return null;
        return this.mNavigationViews[viewId];
    }

    /**
     * Method that initializes the activity.
     */
    private void init() {
        this.mHistory = new ArrayList<History>();
        this.mHistorySaved = new ArrayList<History>();
        this.mChRooted = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;
    }

    /**
     * Method that initializes the titlebar of the activity.
     */
    private void initTitleActionBar() {
        //Inflate the view and associate breadcrumb
        mTitleLayout = mLayoutInflater.inflate(
                R.layout.navigation_view_customtitle, null, false);
        NavigationCustomTitleView title =
                (NavigationCustomTitleView) mTitleLayout.
                        findViewById(R.id.navigation_title_flipper);
        title.setOnHistoryListener(this);
        Breadcrumb breadcrumb = (Breadcrumb)title.findViewById(R.id.breadcrumb_view);
        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            this.mNavigationViews[i].setBreadcrumb(breadcrumb);
            this.mNavigationViews[i].setOnHistoryListener(this);
            this.mNavigationViews[i].setOnNavigationSelectionChangedListener(this);
            this.mNavigationViews[i].setOnNavigationOnRequestMenuListener(this);
            this.mNavigationViews[i].setCustomTitle(title);
        }

        // Set the free disk space warning level of the breadcrumb widget
        String fds = Preferences.getSharedPreferences().getString(
                FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getId(),
                (String) FileManagerSettings.SETTINGS_DISK_USAGE_WARNING_LEVEL.getDefaultValue());
        breadcrumb.setFreeDiskSpaceWarningLevel(Integer.parseInt(fds));

        //Configure the action bar options
        mToolBar.setBackgroundDrawable(
                getResources().getDrawable(R.drawable.bg_material_titlebar));
        mToolBar.addView(mTitleLayout);
    }

    /**
     * Method that initializes the statusbar of the activity.
     */
    private void initStatusActionBar() {
        //Performs a width calculation of buttons. Buttons exceeds the width
        //of the action bar should be hidden
        //This application not use android ActionBar because the application
        //make uses of the title and bottom areas, and wants to force to show
        //the overflow button (without care of physical buttons)
        this.mActionBar = (ViewGroup) mView.findViewById(R.id.navigation_actionbar);
        this.mActionBar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View v, int left, int top, int right, int bottom, int oldLeft,
                    int oldTop, int oldRight, int oldBottom) {
                //Get the width of the action bar
                int w = v.getMeasuredWidth();

                //Wake through children calculation his dimensions
                int bw = (int)getResources().getDimension(R.dimen.default_buttom_width);
                int cw = 0;
                final ViewGroup abView = ((ViewGroup)v);
                int cc = abView.getChildCount();
                for (int i = 0; i < cc; i++) {
                    View child = abView.getChildAt(i);
                    child.setVisibility(cw + bw > w ? View.INVISIBLE : View.VISIBLE);
                    cw += bw;
                }
            }
        });

        // Have overflow menu? Actually no. There is only a search action, so just hide
        // the overflow
        View overflow = mView.findViewById(R.id.ab_overflow);
        overflow.setVisibility(View.GONE);

        // Show the status bar
        View statusBar = mView.findViewById(R.id.navigation_statusbar_portrait_holder);
        statusBar.setVisibility(View.VISIBLE);
    }

    /**
     * Method that initializes the selectionbar of the activity.
     */
    private void initSelectionBar() {
        this.mSelectionBar = (SelectionView) mView.findViewById(R.id.navigation_selectionbar);
    }

    /**
     * Method that initializes the navigation drawer of the activity.
     */
    private void initDrawer() {
        // TODO: Move into MainActivity or remove altogether.
        /*mDrawerLayout = (android.support.v4.widget.DrawerLayout) mView.findViewById(
                R.id.drawer_layout);
        //Set our status bar color
        mDrawerLayout.setStatusBarBackgroundColor(R.color.material_palette_blue_primary_dark);
        mDrawer = (ViewGroup) mView.findViewById(
                R.id.drawer);
        mDrawerBookmarks = (android.widget.LinearLayout) mView.findViewById(
                R.id.bookmarks_list);
        mDrawerHistory = (android.widget.LinearLayout) mView.findViewById(
                R.id.history_list);
        mDrawerHistoryEmpty = (TextView) mView.findViewById(
                R.id.history_empty);

        mBookmarksLayout = mView.findViewById(R.id.drawer_bookmarks);
        mHistoryLayout = mView.findViewById(R.id.drawer_history);
        mBookmarksTab = (TextView) mView.findViewById(
                R.id.drawer_bookmarks_tab);
        mHistoryTab = (TextView) mView.findViewById(
                R.id.drawer_history_tab);
        mBookmarksTab.setOnClickListener(mOnClickDrawerTabListener);
        mHistoryTab.setOnClickListener(mOnClickDrawerTabListener);

        mSettings = (cButtonItem) mView.findViewById(
                R.id.ab_settings);
        mSettings.setOnClickListener(mOnClickDrawerActionBarListener);
        mClearHistory = (cButtonItem) mView.findViewById(
                R.id.ab_clear_history);
        mClearHistory.setOnClickListener(mOnClickDrawerActionBarListener);

        // Restore the last tab pressed
        Integer lastTab = Preferences.getSharedPreferences().getInt(
                FileManagerSettings.USER_PREF_LAST_DRAWER_TAB.getId(),
                (Integer) FileManagerSettings.USER_PREF_LAST_DRAWER_TAB
                        .getDefaultValue());
        mOnClickDrawerTabListener.onClick(lastTab == 0 ? mBookmarksTab : mHistoryTab);

        // Set the navigation drawer "hamburger" icon
        mDrawerToggle = new android.support.v4.app.ActionBarDrawerToggle(getActivity(),
                mDrawerLayout,
                R.drawable.ic_material_light_navigation_drawer,
                R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                onDrawerLayoutOpened(drawerView);
                super.onDrawerOpened(drawerView);
            }
        };
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);*/
    }

    /***
     * Method that do something when the DrawerLayout opened.
     */
    private void onDrawerLayoutOpened(View drawerView){
        if (mSearchView != null && mSearchView.getVisibility() == View.VISIBLE) {
            closeSearch();
            hideSoftInput(drawerView);
        }
    }

    /**
     * Method that hide the software when the software showing.
     *
     * */
    private void hideSoftInput(View view){
        if (mImm != null) {
            mImm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Method that adds a history entry to the history list in the drawer
     */
    private void addHistoryToDrawer(int index, HistoryNavigable navigable) {
        // hide empty message
        mDrawerHistoryEmpty.setVisibility(View.GONE);

        Theme theme = ThemeManager.getCurrentTheme(getActivity());
        IconHolder iconholder = new IconHolder(getActivity(), false);

        // inflate single bookmark layout item and fill it
        LinearLayout view = (LinearLayout) mLayoutInflater.inflate(
                R.layout.history_item, null);

        ImageView iconView = (ImageView) view
                .findViewById(R.id.history_item_icon);
        TextView name = (TextView) view.findViewById(R.id.history_item_name);
        TextView directory = (TextView) view
                .findViewById(R.id.history_item_directory);

        Drawable icon = iconholder.getDrawable("ic_fso_folder_drawable"); //$NON-NLS-1$
        if (navigable instanceof SearchInfoParcelable) {
            icon = iconholder.getDrawable("ic_history_search_drawable"); //$NON-NLS-1$
        }
        iconView.setImageDrawable(icon);

        String title = navigable.getTitle();
        if (title == null || title.trim().length() == 0) {
            title = getString(R.string.root_directory_name);
        }

        name.setText(title);
        directory.setText(navigable.getDescription());

        theme.setTextColor(getActivity(), name, "text_color");
        theme.setTextColor(getActivity(), directory, "text_color");

        // handle item click
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int index = mDrawerHistory.indexOfChild(v);
                final int count = mDrawerHistory.getChildCount();
                final History history = mHistorySaved.get(count - index - 1);

                navigateToHistory(history, true);
            }
        });

        // add as first child
        mDrawerHistory.addView(view, 0);

        // Show clear button if history tab is selected
        mClearHistory.setVisibility(mHistoryTab.isSelected() ? View.VISIBLE : View.GONE);
    }

    /**
     * Method takes a bookmark as argument and adds it to mBookmarks and the
     * list in the drawer
     */
    public void addBookmark(Bookmark bookmark) {
        mBookmarks.add(bookmark);
        addBookmarkToDrawer(bookmark);
    }

    /**
     * Method takes a bookmark as argument and adds it to the bookmark list in
     * the drawer
     */
    private void addBookmarkToDrawer(Bookmark bookmark) {
        Theme theme = ThemeManager.getCurrentTheme(getActivity());
        IconHolder iconholder = new IconHolder(getActivity(), false);

        // inflate single bookmark layout item and fill it
        LinearLayout view = (LinearLayout) mLayoutInflater.inflate(
                R.layout.bookmarks_item, null);

        ImageView icon = (ImageView) view
                .findViewById(R.id.bookmarks_item_icon);
        TextView name = (TextView) view.findViewById(R.id.bookmarks_item_name);
        TextView path = (TextView) view.findViewById(R.id.bookmarks_item_path);
        ImageButton actionButton = (ImageButton) view
                .findViewById(R.id.bookmarks_item_action);

        name.setText(bookmark.mName);
        path.setText(bookmark.mPath);

        theme.setTextColor(getActivity(), name, "text_color");
        theme.setTextColor(getActivity(), path, "text_color");

        icon.setImageDrawable(iconholder.getDrawable(BookmarksHelper
                .getIcon(bookmark)));

        Drawable action = null;
        String actionCd = null;
        if (bookmark.mType.compareTo(BOOKMARK_TYPE.HOME) == 0) {
            action = iconholder.getDrawable("ic_config_drawable"); //$NON-NLS-1$
            actionCd = getActivity().getApplicationContext().getString(
                    R.string.bookmarks_button_config_cd);
        }
        else if (bookmark.mType.compareTo(BOOKMARK_TYPE.USER_DEFINED) == 0) {
            action = iconholder.getDrawable("ic_close_drawable"); //$NON-NLS-1$
            actionCd = getActivity().getApplicationContext().getString(
                    R.string.bookmarks_button_remove_bookmark_cd);
        }

        actionButton.setImageDrawable(action);
        actionButton.setVisibility(action != null ? View.VISIBLE : View.GONE);
        actionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final View v = (View) view.getParent();
                final int index = mDrawerBookmarks.indexOfChild(v);
                final Bookmark bookmark = mBookmarks.get(index);

                // Configure home
                if (bookmark.mType.compareTo(BOOKMARK_TYPE.HOME) == 0) {
                    // Show a dialog for configure initial directory
                    InitialDirectoryDialog dialog = new InitialDirectoryDialog(
                            getActivity());
                    dialog.setOnValueChangedListener(
                            new InitialDirectoryDialog.OnValueChangedListener() {
                        @Override
                        public void onValueChanged(String newInitialDir) {
                            bookmark.mPath = newInitialDir;

                            // reset drawer bookmarks list
                            initBookmarks();
                        }
                    });
                    dialog.show();
                    return;
                }

                // Remove bookmark
                if (bookmark.mType.compareTo(BOOKMARK_TYPE.USER_DEFINED) == 0) {
                    boolean result = Bookmarks.removeBookmark(
                            getActivity().getApplicationContext(), bookmark);
                    if (!result) { // Show warning
                        DialogHelper.showToast(getActivity().getApplicationContext(),
                                R.string.msgs_operation_failure,
                                Toast.LENGTH_SHORT);
                        return;
                    }
                    mBookmarks.remove(bookmark);
                    mDrawerBookmarks.removeView(v);
                    return;
                }
            }
        });
        actionButton.setContentDescription(actionCd);

        // handle item click
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int index = mDrawerBookmarks.indexOfChild(v);
                final Bookmark bookmark = mBookmarks.get(index);

                // try to navigate to the bookmark path
                try {
                    FileSystemObject fso = CommandHelper.getFileInfo(
                            getActivity().getApplicationContext(), bookmark.mPath, null);
                    if (fso != null) {
                        getCurrentNavigationView().open(fso);
                    }
                    else {
                        // The bookmark does not exist, delete the user-defined
                        // bookmark
                        try {
                            Bookmarks.removeBookmark(getActivity().getApplicationContext(),
                                    bookmark);

                            // reset bookmarks list to default
                            initBookmarks();
                        }
                        catch (Exception ex) {
                        }
                    }
                }
                catch (Exception e) { // Capture the exception
                    ExceptionUtil
                            .translateException(getActivity(), e);
                    if (e instanceof NoSuchFileOrDirectory
                            || e instanceof FileNotFoundException) {
                        // The bookmark does not exist, delete the user-defined
                        // bookmark
                        try {
                            Bookmarks.removeBookmark(getActivity().getApplicationContext(),
                                    bookmark);

                            // reset bookmarks list to default
                            initBookmarks();
                        }
                        catch (Exception ex) {
                        }
                    }
                    return;
                }
            }
        });

        mDrawerBookmarks.addView(view);
    }

    /**
     * Method that initializes the bookmarks.
     */
    private synchronized void initBookmarks() {
        // TODO: Move into MainActivity or remove altogether.
        /*if (mBookmarksTask != null &&
                !mBookmarksTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            return;
        }

        // Retrieve the loading view
        final View waiting = mView.findViewById(
                R.id.bookmarks_loading);

        // Load bookmarks in background
        mBookmarksTask = new android.os.AsyncTask<Void, Void, Boolean>() {
            Exception mCause;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mBookmarks = loadBookmarks();
                    return Boolean.TRUE;

                }
                catch (Exception e) {
                    this.mCause = e;
                    return Boolean.FALSE;
                }
            }

            @Override
            protected void onPreExecute() {
                waiting.setVisibility(View.VISIBLE);
                mDrawerBookmarks.removeAllViews();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                waiting.setVisibility(View.GONE);
                if (result.booleanValue()) {
                    for (Bookmark bookmark : mBookmarks) {
                        addBookmarkToDrawer(bookmark);
                    }
                }
                else {
                    if (this.mCause != null) {
                        ExceptionUtil.translateException(getActivity(), this.mCause);
                    }
                }
                mBookmarksTask = null;
            }

            @Override
            protected void onCancelled() {
                waiting.setVisibility(View.GONE);
                mBookmarksTask = null;
            }
        };
        mBookmarksTask.execute(); */
    }

    /**
     * Method that initializes the history.
     */
    private synchronized void initHistory() {
        if (mHistoryTask != null &&
                !mHistoryTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            return;
        }

        // Load history in background
        mHistoryTask = new AsyncTask<Void, Void, Boolean>() {
            Exception mCause;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    loadHistory();
                    return Boolean.TRUE;
                }
                catch (Exception e) {
                    this.mCause = e;
                    return Boolean.FALSE;
                }
            }

            @Override
            protected void onPreExecute() {
                mDrawerHistory.removeAllViews();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result.booleanValue()) {
                    for (int i = 0; i < mHistory.size(); i++) {
                        final History history = mHistory.get(i);
                        addHistoryToDrawer(i, history.getItem());
                    }
                } else {
                    if (this.mCause != null) {
                        ExceptionUtil.translateException(
                                getActivity(), this.mCause);
                    }
                }
                mHistoryTask = null;
                mHistory.clear();
            }

            @Override
            protected void onCancelled() {
                mHistoryTask = null;
            }
        };
        mHistoryTask.execute();
    }

    /**
     * Method that loads all kind of bookmarks and join in an array to be used
     * in the listview adapter.
     *
     * @return List<Bookmark>
     * @hide
     */
    List<Bookmark> loadBookmarks() {
        // Bookmarks = HOME + FILESYSTEM + SD STORAGES + USER DEFINED
        // In ChRooted mode = SD STORAGES + USER DEFINED (from SD STORAGES)
        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
        if (!this.mChRooted) {
            bookmarks.add(loadHomeBookmarks());
            bookmarks.addAll(loadFilesystemBookmarks());
        }
        mSdBookmarks = loadSdStorageBookmarks();
        bookmarks.addAll(mSdBookmarks);
        bookmarks.addAll(loadVirtualBookmarks());
        bookmarks.addAll(loadUserBookmarks());
        return bookmarks;
    }

    /**
     * Method that loads the home bookmark from the user preference.
     *
     * @return Bookmark The bookmark loaded
     */
    private Bookmark loadHomeBookmarks() {
        String initialDir = Preferences.getSharedPreferences().getString(
                FileManagerSettings.SETTINGS_INITIAL_DIR.getId(),
                (String) FileManagerSettings.SETTINGS_INITIAL_DIR
                        .getDefaultValue());
        return new Bookmark(BOOKMARK_TYPE.HOME,
                getString(R.string.bookmarks_home), initialDir);
    }

    /**
     * Method that loads the filesystem bookmarks from the internal xml file.
     * (defined by this application)
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private List<Bookmark> loadFilesystemBookmarks() {
        try {
            // Initialize the bookmarks
            List<Bookmark> bookmarks = new ArrayList<Bookmark>();

            // Read the command list xml file
            XmlResourceParser parser = getResources().getXml(
                    R.xml.filesystem_bookmarks);

            try {
                // Find the root element
                XmlUtils.beginDocument(parser, TAG_BOOKMARKS);
                while (true) {
                    XmlUtils.nextElement(parser);
                    String element = parser.getName();
                    if (element == null) {
                        break;
                    }

                    if (TAG_BOOKMARK.equals(element)) {
                        CharSequence name = null;
                        CharSequence directory = null;

                        try {
                            name = getString(parser.getAttributeResourceValue(
                                    R.styleable.Bookmark_name, 0));
                        }
                        catch (Exception e) {
                            /** NON BLOCK **/
                        }
                        try {
                            directory = getString(parser
                                    .getAttributeResourceValue(
                                            R.styleable.Bookmark_directory, 0));
                        }
                        catch (Exception e) {
                            /** NON BLOCK **/
                        }
                        if (directory == null) {
                            directory = parser
                                    .getAttributeValue(R.styleable.Bookmark_directory);
                        }
                        if (name != null && directory != null) {
                            bookmarks.add(new Bookmark(
                                    BOOKMARK_TYPE.FILESYSTEM, name.toString(),
                                    directory.toString()));
                        }
                    }
                }

                // Return the bookmarks
                return bookmarks;

            }
            finally {
                parser.close();
            }
        }
        catch (Throwable ex) {
            Log.e(TAG, "Load filesystem bookmarks failed", ex); //$NON-NLS-1$
        }

        // No data
        return new ArrayList<Bookmark>();
    }

    /**
     * Method that loads the secure digital card storage bookmarks from the
     * system.
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private List<Bookmark> loadSdStorageBookmarks() {
        // Initialize the bookmarks
        List<Bookmark> bookmarks = new ArrayList<Bookmark>();

        try {
            // Recovery sdcards from storage manager
            StorageVolume[] volumes = StorageHelper
                    .getStorageVolumes(getActivity().getApplication(), true);
            for (StorageVolume volume: volumes) {
                if (volume != null) {
                    String mountedState = volume.getState();
                    String path = volume.getPath();
                    if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(mountedState) &&
                            !Environment.MEDIA_MOUNTED_READ_ONLY.equalsIgnoreCase(mountedState)) {
                        Log.w(TAG, "Ignoring '" + path + "' with state of '"+ mountedState + "'");
                        continue;
                    }
                    if (!TextUtils.isEmpty(path)) {
                        String lowerPath = path.toLowerCase(Locale.ROOT);
                        Bookmark bookmark;
                        if (lowerPath.contains(STR_USB)) {
                            bookmark = new Bookmark(BOOKMARK_TYPE.USB, StorageHelper
                                    .getStorageVolumeDescription(getActivity().getApplication(),
                                            volume), path);
                        } else {
                            bookmark = new Bookmark(BOOKMARK_TYPE.SDCARD, StorageHelper
                                    .getStorageVolumeDescription(getActivity().getApplication(),
                                            volume), path);
                        }
                        bookmarks.add(bookmark);
                    }
                }
            }

            // Return the bookmarks
            return bookmarks;
        }
        catch (Throwable ex) {
            Log.e(TAG, "Load filesystem bookmarks failed", ex); //$NON-NLS-1$
        }

        // No data
        return new ArrayList<Bookmark>();
    }

    /**
     * Method that loads all virtual mount points.
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private List<Bookmark> loadVirtualBookmarks() {
        // Initialize the bookmarks
        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
        List<MountPoint> mps = VirtualMountPointConsole.getVirtualMountPoints();
        for (MountPoint mp : mps) {
            BOOKMARK_TYPE type = null;
            String name = null;
            if (mp.isSecure()) {
                type = BOOKMARK_TYPE.SECURE;
                name = getString(R.string.bookmarks_secure);
            } else if (mp.isRemote()) {
                type = BOOKMARK_TYPE.REMOTE;
                name = getString(R.string.bookmarks_remote);
            } else {
                continue;
            }
            bookmarks.add(new Bookmark(type, name, mp.getMountPoint()));
        }
        return bookmarks;
    }

    /**
     * Method that loads the user bookmarks (added by the user).
     *
     * @return List<Bookmark> The bookmarks loaded
     */
    private List<Bookmark> loadUserBookmarks() {
        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
        Cursor cursor = Bookmarks.getAllBookmarks(getActivity().getContentResolver());
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Bookmark bm = new Bookmark(cursor);
                    if (this.mChRooted
                            && !StorageHelper.isPathInStorageVolume(bm.mPath)) {
                        continue;
                    }
                    bookmarks.add(bm);
                }
                while (cursor.moveToNext());
            }
        }
        finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            }
            catch (Exception e) {
                /** NON BLOCK **/
            }
        }

        // Remove bookmarks from virtual storage if the filesystem is not mount
        int c = bookmarks.size() - 1;
        for (int i = c; i >= 0; i--) {
            VirtualMountPointConsole vc =
                    VirtualMountPointConsole.getVirtualConsoleForPath(bookmarks.get(i).mPath);
            if (vc != null && !vc.isMounted()) {
                bookmarks.remove(i);
            }
        }

        return bookmarks;
    }

    /**
     * Method that loads the history saved in database.
     */
    private void loadHistory() {
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = contentResolver.query(
                History.Columns.CONTENT_URI,
                History.Columns.HISTORY_QUERY_COLUMNS,
                null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(1);
                    String desc = cursor.getString(2);
                    HistoryItem item = new HistoryItem(title, desc);
                    History history = new History(mHistory.size(), item);

                    mHistory.add(history);
                    mHistorySaved.add(history);
                } while (cursor.moveToNext());
            }
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Method that saves the history to the database.
     *
     * @param historyItem
     * @return boolean
     */
    private boolean addHistory(HistoryNavigable historyItem) {
        ContentValues values = new ContentValues(2);
        values.put(History.Columns.TITLE, historyItem.getTitle());
        values.put(History.Columns.DESCRIPTION, historyItem.getDescription());

        final Uri uri = getContext().getContentResolver()
                .insert(History.Columns.CONTENT_URI, values);
        if ((int) ContentUris.parseId(uri) == -1) {
            if (DEBUG) {
                Log.e(TAG, "Error inserting the navigation history");
            }
            return false;
        }

        return true;
    }

    /**
     * Method that clears the history database.
     */
    private void deleteAllHistorys() {
        getContext().getContentResolver().delete(History.Columns.CONTENT_URI, "", null);
    }

    /**
     * Method that decides if the history item should be saved to database.
     *
     * @param historyItem the history item to be saved to database
     * @return boolean
     */
    private boolean shouldAddHistory(HistoryNavigable historyItem) {
        final String description = historyItem.getDescription();
        if (description == null) {
            return false;
        }

        for (History history : mHistorySaved) {
            String desc = history.getItem().getDescription();
            if (desc != null && desc.equals(description)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method that initializes the navigation views of the activity
     */
    private void initNavigationViews() {
        //Get the navigation views (wishlist: multiple view; for now only one view)
        this.mNavigationViews = new NavigationView[1];
        this.mCurrentNavigationView = 0;
        //- 0
        this.mNavigationViews[0] = (NavigationView) mView.findViewById(R.id.navigation_view);
        this.mNavigationViews[0].setId(0);
    }

    /**
     * Method that initialize the console
     * @hide
     */
    void initConsole() {
        //Create the default console (from the preferences)
        try {
            Console console = ConsoleBuilder.getConsole(getActivity());
            if (console == null) {
                throw new ConsoleAllocException("console == null"); //$NON-NLS-1$
            }
        } catch (Throwable ex) {
            if (!NavigationFragment.this.mChRooted) {
                //Show exception and exit
                Log.e(TAG, getString(R.string.msgs_cant_create_console), ex);
                // We don't have any console
                // Show exception and exit
                DialogHelper.showToast(
                        getActivity(),
                        R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                exit();
                return;
            }

            // We are in a trouble (something is not allowing creating the console)
            // Ask the user to return to prompt or root access mode mode with a
            // non-privileged console, prior to make crash the application
            askOrExit();
            return;
        }
    }

    /**
     * Method that initializes the navigation.
     *
     * @param viewId The navigation view identifier where apply the navigation
     * @param restore Initialize from a restore info
     * @param intent The current intent
     * @hide
     */
    void initNavigation(final int viewId, final boolean restore, final Intent intent) {
        final NavigationView navigationView = getNavigationView(viewId);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                //Is necessary navigate?
                if (!restore) {
                    applyInitialDir(navigationView, intent);
                }
            }
        });
    }

    /**
     * Method that applies the user-defined initial directory
     *
     * @param navigationView The navigation view
     * @param intent The current intent
     * @hide
     */
    void applyInitialDir(final NavigationView navigationView, final Intent intent) {
        //Load the user-defined initial directory
        String initialDir =
                Preferences.getSharedPreferences().getString(
                    FileManagerSettings.SETTINGS_INITIAL_DIR.getId(),
                    (String)FileManagerSettings.
                        SETTINGS_INITIAL_DIR.getDefaultValue());

        // Check if request navigation to directory (use as default), and
        // ensure chrooted and absolute path
        String navigateTo = intent.getStringExtra(EXTRA_NAVIGATE_TO);
        if (navigateTo != null && navigateTo.length() > 0) {
            initialDir = navigateTo;
        }

        // Add to history
        final boolean addToHistory = intent.getBooleanExtra(EXTRA_ADD_TO_HISTORY, true);

        // We cannot navigate to a secure console if it is unmounted. So go to root in that case
        VirtualConsole vc = VirtualMountPointConsole.getVirtualConsoleForPath(initialDir);
        if (vc != null && vc instanceof SecureConsole && !((SecureConsole) vc).isMounted()) {
            initialDir = FileHelper.ROOT_DIRECTORY;
        }

        if (this.mChRooted) {
            // Initial directory is the first external sdcard (sdcard, emmc, usb, ...)
            if (!StorageHelper.isPathInStorageVolume(initialDir)) {
                StorageVolume[] volumes =
                        StorageHelper.getStorageVolumes(getActivity(), false);
                if (volumes != null && volumes.length > 0) {
                    initialDir = volumes[0].getPath();
                    //Ensure that initial directory is an absolute directory
                    initialDir = FileHelper.getAbsPath(initialDir);
                } else {
                    // Show exception and exit
                    DialogHelper.showToast(
                            getActivity(),
                            R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                    exit();
                    return;
                }
            }
        } else {
            //Ensure that initial directory is an absolute directory
            final String userInitialDir = initialDir;
            initialDir = FileHelper.getAbsPath(initialDir);
            final String absInitialDir = initialDir;
            File f = new File(initialDir);
            boolean exists = f.exists();
            if (!exists) {
                // Fix for /data/media/0. Libcore doesn't detect it correctly.
                try {
                    exists = CommandHelper.getFileInfo(getActivity(),
                            initialDir, false, null) != null;
                } catch (InsufficientPermissionsException ipex) {
                    ExceptionUtil.translateException(
                            getActivity(), ipex, false, true,
                            new ExceptionUtil.OnRelaunchCommandResult() {
                                @Override
                                public void onSuccess() {
                                    navigationView.changeCurrentDir(absInitialDir, addToHistory);
                                }
                                @Override
                                public void onFailed(Throwable cause) {
                                    showInitialInvalidDirectoryMsg(userInitialDir);
                                    navigationView.changeCurrentDir(
                                            FileHelper.ROOT_DIRECTORY,
                                            addToHistory);
                                }
                                @Override
                                public void onCancelled() {
                                    showInitialInvalidDirectoryMsg(userInitialDir);
                                    navigationView.changeCurrentDir(
                                            FileHelper.ROOT_DIRECTORY,
                                            addToHistory);
                                }
                            });

                    // Asynchronous mode
                    return;
                } catch (Exception ex) {
                    // We are not interested in other exceptions
                    ExceptionUtil.translateException(getActivity(), ex, true, false);
                }

                // Check again the initial directory
                if (!exists) {
                    showInitialInvalidDirectoryMsg(userInitialDir);
                    initialDir = FileHelper.ROOT_DIRECTORY;
                }

                // Weird, but we have a valid initial directory
            }
        }

        // Change the current directory to the user-defined initial directory
        navigationView.changeCurrentDir(initialDir, addToHistory);
    }

    /**
     * Displays a message reporting invalid directory
     *
     * @param initialDir The initial directory
     * @hide
     */
    void showInitialInvalidDirectoryMsg(String initialDir) {
        // Change to root directory
        DialogHelper.showToast(
                getActivity(),
                getString(
                        R.string.msgs_settings_invalid_initial_directory,
                        initialDir),
                Toast.LENGTH_SHORT);
    }

    /**
     * Method that verifies the intent passed to the activity, and checks
     * if a request is made like Search.
     *
     * @param intent The intent to check
     * @hide
     */
    void checkIntent(Intent intent) {
        //Search action
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
            searchIntent.setAction(Intent.ACTION_SEARCH);
            //- SearchActivity.EXTRA_SEARCH_DIRECTORY
            searchIntent.putExtra(
                    SearchActivity.EXTRA_SEARCH_DIRECTORY,
                    getCurrentNavigationView().getCurrentDir());
            //- SearchManager.APP_DATA
            if (intent.getBundleExtra(SearchManager.APP_DATA) != null) {
                Bundle bundle = new Bundle();
                bundle.putAll(intent.getBundleExtra(SearchManager.APP_DATA));
                searchIntent.putExtra(SearchManager.APP_DATA, bundle);
            }
            //-- SearchManager.QUERY
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null) {
                searchIntent.putExtra(SearchManager.QUERY, query);
            }
            //- android.speech.RecognizerIntent.EXTRA_RESULTS
            ArrayList<String> extraResults =
                    intent.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (extraResults != null) {
                searchIntent.putStringArrayListExtra(
                        android.speech.RecognizerIntent.EXTRA_RESULTS, extraResults);
            }
            startActivityForResult(searchIntent, INTENT_REQUEST_SEARCH);
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_REQUEST_SETTINGS) {
            // reset bookmarks list to default as the user could changed the
            // root mode which changes the system bookmarks
            initBookmarks();
            return;
        }

        if (data != null) {
            switch (requestCode) {
                case INTENT_REQUEST_SEARCH:
                    if (resultCode == getActivity().RESULT_OK) {
                        //Change directory?
                        Bundle bundle = data.getExtras();
                        if (bundle != null) {
                            FileSystemObject fso = (FileSystemObject) bundle.getSerializable(
                                    EXTRA_SEARCH_ENTRY_SELECTION);
                            SearchInfoParcelable searchInfo =
                                    bundle.getParcelable(EXTRA_SEARCH_LAST_SEARCH_DATA);
                            if (fso != null) {
                                //Goto to new directory
                                getCurrentNavigationView().open(fso, searchInfo);
                            }
                        }
                    } else if (resultCode == getActivity().RESULT_CANCELED) {
                        SearchInfoParcelable searchInfo =
                                data.getParcelableExtra(EXTRA_SEARCH_LAST_SEARCH_DATA);
                        if (searchInfo != null && searchInfo.isSuccessNavigation()) {
                            //Navigate to previous history
                            back();
                        } else {
                            // I don't know is the search view was changed, so try to do a refresh
                            // of the navigation view
                            getCurrentNavigationView().refresh(true);
                        }
                    }
                    // reset bookmarks list to default as the user could have set a
                    // new bookmark in the search activity
                    initBookmarks();
                    break;

                // Paste selection
                case INTENT_REQUEST_COPY:
                    if (resultCode == Activity.RESULT_OK) {
                        Bundle extras = data.getExtras();
                        String destination = extras.getString(EXTRA_FOLDER_PATH);
                        List<FileSystemObject> selection =
                                getCurrentNavigationView().onRequestSelectedFiles();
                        if (!TextUtils.isEmpty(destination)) {
                            CopyMoveActionPolicy.copyFileSystemObjects(
                                    getActivity(),
                                    selection,
                                    destination,
                                    getCurrentNavigationView(),
                                    this);
                        }
                    }
                    break;

                // Move selection
                case INTENT_REQUEST_MOVE:
                    if (resultCode == Activity.RESULT_OK) {
                        Bundle extras = data.getExtras();
                        String destination = extras.getString(EXTRA_FOLDER_PATH);
                        List<FileSystemObject> selection =
                                getCurrentNavigationView().onRequestSelectedFiles();
                        if (!TextUtils.isEmpty(destination)) {
                            CopyMoveActionPolicy.moveFileSystemObjects(
                                    getActivity(),
                                    selection,
                                    destination,
                                    getCurrentNavigationView(),
                                    this);
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewHistory(HistoryNavigable navigable) {
        //Recollect information about current status
        History history = new History(this.mHistory.size(), navigable);
        this.mHistory.add(history);
        if (!shouldAddHistory(navigable)) {
            return;
        }
        // Show history in the navigation drawer
        addHistoryToDrawer(this.mHistory.size() - 1, navigable);
        mHistorySaved.add(history);
        // Add history to the database
        addHistory(navigable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCheckHistory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRefresh(Object o, boolean clearSelection) {
        if (o instanceof FileSystemObject) {
            // Refresh only the item
            this.getCurrentNavigationView().refresh((FileSystemObject)o);
        } else if (o == null) {
            // Refresh all
            getCurrentNavigationView().refresh();
        }
        if (clearSelection) {
            this.getCurrentNavigationView().onDeselectAll();
        }
    }

    @Override
    public void onClearCache(Object o) {
        getCurrentNavigationView().onClearCache(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestBookmarksRefresh() {
        initBookmarks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestRemove(Object o, boolean clearSelection) {
        if (o instanceof FileSystemObject) {
            // Remove from view
            this.getCurrentNavigationView().removeItem((FileSystemObject)o);

            //Remove from history
            removeFromHistory((FileSystemObject)o);
        } else {
            onRequestRefresh(null, clearSelection);
        }
        if (clearSelection) {
            this.getCurrentNavigationView().onDeselectAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNavigateTo(Object o) {
        // Ignored
    }

    @Override
    public void onCancel(){
        // nop
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSelectionChanged(NavigationView navView, List<FileSystemObject> selectedItems) {
        this.mSelectionBar.setSelection(selectedItems);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestMenu(NavigationView navView, FileSystemObject item) {
        // Show the actions dialog
        openActionsDialog(item, false);
    }

    /**
     * Method that shows a popup with a menu associated a {@link FileManagerSettings}.
     *
     * @param anchor The action button that was pressed
     * @param settings The array of settings associated with the action button
     */
    public void showSettingsPopUp(View anchor, List<FileManagerSettings> settings) {
        //Create the adapter
        final MenuSettingsAdapter adapter = new MenuSettingsAdapter(getActivity(), settings);

        //Create a show the popup menu
        mPopupWindow = DialogHelper.createListPopupWindow(getActivity(), adapter, anchor);
        mPopupWindow.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                FileManagerSettings setting =
                        ((MenuSettingsAdapter)parent.getAdapter()).getSetting(position);
                final int value = ((MenuSettingsAdapter)parent.getAdapter()).getId(position);
                mPopupWindow.dismiss();
                mPopupWindow = null;
                try {
                    if (setting.compareTo(FileManagerSettings.SETTINGS_LAYOUT_MODE) == 0) {
                        //Need to change the layout
                        getCurrentNavigationView().changeViewMode(
                                NavigationLayoutMode.fromId(value));
                    } else {
                        //Save and refresh
                        if (setting.getDefaultValue() instanceof Enum<?>) {
                            //Enumeration
                            Preferences.savePreference(setting, new ObjectIdentifier() {
                                @Override
                                public int getId() {
                                    return value;
                                }
                            }, false);
                        } else {
                            //Boolean
                            boolean newval =
                                    Preferences.getSharedPreferences().
                                        getBoolean(
                                            setting.getId(),
                                            ((Boolean)setting.getDefaultValue()).booleanValue());
                            Preferences.savePreference(setting, Boolean.valueOf(!newval), false);
                        }
                        getCurrentNavigationView().refresh();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error applying navigation option", e); //$NON-NLS-1$
                    NavigationFragment.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            DialogHelper.showToast(
                                    getActivity(),
                                    R.string.msgs_settings_save_failure, Toast.LENGTH_SHORT);
                        }
                    });

                } finally {
                    adapter.dispose();
                    getCurrentNavigationView().getCustomTitle().restoreView();
                }

            }
        });
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                adapter.dispose();
            }
        });
        mPopupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopupWindow.show();
    }

    /**
     * Method that show the information of a filesystem mount point.
     *
     * @param mp The mount point info
     * @param du The disk usage of the mount point
     */
    public void showMountPointInfo(MountPoint mp, DiskUsage du) {
        //Has mount point info?
        if (mp == null) {
            //There is no information
            AlertDialog alert =
                    DialogHelper.createWarningDialog(
                            getActivity(),
                            R.string.filesystem_info_warning_title,
                            R.string.filesystem_info_warning_msg);
            DialogHelper.delegateDialogShow(getActivity(), alert);
            return;
        }

        //Show a the filesystem info dialog
        FilesystemInfoDialog dialog = new FilesystemInfoDialog(getActivity(), mp, du);
        dialog.setOnMountListener(new OnMountListener() {
            @Override
            public void onRemount(MountPoint mountPoint) {
                //Update the statistics of breadcrumb, only if mount point is the same
                Breadcrumb breadcrumb = getCurrentNavigationView().getBreadcrumb();
                if (breadcrumb.getMountPointInfo().compareTo(mountPoint) == 0) {
                    breadcrumb.updateMountPointInfo();
                }
                if (mountPoint.isSecure()) {
                    // Secure mountpoints only can be unmount, so we need to move the navigation
                    // to a secure storage (do not add to history)
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_ADD_TO_HISTORY, false);
                    initNavigation(NavigationFragment.this.mCurrentNavigationView, false, intent);
                }
            }
        });
        dialog.show();
    }

    /**
     * Method that checks the action that must be realized when the
     * back button is pushed.
     *
     * @return boolean Indicates if the action must be intercepted
     */
    private boolean checkBackAction() {
        // We need a basic structure to check this
        if (getCurrentNavigationView() == null) return false;

        if (mSearchView.getVisibility() == View.VISIBLE) {
            closeSearch();
        }

        //Check if the configuration view is showing. In this case back
        //action must be "close configuration"
        if (getCurrentNavigationView().getCustomTitle().isConfigurationViewShowing()) {
            getCurrentNavigationView().getCustomTitle().restoreView();
            return true;
        }

        //Do back operation over the navigation history
        boolean flag = this.mExitFlag;

        this.mExitFlag = !back();

        // Retrieve if the exit status timeout has expired
        long now = System.currentTimeMillis();
        boolean timeout = (this.mExitBackTimeout == -1 ||
                            (now - this.mExitBackTimeout) > RELEASE_EXIT_CHECK_TIMEOUT);

        //Check if there no history and if the user was advised in the last back action
        if (this.mExitFlag && (this.mExitFlag != flag || timeout)) {
            //Communicate the user that the next time the application will be closed
            this.mExitBackTimeout = System.currentTimeMillis();
            DialogHelper.showToast(getActivity(), R.string.msgs_push_again_to_exit,
                    Toast.LENGTH_SHORT);
        }

        //Back action not applied
        return !this.mExitFlag;
    }

    @Override
    public void startActivity(Intent intent) {
        // check if search intent
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            intent.putExtra(SearchActivity.EXTRA_SEARCH_DIRECTORY,
                    getCurrentNavigationView().getCurrentDir());
        }

        super.startActivity(intent);
    }

    /**
     * Method that returns the history size.
     */
    private void clearHistory() {
        this.mHistory.clear();
        mHistorySaved.clear();
        mDrawerHistory.removeAllViews();
        mDrawerHistoryEmpty.setVisibility(View.VISIBLE);

        // Delete all history items in the database
        deleteAllHistorys();
    }

    /**
     * Method that navigates to the passed history reference.
     *
     * @param history The history reference
     * @param isFromSavedHistory Whether this is called by saved history item
     * @return boolean A problem occurs while navigate
     */
    public synchronized boolean navigateToHistory(
            History history, boolean isFromSavedHistory) {
        try {
            //Gets the history
            final History realHistory;
            if (isFromSavedHistory) {
                realHistory = mHistorySaved.get(history.getPosition());
            } else {
                realHistory = mHistory.get(history.getPosition());
            }

            //Navigate to item. Check what kind of history is
            if (realHistory.getItem() instanceof NavigationViewInfoParcelable) {
                //Navigation
                NavigationViewInfoParcelable info =
                        (NavigationViewInfoParcelable)realHistory.getItem();
                int viewId = info.getId();
                NavigationView view = getNavigationView(viewId);
                // Selected items must not be restored from on history navigation
                info.setSelectedFiles(view.getSelectedFiles());
                if (!view.onRestoreState(info)) {
                    return true;
                }

            } else if (realHistory.getItem() instanceof SearchInfoParcelable) {
                //Search (open search with the search results)
                SearchInfoParcelable info = (SearchInfoParcelable)realHistory.getItem();
                Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
                searchIntent.setAction(SearchActivity.ACTION_RESTORE);
                searchIntent.putExtra(SearchActivity.EXTRA_SEARCH_RESTORE, (Parcelable)info);
                startActivityForResult(searchIntent, INTENT_REQUEST_SEARCH);
            } else if (realHistory.getItem() instanceof HistoryItem) {
                final String path = realHistory.getItem().getDescription();
                final FileSystemObject fso = CommandHelper.getFileInfo(
                        getActivity().getApplicationContext(), path, null);
                if (fso != null) {
                    getCurrentNavigationView().open(fso);
                }
            } else {
                //The type is unknown
                throw new IllegalArgumentException("Unknown history type"); //$NON-NLS-1$
            }

            //Remove the old history
            int cc = realHistory.getPosition();
            for (int i = this.mHistory.size() - 1; i >= cc; i--) {
                this.mHistory.remove(i);
            }

            if (mDrawerHistory.getChildCount() == 0) {
                mDrawerHistoryEmpty.setVisibility(View.VISIBLE);
            }

            //Navigate
            final boolean clearHistory =
                    mHistoryTab.isSelected() && mHistorySaved.size() > 0;
            mClearHistory.setVisibility(clearHistory ? View.VISIBLE : View.GONE);
            return true;

        } catch (Throwable ex) {
            if (history != null) {
                Log.e(TAG,
                        String.format("Failed to navigate to history %d: %s", //$NON-NLS-1$
                                Integer.valueOf(history.getPosition()),
                                history.getItem().getTitle()), ex);
            } else {
                Log.e(TAG,
                        String.format("Failed to navigate to history: null", ex)); //$NON-NLS-1$
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DialogHelper.showToast(
                            getActivity(),
                            R.string.msgs_history_unknown, Toast.LENGTH_LONG);
                }
            });

            //Not change directory
            return false;
        }
    }

    /**
     * Method that request a back action over the navigation history.
     *
     * @return boolean If a back action was applied
     */
    public boolean back() {
        // Check that has valid history
        while (this.mHistory.size() > 0) {
            History h = this.mHistory.get(this.mHistory.size() - 1);
            if (h.getItem() instanceof NavigationViewInfoParcelable) {
                // Verify that the path exists
                String path = ((NavigationViewInfoParcelable)h.getItem()).getCurrentDir();

                try {
                    FileSystemObject info = CommandHelper.getFileInfo(getActivity(), path, null);
                    if (info != null) {
                        break;
                    }
                    this.mHistory.remove(this.mHistory.size() - 1);
                } catch (Exception e) {
                    ExceptionUtil.translateException(getActivity(), e, true, false);
                    this.mHistory.remove(this.mHistory.size() - 1);
                }
            } else {
                break;
            }
        }

        //Navigate to history
        if (this.mHistory.size() > 0) {
            return navigateToHistory(mHistory.get(mHistory.size() - 1), false);
        }

        //Nothing to apply
        mClearHistory.setVisibility(View.GONE);
        return false;
    }

    public void openActionsDialog(String path, boolean global) {
        FileSystemObject fso = null;
        try {
            fso = CommandHelper.getFileInfo(getActivity(), path, false, null);
            if (fso == null) {
                throw new NoSuchFileOrDirectory(path);
            }
            openActionsDialog(fso, global);
        } catch (Exception e) {
            // Notify the user
            ExceptionUtil.translateException(getActivity(), e);

            // Remove the object
            if (e instanceof FileNotFoundException || e instanceof NoSuchFileOrDirectory) {
                // If have a FileSystemObject reference then there is no need to search
                // the path (less resources used)
                getCurrentNavigationView().removeItem(path);
            }
            return;
        }
    }

    /**
     * Method that opens the actions dialog
     *
     * @param item The path or the {@link FileSystemObject}
     * @param global If the menu to display is the one with global actions
     */
    private void openActionsDialog(FileSystemObject item, boolean global) {
        // We used to refresh the item reference here, but the access to the SecureConsole is synchronized,
        // which can/will cause on ANR in certain scenarios.  We don't care if it doesn't exist anymore really
        // For this to work, SecureConsole NEEDS to be refactored.

        // Show the dialog
        if (mActionsDialog != null && mActionsDialog.isShowing()) {
            return;
        }
        mActionsDialog = new ActionsDialog(getActivity(), this, item, global, false);
        mActionsDialog.setOnRequestRefreshListener(this);
        mActionsDialog.setOnSelectionListener(getCurrentNavigationView());
        mActionsDialog.show();
    }

    /**
     * Method that opens the search view.
     *
     * @hide
     */
    public void openSearch() {
        mSearchView.setVisibility(View.VISIBLE);
        mSearchView.onActionViewExpanded();
        mCustomTitleView.setVisibility(View.GONE);
    }

    void closeSearch() {
        mSearchView.setVisibility(View.GONE);
        mSearchView.onActionViewCollapsed();
        mCustomTitleView.setVisibility(View.VISIBLE);
    }

    /**
     * Method that opens the settings activity.
     *
     * @hide
     */
    void openSettings() {
        Intent settingsIntent = new Intent(getActivity(),
                SettingsPreferences.class);
        startActivityForResult(settingsIntent, INTENT_REQUEST_SETTINGS);
    }

    /**
     * Method that remove the {@link FileSystemObject} from the history
     */
    private void removeFromHistory(FileSystemObject fso) {
        if (this.mHistory != null) {
            int cc = this.mHistory.size() - 1;
            for (int i = cc; i >= 0 ; i--) {
                History history = this.mHistory.get(i);
                if (history.getItem() instanceof NavigationViewInfoParcelable) {
                    String p0 = fso.getFullPath();
                    String p1 = ((NavigationViewInfoParcelable) history.getItem()).getCurrentDir();
                    if (p0.compareTo(p1) == 0) {
                        this.mHistory.remove(i);
                        mDrawerHistory.removeViewAt(mDrawerHistory.getChildCount() - i - 1);
                        mDrawerHistoryEmpty.setVisibility(
                                mDrawerHistory.getChildCount() == 0 ? View.VISIBLE : View.GONE);
                        updateHistoryPositions();
                    }
                }
            }
        }
    }

    /**
     * Update the history positions after one of the history is removed from drawer
     */
    private void updateHistoryPositions() {
        int cc = this.mHistory.size() - 1;
        for (int i = 0; i <= cc ; i++) {
            History history = this.mHistory.get(i);
            history.setPosition(i + 1);
        }
    }

    /**
     * Method that ask the user to change the access mode prior to crash.
     * @hide
     */
    void askOrExit() {
        //Show a dialog asking the user
        AlertDialog dialog =
            DialogHelper.createYesNoDialog(
                getActivity(),
                R.string.msgs_change_to_prompt_access_mode_title,
                R.string.msgs_change_to_prompt_access_mode_msg,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface alertDialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            // We don't have any console
                            // Show exception and exit
                            DialogHelper.showToast(
                                    getActivity(),
                                    R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                            exit();
                            return;
                        }

                        // Ok. Now try to change to prompt mode. Any crash
                        // here is a fatal error. We won't have any console to operate.
                        try {
                            // Change console
                            ConsoleBuilder.changeToNonPrivilegedConsole(getActivity());

                            // Save preferences
                            Preferences.savePreference(
                                    FileManagerSettings.SETTINGS_ACCESS_MODE,
                                    AccessMode.PROMPT, true);

                        } catch (Exception e) {
                            // Displays an exception and exit
                            Log.e(TAG, getString(R.string.msgs_cant_create_console), e);
                            DialogHelper.showToast(
                                    getActivity(),
                                    R.string.msgs_cant_create_console, Toast.LENGTH_LONG);
                            exit();
                        }
                    }
                });
        DialogHelper.delegateDialogShow(getActivity(), dialog);
    }

    /**
     * Method that creates a ChRooted environment, protecting the user to break anything in
     * the device
     * @hide
     */
    void createChRooted() {
        // If we are in a ChRooted mode, then do nothing
        if (this.mChRooted) return;
        this.mChRooted = true;

        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            this.mNavigationViews[i].createChRooted();
        }

        // Remove the selection
        cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            getCurrentNavigationView().onDeselectAll();
        }

        // Remove the history (don't allow to access to previous data)
        clearHistory();
    }

    /**
     * Method that exits from a ChRooted
     * @hide
     */
    void exitChRooted() {
        // If we aren't in a ChRooted mode, then do nothing
        if (!this.mChRooted) return;
        this.mChRooted = false;

        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            this.mNavigationViews[i].exitChRooted();
        }
    }

    /**
     * Method called when a controlled exit is required
     * @hide
     */
    void exit() {
        getActivity().finish();
    }

    private void recycle() {
        // Recycle the navigation views
        if (mNavigationViews != null) {
            int cc = this.mNavigationViews.length;
            for (int i = 0; i < cc; i++) {
                this.mNavigationViews[i].recycle();
            }
        }
        try {
            FileManagerApplication.destroyBackgroundConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        try {
            ConsoleBuilder.destroyConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
    }

    /**
     * Method that reconfigures the layout for better fit in portrait and landscape modes
     */
    private void onLayoutChanged() {
        // Apply only when the orientation was changed
        int orientation = getResources().getConfiguration().orientation;
        if (this.mOrientation == orientation) return;
        this.mOrientation = orientation;

        // Portrait mode
        if (mStatusBar != null) {
            if (mStatusBar.getParent() != null) {
                ViewGroup parent = (ViewGroup) mStatusBar.getParent();
                parent.removeView(mStatusBar);
            }
            if (this.mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Calculate the action button size (all the buttons must fit in the title bar)
                int bw = (int)getResources().getDimension(R.dimen.default_buttom_width);
                int abw = this.mActionBar.getChildCount() * bw;
                int rbw = 0;
                int cc = ((ViewGroup) mStatusBar).getChildCount();
                for (int i = 0; i < cc; i++) {
                    View child = ((ViewGroup) mStatusBar).getChildAt(i);
                    if (child instanceof ButtonItem) {
                        rbw += bw;
                    }
                }
                // Currently there isn't overflow menu
                int w = abw + rbw - bw;

                // Add to the new location
                ViewGroup newParent = (ViewGroup)mTitleLayout.findViewById(
                        R.id.navigation_title_landscape_holder);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                w,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                mStatusBar.setLayoutParams(params);
                newParent.addView(mStatusBar);

                // Apply theme
                mStatusBar.setBackgroundResource(R.drawable.titlebar_drawable);

                // Hide holder
                View holder = mView.findViewById(
                        R.id.navigation_statusbar_portrait_holder);
                holder.setVisibility(View.GONE);

            } else {
                // Add to the new location
                ViewGroup newParent = (ViewGroup) mView.findViewById(
                        R.id.navigation_statusbar_portrait_holder);
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                mStatusBar.setLayoutParams(params);
                newParent.addView(mStatusBar);

                // Apply theme
                mStatusBar.setBackgroundResource(R.drawable.statusbar_drawable);

                // Show holder
                newParent.setVisibility(View.VISIBLE);
            }
        }

    }

    /**
     * Method that removes all the history items that refers to virtual unmounted filesystems
     */
    private void removeUnmountedHistory() {
        int cc = mHistory.size() - 1;
        for (int i = cc; i >= 0; i--) {
            History history = mHistory.get(i);
            if (history.getItem() instanceof NavigationViewInfoParcelable) {
                NavigationViewInfoParcelable navigableInfo =
                        ((NavigationViewInfoParcelable) history.getItem());
                VirtualMountPointConsole vc =
                        VirtualMountPointConsole.getVirtualConsoleForPath(
                                navigableInfo.getCurrentDir());
                if (vc != null && !vc.isMounted()) {
                    mHistory.remove(i);
                }
            }
        }
        updateHistoryPositions();
    }

    /**
     * Method that removes all the selection items that refers to virtual unmounted filesystems
     */
    private void removeUnmountedSelection() {
        for (NavigationView view : mNavigationViews) {
            view.removeUnmountedSelection();
        }
        mSelectionBar.setSelection(getNavigationView(mCurrentNavigationView).getSelectedFiles());
    }

    /**
     * Method that applies the current theme to the activity
     * @hide
     */
    void applyTheme() {
        int orientation = getResources().getConfiguration().orientation;
        Theme theme = ThemeManager.getCurrentTheme(getActivity());
        applyTabTheme();

        //- Layout
        View navLayout = mView.findViewById(R.id.navigation_layout);
        navLayout.setBackgroundResource(R.drawable.background_drawable);

        // Hackery to theme search view
        mSearchView = (SearchView) mTitleLayout.findViewById(R.id.navigation_search_bar);
        int searchPlateId = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = mSearchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            int searchTextId = searchPlate.getContext().getResources()
                    .getIdentifier("android:id/search_src_text", null, null);
            TextView searchText = (TextView) searchPlate.findViewById(searchTextId);
            if (searchText != null) {
                searchText.setTextColor(Color.WHITE);
                searchText.setHintTextColor(Color.WHITE);
            }

            int magId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
            ImageView magImage = (ImageView) mSearchView.findViewById(magId);
            if (magImage != null) {
                magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            }
        }

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context
                .SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity()
                .getComponentName()));
        mSearchView.setIconifiedByDefault(false);

        mCustomTitleView = (NavigationCustomTitleView) mTitleLayout.findViewById(
                R.id.navigation_title_flipper);
        mCustomTitleView.setVisibility(View.VISIBLE);

        //- StatusBar
        mStatusBar = mView.findViewById(R.id.navigation_statusbar);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mStatusBar.setBackgroundResource(R.drawable.titlebar_drawable);
        } else {
            mStatusBar.setBackgroundResource(R.drawable.statusbar_drawable);
        }
        View v = mView.findViewById(R.id.ab_overflow);
        theme.setImageDrawable(getActivity(), (ImageView)v, "ab_overflow_drawable"); //$NON-NLS-1$
        v = mView.findViewById(R.id.ab_actions);
        theme.setImageDrawable(getActivity(), (ImageView)v, "ab_actions_drawable"); //$NON-NLS-1$
        v = mView.findViewById(R.id.ab_search);
        theme.setImageDrawable(getActivity(), (ImageView)v, "ab_search_drawable"); //$NON-NLS-1$

        //- Expanders
        v = mTitleLayout.findViewById(R.id.ab_configuration);
        theme.setImageDrawable(getActivity(), (ImageView)v, "expander_open_drawable"); //$NON-NLS-1$
        v = mTitleLayout.findViewById(R.id.ab_close);
        theme.setImageDrawable(getActivity(),
                (ImageView)v, "expander_close_drawable"); //$NON-NLS-1$
        v = mTitleLayout.findViewById(R.id.ab_sort_mode);
        theme.setImageDrawable(getActivity(), (ImageView)v, "ab_sort_mode_drawable"); //$NON-NLS-1$
        v = mTitleLayout.findViewById(R.id.ab_layout_mode);
        theme.setImageDrawable(getActivity(),
                (ImageView)v, "ab_layout_mode_drawable"); //$NON-NLS-1$
        v = mTitleLayout.findViewById(R.id.ab_view_options);
        theme.setImageDrawable(getActivity(),
                (ImageView)v, "ab_view_options_drawable"); //$NON-NLS-1$

        //- SelectionBar
        v = mView.findViewById(R.id.navigation_selectionbar);
        theme.setBackgroundDrawable(getActivity(), v, "selectionbar_drawable"); //$NON-NLS-1$
        v = mView.findViewById(R.id.ab_selection_done);
        theme.setImageDrawable(getActivity(),
                (ImageView)v, "ab_selection_done_drawable"); //$NON-NLS-1$
        v = mView.findViewById(R.id.navigation_status_selection_label);
        theme.setTextColor(getActivity(), (TextView)v, "text_color"); //$NON-NLS-1$

        //- NavigationView
        int cc = this.mNavigationViews.length;
        for (int i = 0; i < cc; i++) {
            getNavigationView(i).applyTheme();
        }
    }

    /**
     * Method that applies the current theme to the tab host
     */
    private void applyTabTheme() {
        // Apply the theme
        Theme theme = ThemeManager.getCurrentTheme(getActivity());
    }

    public void updateActiveDialog(Dialog dialog) {
        mActiveDialog = dialog;
    }

    private class HistoryItem extends HistoryNavigable {
        private final String mTitle;
        private final String mDescription;

        public HistoryItem(String title, String description) {
            mTitle = title;
            mDescription = description;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {}

        public String getTitle() {
            return mTitle;
        }

        public String getDescription() {
            return mDescription;
        }
    }
}