package com.brianco.fileheaven;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Outline;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.brianco.fileheaven.dialogactivity.DeleteActivity;
import com.brianco.fileheaven.dialogactivity.RenameActivity;
import com.brianco.fileheaven.dialogactivity.ZipActivity;
import com.brianco.fileheaven.util.FileUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ExplorerFragment extends Fragment {

    public static final String FILE_PATH_ARG = "FILE_PATH_ARG";
    private static final String FILE_PATH_KEY = "FILE_ITEMS_KEY";
    private static final String KEY_SUB_FILE_ITEMS = "KEY_SUB_FILE_ITEMS";
    private static final String KEY_CLIPBOARD_FILES = "KEY_CLIPBOARD_FILES";
    private static final String KEY_CUT_MODE = "KEY_CUT_MODE";
    private static final String KEY_CHECKED_POSITIONS = "KEY_CHECKED_POSITIONS";
    private static final int RENAME_REQUEST_CODE = 4;
    private static final int DELETE_REQUEST_CODE = 5;
    private static final int ZIP_REQUEST_CODE = 6;

    private ListView mListView;
    private ImageButton mFabPaste;
    private ExplorerAdapter mAdapter;
    private File mFile = null;
    private ArrayList<FileItem> mSubFiles;
    private ArrayList<String> mClipboardFilePaths = null;
    private boolean mCutMode = false;

    private final View.OnClickListener mBinaryClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final File file = ((ExplorerAdapter.ViewHolder) view.getTag()).file;
            if (Intent.ACTION_GET_CONTENT.equals(getActivity().getIntent().getAction())) {
                final Intent intent = new Intent();
                intent.setData(Uri.fromFile(file));
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
            } else {
                openFile(file, false);
            }
        }
    };

    private final View.OnClickListener mDirectoryClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final File file = ((ExplorerAdapter.ViewHolder) view.getTag()).file;
            ((ExplorerActivity) getActivity()).setCurrentFile(file, mListView.getFirstVisiblePosition());
        }
    };

    private final View.OnClickListener mUpClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            for (int i = 0; i < mAdapter.getCount(); i++) {
                mListView.setItemChecked(i, false);
            }
            final File file = mFile.getParentFile();
            ((ExplorerActivity) getActivity()).setCurrentFile(file, mListView.getFirstVisiblePosition());
        }
    };

    private final View.OnClickListener mItemOverflowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final int position
                    = ((ExplorerAdapter.ViewHolder) ((View) view.getParent()).getTag()).position;
            mListView.setItemChecked(position, !mListView.isItemChecked(position));
        }
    };

    private final View.OnLongClickListener mLongCheckedListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            final int position
                    = ((ExplorerAdapter.ViewHolder) view.getTag()).position;
            mListView.setItemChecked(position, !mListView.isItemChecked(position));
            return true;
        }
    };

    private final View.OnClickListener mCheckedModeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final int position = ((ExplorerAdapter.ViewHolder) view.getTag()).position;
            mListView.setItemChecked(position, !mListView.isItemChecked(position));
        }
    };

    private final View.OnClickListener mFabPasteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            doPaste();
        }
    };

    public ExplorerFragment() {
        mSubFiles = new ArrayList<FileItem>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_explorer, container, false);
        mListView = (ListView) root.findViewById(R.id.list_view);
        mFabPaste = (ImageButton) root.findViewById(R.id.fab_paste);
        //Outline
        final int size = getResources().getDimensionPixelSize(R.dimen.fab_size);
        final Outline outline = new Outline();
        outline.setOval(0, 0, size, size);
        mFabPaste.setOutline(outline);
        return root;
    }

    @Override
     public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mFile = new File(savedInstanceState.getString(FILE_PATH_KEY));
            mSubFiles = savedInstanceState.getParcelableArrayList(KEY_SUB_FILE_ITEMS);
            final ArrayList<String> clipboardFilePaths = savedInstanceState.getStringArrayList(KEY_CLIPBOARD_FILES);
            if (clipboardFilePaths != null) {
                mClipboardFilePaths = clipboardFilePaths;
            }
            mCutMode = savedInstanceState.getBoolean(KEY_CUT_MODE, false);
        }
        if (mFile == null) {
            mFile = new File(getArguments().getString(FILE_PATH_ARG));
        }
        if (!((ExplorerActivity) getActivity()).isInSearchMode()) {
            setSubFiles();
        }
        mAdapter = new ExplorerAdapter((ExplorerActivity) getActivity(), mFile.getParentFile() != null, mSubFiles,
                mBinaryClickListener, mDirectoryClickListener, mUpClickListener,
                mItemOverflowClickListener, mLongCheckedListener, mCheckedModeClickListener);
        mListView.setAdapter(mAdapter);
        /*mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
                outRect.set(0, 0, 0, (int) getResources().getDimension(R.dimen.card_spacing));
            }
        });*/
        mListView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                final AsyncTask imageTask = ((ExplorerAdapter.ViewHolder) view.getTag()).imageTask;
                final AsyncTask sizeTask = ((ExplorerAdapter.ViewHolder) view.getTag()).sizeTask;
                if (imageTask != null) {
                    imageTask.cancel(true);
                }
                if (sizeTask != null) {
                    sizeTask.cancel(true);
                }
            }
        });
        if (mClipboardFilePaths != null) {
            mFabPaste.setVisibility(View.VISIBLE);
        } else {
            mFabPaste.setVisibility(View.GONE);
        }
        setDividerDark(false);
        mFabPaste.setOnClickListener(mFabPasteClickListener);
        setupContextualItemMenu(savedInstanceState);
    }

    private void setDividerDark(final boolean dark) {
        final int dividerResId
                = dark
                ? R.drawable.file_item_list_divider_dark
                : R.drawable.file_item_list_divider_light;
        mListView.setDivider(getResources().getDrawable(dividerResId));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(FILE_PATH_KEY, mFile.getPath());
        for (FileItem fileItem : mSubFiles) {
            fileItem.image = null;
        }
        outState.putParcelableArrayList(KEY_SUB_FILE_ITEMS, mSubFiles);
        outState.putStringArrayList(KEY_CLIPBOARD_FILES, mClipboardFilePaths);
        outState.putBoolean(KEY_CUT_MODE, mCutMode);
        outState.putIntegerArrayList(KEY_CHECKED_POSITIONS, getCheckedPositions());
        super.onSaveInstanceState(outState);
    }

    private ArrayList<Integer> getCheckedPositions() {
        final SparseBooleanArray sba = mListView.getCheckedItemPositions();
        final ArrayList<Integer> checkedPositions = new ArrayList<Integer>(sba.size());
        for (int i = 0; i < sba.size(); i++) {
            final Integer position = sba.keyAt(i);
            if (sba.get(position)) {
                checkedPositions.add(position);
            }
        }
        return checkedPositions;
    }

    public void setFile(final File file, int position) {
        mListView.setVisibility(View.INVISIBLE);
        mFile = file;
        setSubFiles();
        mAdapter.setHasUpHeader(hasUpHeader());
        mAdapter.notifyDataSetChanged();

        if (position > mAdapter.getCount()) {
            position = mAdapter.getCount();
        }
        final int pos = position;
        mListView.clearFocus();
        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(pos);
                mListView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupContextualItemMenu(final Bundle savedInstanceState) {
        final List<File> mFilesSelected;
        if (savedInstanceState != null) {
            final ArrayList<Integer> checkedPositions
                    = savedInstanceState.getIntegerArrayList(KEY_CHECKED_POSITIONS);
            mFilesSelected = new ArrayList<File>(checkedPositions.size());
            for (Integer position : checkedPositions) {
                mFilesSelected.add(mAdapter.getFile(position));
            }
        } else {
            mFilesSelected = new ArrayList<File>();
        }
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                  long id, boolean checked) {
                // Here you can do something when items are selected/de-selected,
                // such as update the title in the CAB
                final File file = mAdapter.getFile(position);
                if (checked) {
                    mFilesSelected.add(file);
                } else {
                    mFilesSelected.remove(file);
                }
                mode.invalidate();
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // Respond to clicks on the actions in the CAB
                switch (item.getItemId()) {
                    case R.id.copy:
                        mCutMode = false;
                        beginToCopy(mFilesSelected);
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.cut:
                        mCutMode = true;
                        beginToCopy(mFilesSelected);
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.rename:
                        final SparseBooleanArray sba = mListView.getCheckedItemPositions();
                        for (int i = 0; i < mAdapter.getCount(); i++) {
                            if (sba.get(i)) {
                                beginToRename(mAdapter.getFile(i),
                                        mListView.getChildAt(i - mListView.getFirstVisiblePosition()));
                                mode.finish(); // Action picked, so close the CAB
                                return true;
                            }
                        }
                    case R.id.delete:
                        beginToDelete(new ArrayList<File>(mFilesSelected));
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.zip:
                        beginToZip(new ArrayList<File>(mFilesSelected));
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.share:
                        shareFiles(new ArrayList<File>(mFilesSelected));
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.open_with:
                        openFile(mFilesSelected.get(0), true);
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate the menu for the CAB
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.file_item, menu);
                mAdapter.setCheckedMode(true);
                setDividerDark(true);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Here you can make any necessary updates to the activity when
                // the CAB is removed. By default, selected items are deselected/unchecked.
                mFilesSelected.clear();
                mAdapter.setCheckedMode(false);
                setDividerDark(false);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Here you can perform updates to the CAB due to
                // an invalidate() request
                menu.findItem(R.id.open_with).setVisible(mFilesSelected.size() == 1
                        && !mFilesSelected.get(0).isDirectory());
                menu.findItem(R.id.rename).setVisible(mFilesSelected.size() == 1);
                // TODO: make zipping directories work
                menu.findItem(R.id.zip).setVisible(!selectionContainsDirectory());
                return false;
            }

            private boolean selectionContainsDirectory() {
                for (File file : mFilesSelected) {
                    if (file.isDirectory()) return true;
                }
                return false;
            }
        });
    }

    private void beginToCopy(List<File> filesToCopy) {
        if (filesToCopy.isEmpty()) {
            throw new RuntimeException("0 files to copy");
        }
        mClipboardFilePaths = new ArrayList<String>(filesToCopy.size());
        for (File file : filesToCopy) {
            mClipboardFilePaths.add(file.getPath());
        }
        mFabPaste.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.up_from_bottom));
        mFabPaste.setVisibility(View.VISIBLE);
    }

    private void doPaste() {
        // TODO: prompt overwrite
        // currently, automatically overwrites
        ((ExplorerActivity) getActivity()).doPaste(mClipboardFilePaths,
                mCutMode, mFile);
    }

    private void beginToRename(final File file, final View view) {
        /*view.setBackgroundResource(R.drawable.ripple_success);
        int[] state = new int[] {android.R.attr.state_focused, android.R.attr.state_pressed};
        android.util.Log.d("eric", "eric invalidate? : " + view.getBackground().setState(state));
        view.getBackground().invalidateSelf();*/
        final Intent intent = new Intent(getActivity(), RenameActivity.class);
        intent.putExtra(RenameActivity.EXTRA_FILE_OLD_NAME, file.getName());
        intent.putExtra(RenameActivity.EXTRA_FILE_OLD_PATH, file.getPath());
        startActivityForResult(intent, RENAME_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RENAME_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    refreshFile();
                } else {
                    Toast.makeText(getActivity(), R.string.rename_canceled, Toast.LENGTH_SHORT).show();
                }
                break;
            case DELETE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    refreshFile();
                } else {
                    Toast.makeText(getActivity(), R.string.delete_canceled, Toast.LENGTH_SHORT).show();
                }
                break;
            case ZIP_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    ((ExplorerActivity) getActivity())
                            .doZip(data.getStringArrayListExtra(
                                    ZipActivity.EXTRA_ZIP_FILE_PATHS),
                                    data.getStringExtra(ZipActivity.EXTRA_FINAL_ZIP_FILE_PATH));
                } else {
                    Toast.makeText(getActivity(), R.string.zip_canceled, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    public final void refreshFile() {
        setSubFiles();
        mAdapter.setHasUpHeader(hasUpHeader());
        mAdapter.notifyDataSetChanged();
    }

    /*
     * use for when only the order of the subfiles needs to change
     */
    public final void refreshSubFileOrder() {
        ((ExplorerActivity) getActivity()).doSort(mSubFiles);
    }

    private void beginToDelete(final ArrayList<File> files) {
        final ArrayList<String> filePaths = new ArrayList<String>(files.size());
        for (File file : files) {
            filePaths.add(file.getPath());
        }
        final Intent intent = new Intent(getActivity(), DeleteActivity.class);
        intent.putExtra(DeleteActivity.EXTRA_FILES_TO_DELETE, filePaths);
        startActivityForResult(intent, DELETE_REQUEST_CODE);
    }

    private void beginToZip(ArrayList<File> files) {
        final ArrayList<String> filePaths = new ArrayList<String>(files.size());
        for (File file : files) {
            filePaths.add(file.getPath());
        }
        final Intent intent = new Intent(getActivity(), ZipActivity.class);
        intent.putExtra(ZipActivity.EXTRA_PARENT_FILE_PATH, mFile.getPath());
        intent.putExtra(ZipActivity.EXTRA_ZIP_FILE_PATHS, filePaths);
        startActivityForResult(intent, ZIP_REQUEST_CODE);
    }

    private void shareFiles(final List<File> files) {
        final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        final ArrayList<Uri> uris = new ArrayList<Uri>(files.size());
        for (File file : files) {
            uris.add(Uri.fromFile(file));
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.setType("*/*");
        getActivity().startActivity(Intent.createChooser(intent, getText(R.string.share_using)));
    }

    private void openFile(final File file, final boolean showChooser) {
        if (!file.exists()) {
            Toast.makeText(getActivity(), R.string.error_file_does_not_exists, Toast.LENGTH_SHORT).show();
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Uri data = Uri.fromFile(file);
        String type = getMimeTypes().getMimeType(file.getName());
        intent.setDataAndType(data, type);
        if (showChooser) {
            getActivity().startActivity(Intent.createChooser(intent, getText(R.string.open_with)));
        } else {
            try {
                getActivity().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), R.string.application_not_available, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void doCompletedZipping(final boolean result, final String zipPath) {
        final int resultId = result ? R.string.zip_success : R.string.zip_error;
        Toast.makeText(getActivity(), resultId, Toast.LENGTH_LONG).show();
        final File zipFile = new File(zipPath);
        if (!result && zipFile.exists()) {
            FileUtils.deleteFile(zipFile);
        }
        refreshFile();
    }

    public void doCompletedPasting(final boolean result) {
        if (mCutMode) {
            mCutMode = false;
            if (result) {
                if (mClipboardFilePaths.size() == 1) {
                    Toast.makeText(getActivity(),
                            getString(R.string.cut_single_item_success,
                                    new File(mClipboardFilePaths.get(0)).getName()),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(),
                            getString(R.string.cut_multiple_items_success, mClipboardFilePaths.size()),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), R.string.error_cutting, Toast.LENGTH_LONG).show();
            }
        } else {
            if (result) {
                if (mClipboardFilePaths.size() == 1) {
                    Toast.makeText(getActivity(),
                            getString(R.string.copied_single_item_success,
                                    new File(mClipboardFilePaths.get(0)).getName()),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(),
                            getString(R.string.copied_multiple_items_success, mClipboardFilePaths.size()),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), R.string.error_copying, Toast.LENGTH_LONG).show();
            }
        }
        mClipboardFilePaths = null;
        mFabPaste.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.down_to_bottom));
        mFabPaste.setVisibility(View.GONE);
        refreshFile();
    }

    private MimeTypes getMimeTypes() {
        MimeTypeParser mtp = new MimeTypeParser();
        XmlResourceParser in = getResources().getXml(R.xml.mimetypes);
        try {
            return mtp.fromXmlResource(in);
        } catch (XmlPullParserException e) {
            throw new RuntimeException(
                    "PreselectedChannelsActivity: XmlPullParserException");
        } catch (IOException e) {
            throw new RuntimeException(
                    "PreselectedChannelsActivity: IOException");
        }
    }

    public int getCurrentPosition() {
        return mListView.getFirstVisiblePosition();
    }

    private void setSubFiles() {
        final boolean wasCached;
        List<FileItem> fileItems = ((ExplorerActivity) getActivity()).getSubFiles(mFile);
        final List<FileItem> newFiles = FileUtils.getFileItemList(mFile.listFiles());
        if (fileItems == null) {
            wasCached = false;
            fileItems = newFiles;
        } else {
            wasCached = true;
            updateOriginalList(fileItems, newFiles);
        }
        updateOriginalList(mSubFiles, fileItems);
        ((ExplorerActivity) getActivity()).cacheSubFiles(mFile, mSubFiles);
        if (!wasCached) {
            ((ExplorerActivity) getActivity()).doSort(mSubFiles);
        }
    }

    private static <T> void updateOriginalList(List<T> originalList, List<T> newList) {
        final Iterator<T> iter = originalList.iterator();
        while (iter.hasNext()) {
            final T item = iter.next();
            if (!newList.contains(item)) {
                iter.remove();
            }
        }
        for (T fileItem : newList) {
            if (!originalList.contains(fileItem)) {
                originalList.add(fileItem);
            }
        }
    }

    public void doCompletedSorting(final List<Integer> result) {
        final Map<FileItem, Integer> map = new HashMap<FileItem, Integer>(mSubFiles.size());
        for (int i = 0; i < mSubFiles.size(); i++) {
            // will size ever be different?
            map.put(mSubFiles.get(i), result.get(i));
        }
        Collections.sort(mSubFiles, new Comparator<FileItem>() {
            @Override
            public int compare(FileItem file0, FileItem file1) {
                return map.get(file0).compareTo(map.get(file1));
            }
        });
        mAdapter.setHasUpHeader(hasUpHeader());
        mAdapter.notifyDataSetChanged();
        ((ExplorerActivity) getActivity()).cacheSubFiles(mFile, mSubFiles);
    }

    private boolean hasUpHeader() {
        return !((ExplorerActivity) getActivity()).isInSearchMode()
                && mFile.getParentFile() != null;
    }

    /*
     * Just clears the list of subfiles.
     */
    public void clearSubfiles() {
        mSubFiles.clear();
        mAdapter.notifyDataSetChanged();
        //FileUtils.searchFile(mSubFiles, mFile, query);
    }

    public void doCompletedSearching(List<FileItem> fileList) {
        mSubFiles.clear();
        mSubFiles.addAll(fileList);
        mAdapter.notifyDataSetChanged();
    }
}
