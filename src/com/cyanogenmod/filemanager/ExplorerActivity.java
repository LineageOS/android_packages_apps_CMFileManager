package com.cyanogenmod.filemanager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.dialogactivity.NewFileActivity;
import com.cyanogenmod.filemanager.taskfragment.PasteFragment;
import com.cyanogenmod.filemanager.taskfragment.SearchFragment;
import com.cyanogenmod.filemanager.taskfragment.SortFragment;
import com.cyanogenmod.filemanager.taskfragment.ZipFragment;
import com.cyanogenmod.filemanager.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ExplorerActivity extends Activity {

    public static final String PREF_SORT_BY = "PREF_SORT_BY";
    public static final int SORT_BY_NAME_FOLDERS_FIRST = 0;
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_EXTENSION = 2;
    public static final int SORT_BY_SIZE_LOW = 3;
    public static final int SORT_BY_SIZE_HIGH = 4;
    public static final int SORT_BY_LAST_MODIFIED = 5;

    private static final String KEY_IS_IN_SEARCH_MODE = "KEY_IS_IN_SEARCH_MODE";
    private static final String KEY_STORED_QUERY = "KEY_STORED_QUERY";
    private static final String KEY_CACHE_MAP = "KEY_CACHE_MAP";
    private static final String EXPLORER_FRAGMENT_KEY = "EXPLORER_FRAGMENT_KEY";
    private static final String FILE_HISTORY_LIST_KEY = "FILE_HISTORY_LIST_KEY";
    private static final String KEY_POSITION_LIST = "KEY_POSITION_LIST";
    private static final String TAG_ZIP_FRAGMENT = "TAG_ZIP_FRAGMENT";
    private static final String KEY_ZIP_FRAGMENT = "KEY_ZIP_FRAGMENT";
    private static final String TAG_PASTE_FRAGMENT = "TAG_PASTE_FRAGMENT";
    private static final String KEY_PASTE_FRAGMENT = "KEY_PASTE_FRAGMENT";
    private static final String TAG_SORT_FRAGMENT = "TAG_SORT_FRAGMENT";
    private static final String KEY_SORT_FRAGMENT = "KEY_SORT_FRAGMENT";
    private static final String TAG_SEARCH_FRAGMENT = "TAG_SEARCH_FRAGMENT";
    private static final String KEY_SEARCH_FRAGMENT = "KEY_SEARCH_FRAGMENT";
    private static final int NEW_FILE_REQUEST_CODE = 1;

    private boolean mIsInSearchMode;
    private CharSequence mStoredQuery;
    private MenuItem mSearchItem;
    private int mShortAnimationDuration;
    private Rect mStartBounds;
    private float mStartScaleFinal;
    private Animator mCurrentAnimator;
    private SearchView mSearchView;
    private ImageView mExpandedImageView;
    private HorizontalScrollView mTitleScrollView;
    private ViewGroup mTitleView;
    private ExplorerFragment mFragment;
    private Fragment mZipFragment;
    private Fragment mPasteFragment;
    private SortFragment mSortFragment;
    private Fragment mSearchFragment;
    private ArrayList<String> mFileHistory;
    private ArrayList<Integer> mPositionHistory;
    private HashMap<File, ArrayList<FileItem>> mSubFileMap;
    private SharedPreferences mPrefs;
    private int mSortBy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        setResult(Activity.RESULT_CANCELED);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mExpandedImageView = (ImageView) findViewById(
                R.id.expanded_image);
        mTitleScrollView = (HorizontalScrollView)
                LayoutInflater.from(this).inflate(R.layout.title, null);
        mTitleView = (ViewGroup) mTitleScrollView.findViewById(R.id.title);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowCustomEnabled(true);
        getActionBar().setCustomView(mTitleScrollView);
        if (savedInstanceState == null) {
            mIsInSearchMode = false;
            mFileHistory = new ArrayList<String>();
            mPositionHistory = new ArrayList<Integer>();
            mSubFileMap = new HashMap<File, ArrayList<FileItem>>();
            mFileHistory.add(Environment.getExternalStorageDirectory().getPath());
            mFragment = new ExplorerFragment();
            final Bundle bundle = new Bundle();
            bundle.putString(ExplorerFragment.FILE_PATH_ARG, getCurrentFilePath());
            mFragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFragment)
                    .commit();
        } else {
            mIsInSearchMode = savedInstanceState.getBoolean(KEY_IS_IN_SEARCH_MODE);
            mStoredQuery = savedInstanceState.getCharSequence(KEY_STORED_QUERY);
            mFragment = (ExplorerFragment) getFragmentManager()
                    .getFragment(savedInstanceState, EXPLORER_FRAGMENT_KEY);
            mFileHistory = savedInstanceState.getStringArrayList(FILE_HISTORY_LIST_KEY);
            mPositionHistory = savedInstanceState.getIntegerArrayList(KEY_POSITION_LIST);
            mSubFileMap = (HashMap<File, ArrayList<FileItem>>)
                    savedInstanceState.getSerializable(KEY_CACHE_MAP);
            mZipFragment = getFragmentManager()
                    .getFragment(savedInstanceState, KEY_ZIP_FRAGMENT);
            mPasteFragment = getFragmentManager()
                    .getFragment(savedInstanceState, KEY_PASTE_FRAGMENT);
            mSortFragment = (SortFragment) getFragmentManager()
                    .getFragment(savedInstanceState, KEY_SORT_FRAGMENT);
            mSearchFragment = getFragmentManager()
                    .getFragment(savedInstanceState, KEY_SEARCH_FRAGMENT);
        }
        setPathTitle();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSearchIntent();
    }

    public void setCurrentFile(final File file, final int previousPosition) {
        if (requestRootIfNeeded(file)) {
            mSearchItem.collapseActionView(); // will exit search mode
            mPositionHistory.add(previousPosition);
            mFileHistory.add(file.getPath());
            refreshCurrentFile(0);
        }
    }

    private void refreshCurrentFile(final int position) {
        mFragment.setFile(new File(getCurrentFilePath()), position);
        setPathTitle();
        setSearchHint();
    }

    private void setPathTitle() {
        mTitleView.removeAllViews();
        String path = getCurrentFilePath();
        final String rootPath = Environment.getExternalStorageDirectory().getPath();
        if (path.contains(rootPath)) {
            path = path.replace(rootPath, getString(R.string.sdcard_short_name));
        }
        String[] names = path.split("/");
        List<View> pathViews = new ArrayList<View>(names.length);
        for (String name : names) {
            if (name == null || name.length() <= 0) continue;
            final TextView slashView = new TextView(this);
            final TextView textView = new TextView(this);
            slashView.setText("/");
            textView.setText(name);
            slashView.setTextAppearance(this, R.style.TextAppearance_Title_Path);
            textView.setTextAppearance(this, R.style.TextAppearance_Title_Path);
            mTitleView.addView(slashView);
            mTitleView.addView(textView);
            pathViews.add(textView);
        }
        Collections.reverse(pathViews);
        File file = new File(getCurrentFilePath());
        for (View view : pathViews) {
            view.setTag(file);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (shouldCloseZoomedImage()) {
                        closeZoomedView();
                        return;
                    }
                    final File file = (File) view.getTag();
                    setCurrentFile(file, mFragment.getCurrentPosition());
                }
            });
            file = file.getParentFile();
        }
        mTitleScrollView.post(new Runnable() {
            @Override
            public void run() {
                mTitleScrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    private void setSearchHint() {
        final File file = new File(getCurrentFilePath());
        final String rootPath = Environment.getExternalStorageDirectory().getPath();
        final CharSequence hint;
        if (file.getPath().equals(rootPath)) {
            hint = getString(R.string.sdcard_short_name);
        } else {
            hint = file.getName();
        }
        mSearchView.setQueryHint(getString(R.string.search_hint, hint));
    }

    private void handleSearchIntent() {
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(getCurrentFilePath(), query);
        }
    }

    @Override
    public Intent getIntent() {
        final Intent intent = super.getIntent();
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // Activity launched from history
            return new Intent(Intent.ACTION_MAIN);
        } else {
            return intent;
        }
    }

    @Override
    public void onBackPressed() {
        if (shouldCloseZoomedImage()) {
            closeZoomedView();
        } else if (mIsInSearchMode) { // probably already handled by super, but belt & suspenders...
            mSearchItem.collapseActionView();
        } else if (mFileHistory.size() > 1) {
            mFileHistory.remove(mFileHistory.size() - 1);
            refreshCurrentFile(mPositionHistory.remove(mPositionHistory.size() - 1));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        getFragmentManager().putFragment(outState, EXPLORER_FRAGMENT_KEY, mFragment);
        outState.putStringArrayList(FILE_HISTORY_LIST_KEY, mFileHistory);
        outState.putIntegerArrayList(KEY_POSITION_LIST, mPositionHistory);
        outState.putSerializable(KEY_CACHE_MAP, mSubFileMap);
        outState.putBoolean(KEY_IS_IN_SEARCH_MODE, mIsInSearchMode);
        outState.putCharSequence(KEY_STORED_QUERY, mSearchView.getQuery());
        if (mZipFragment != null) {
            getFragmentManager().putFragment(outState, KEY_ZIP_FRAGMENT, mZipFragment);
        }
        if (mPasteFragment != null) {
            getFragmentManager().putFragment(outState, KEY_PASTE_FRAGMENT, mPasteFragment);
        }
        if (mSortFragment != null) {
            getFragmentManager().putFragment(outState, KEY_SORT_FRAGMENT, mSortFragment);
        }
        if (mSearchFragment != null) {
            getFragmentManager().putFragment(outState, KEY_SEARCH_FRAGMENT, mSearchFragment);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.explorer, menu);
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(true);
        mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                enterSearchMode();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                exitSearchMode();
                return true;
            }
        });
        if (mIsInSearchMode) {
            mSearchItem.expandActionView();
            mSearchView.setQuery(mStoredQuery, false);
        }
        setSearchHint();
        final int menuItemId;
        mSortBy = mPrefs.getInt(PREF_SORT_BY, SORT_BY_NAME_FOLDERS_FIRST);
        switch (mSortBy) {
            case SORT_BY_NAME_FOLDERS_FIRST:
                menuItemId = R.id.name_folder_first;
                break;
            case SORT_BY_NAME:
                menuItemId = R.id.name;
                break;
            case SORT_BY_EXTENSION:
                menuItemId = R.id.extension;
                break;
            case SORT_BY_SIZE_LOW:
                menuItemId = R.id.size_low;
                break;
            case SORT_BY_SIZE_HIGH:
                menuItemId = R.id.size_high;
                break;
            case SORT_BY_LAST_MODIFIED:
                menuItemId = R.id.last_modified;
                break;
            default:
                menuItemId = R.id.name_folder_first;
                break;
        }
        menu.findItem(menuItemId).setChecked(true);
        mFragment.refreshSubFileOrder();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int startingSortBy = mSortBy;
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.name_folder_first:
                mSortBy = SORT_BY_NAME_FOLDERS_FIRST;
                break;
            case R.id.name:
                mSortBy = SORT_BY_NAME;
                break;
            case R.id.extension:
                mSortBy = SORT_BY_EXTENSION;
                break;
            case R.id.size_low:
                mSortBy = SORT_BY_SIZE_LOW;
                break;
            case R.id.size_high:
                mSortBy = SORT_BY_SIZE_HIGH;
                break;
            case R.id.last_modified:
                mSortBy = SORT_BY_LAST_MODIFIED;
                break;
            case R.id.new_file:
                beginNewItem();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        item.setChecked(true);
        if (startingSortBy != mSortBy) {
            mPrefs.edit().putInt(PREF_SORT_BY, mSortBy).apply();
            mSubFileMap.clear();
            mFragment.refreshSubFileOrder();
        }
        return true;
    }

    private void beginNewItem() {
        final Intent intent = new Intent(this, NewFileActivity.class);
        intent.putExtra(NewFileActivity.EXTRA_PARENT_PATH, getCurrentFilePath());
        startActivityForResult(intent, NEW_FILE_REQUEST_CODE);
    }

    @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEW_FILE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mFragment.refreshFile();
            }
        }
    }

    public void doZip(final ArrayList<String> filePaths,
                        final String zipPath) {
        if (mZipFragment != null) {
            throw new RuntimeException("You are already zipping");
        }
        mZipFragment = new ZipFragment();
        final Bundle bundle = new Bundle();
        bundle.putStringArrayList(ZipFragment.ARG_FILE_PATHS, filePaths);
        bundle.putString(ZipFragment.ARG_FINAL_ZIP_PATH, zipPath);
        mZipFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().add(mZipFragment, TAG_ZIP_FRAGMENT).commit();
    }

    public void doCompletedZipping(final boolean result, final String zipPath) {
        mFragment.doCompletedZipping(result, zipPath);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().beginTransaction().remove(mZipFragment)
                        .commitAllowingStateLoss();
                mZipFragment = null;
            }
        });
    }

    public void doPaste(final ArrayList<String> clipboardFilePaths,
                        final boolean cutMode,
                        final File file) {
        if (mPasteFragment != null) {
            throw new RuntimeException("You are already pasting");
        }
        mPasteFragment = new PasteFragment();
        final Bundle bundle = new Bundle();
        bundle.putStringArrayList(PasteFragment.ARG_CLIPBOARD_FILE_PATHS, clipboardFilePaths);
        bundle.putBoolean(PasteFragment.ARG_CUT_MODE, cutMode);
        bundle.putString(PasteFragment.ARG_FILE_PATH, file.getPath());
        mPasteFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().add(mPasteFragment, TAG_PASTE_FRAGMENT).commit();
    }

    public void doCompletedPasting(final boolean result) {
        mFragment.doCompletedPasting(result);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().beginTransaction().remove(mPasteFragment)
                        .commitAllowingStateLoss();
                mPasteFragment = null;
            }
        });
    }

    public void doSort(final ArrayList<FileItem> fileItems) {
        if (mSearchFragment != null) { // we will sort at the end of searching
            return;
        }
        final SortFragment fragment = (SortFragment)
                getFragmentManager().findFragmentByTag(TAG_SORT_FRAGMENT);
        if (fragment != null) {
            fragment.cancelSort();
            getFragmentManager().beginTransaction().remove(fragment).commit();
        }
        mSortFragment = new SortFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(SortFragment.ARG_FILE_ITEMS, fileItems);
        bundle.putInt(SortFragment.ARG_SORT_BY, mSortBy);
        mSortFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().add(mSortFragment, TAG_SORT_FRAGMENT).commit();
    }

    public void doCompletedSorting(final List<Integer> result) {
        mFragment.doCompletedSorting(result);
        mSortFragment = null;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mSortFragment == null) {
                    final Fragment fragment = getFragmentManager().findFragmentByTag(TAG_SORT_FRAGMENT);
                    if (fragment != null) {
                        getFragmentManager().beginTransaction().remove(fragment)
                                .commitAllowingStateLoss();
                    }
                }
            }
        });
    }

    private void enterSearchMode() {
        mIsInSearchMode = true;
    }

    private void exitSearchMode() {
        mIsInSearchMode = false;
        mFragment.clearSubfiles();
        mFragment.refreshFile();
    }

    public boolean isInSearchMode() {
        return mIsInSearchMode;
    }

    private void doSearch(final String filePath, final String query) {
        if (mSearchFragment != null) {
            throw new RuntimeException("You are already searching");
        }
        mSearchFragment = new SearchFragment();
        final Bundle bundle = new Bundle();
        bundle.putString(SearchFragment.ARG_FILE_PATH, filePath);
        bundle.putString(SearchFragment.ARG_QUERY, query);
        mSearchFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().add(mSearchFragment, TAG_SEARCH_FRAGMENT).commit();
    }

    public void doCompletedSearching(final List<File> fileList) {
        final ArrayList<FileItem> foundFileList = FileUtils.getFileItemList(fileList);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().beginTransaction().remove(mSearchFragment)
                        .commitAllowingStateLoss();
                mSearchFragment = null;
                doSort(foundFileList);
                mFragment.doCompletedSearching(foundFileList);
            }
        });
    }

    private String getCurrentFilePath() {
        return mFileHistory.get(mFileHistory.size() - 1);
    }

    public final void cacheSubFiles(final File parentFile, final ArrayList<FileItem> oSubFiles) {
        if (mIsInSearchMode) return; // never cache in search mode
        final ArrayList<FileItem> subFiles = new ArrayList<FileItem>(oSubFiles);
        // clear Drawable cache
        for (FileItem fileItem : subFiles) {
            fileItem.image = null;
        }
        mSubFileMap.put(parentFile, subFiles);
    }

    public final ArrayList<FileItem> getSubFiles(final File parentFile) {
        return mSubFileMap.get(parentFile);
    }

    /*
     * returns whether it is OK to proceed or not
     * TODO
     */
    private boolean requestRootIfNeeded(final File file) {
        final String ex = Environment.getExternalStorageDirectory().getPath();
        final String ac = file.getPath();
        return !((ex.contains(ac) && !ex.equals(ac)) || !ac.startsWith(getString(R.string.root_storage)));
    }

    public void zoomImage(final Drawable drawable, final Rect startBounds) {
        mStartBounds = startBounds;
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        mExpandedImageView.setImageDrawable(drawable);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        //final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        //thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.root)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        mStartBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) mStartBounds.width() / mStartBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) mStartBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - mStartBounds.width()) / 2;
            mStartBounds.left -= deltaWidth;
            mStartBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) mStartBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - mStartBounds.height()) / 2;
            mStartBounds.top -= deltaHeight;
            mStartBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        //thumbView.setAlpha(0f);
        mExpandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        mExpandedImageView.setPivotX(0f);
        mExpandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(mExpandedImageView, View.X,
                        mStartBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y,
                        mStartBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(mExpandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        mStartScaleFinal = startScale;
        mExpandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeZoomedView();
            }
        });
    }

    private void closeZoomedView() {
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator
                .ofFloat(mExpandedImageView, View.X, mStartBounds.left))
                .with(ObjectAnimator
                        .ofFloat(mExpandedImageView,
                                View.Y, mStartBounds.top))
                .with(ObjectAnimator
                        .ofFloat(mExpandedImageView,
                                View.SCALE_X, mStartScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(mExpandedImageView,
                                View.SCALE_Y, mStartScaleFinal));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //thumbView.setAlpha(1f);
                mExpandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //thumbView.setAlpha(1f);
                mExpandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
    }

    private boolean shouldCloseZoomedImage() {
        return mExpandedImageView.getVisibility() == View.VISIBLE;
    }
}
