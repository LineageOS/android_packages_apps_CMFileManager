package com.brianco.fileheaven.taskfragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import com.brianco.fileheaven.ExplorerActivity;
import com.brianco.fileheaven.FileItem;
import com.brianco.fileheaven.R;
import com.brianco.fileheaven.util.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortFragment extends Fragment {

    public static final String ARG_FILE_ITEMS = "ARG_FILE_ITEMS";
    public static final String ARG_SORT_BY = "ARG_SORT_BY";

    private ProgressDialog mPd;
    private SortTask mSortTask = null;
    private List<FileItem> mFileItems;
    private int mSortBy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPd = new ProgressDialog(getActivity());
        mPd.setCancelable(false);
        mPd.setMessage(getString(R.string.sorting));
        mPd.show();
        mFileItems = getArguments().getParcelableArrayList(ARG_FILE_ITEMS);
        mSortBy = getArguments().getInt(ARG_SORT_BY);
        setPdLayout();
        if (mSortTask == null) {
            mSortTask = new SortTask();
            mSortTask.execute();
        }
    }

    private void setPdLayout() {
        if ((mSortBy != ExplorerActivity.SORT_BY_SIZE_LOW
                && mSortBy != ExplorerActivity.SORT_BY_SIZE_HIGH)
                || mFileItems.isEmpty()
                || (mFileItems.get(0).size >= 0
                && mFileItems.get(mFileItems.size() / 2).size >= 0
                && mFileItems.get(mFileItems.size() - 1).size >= 0)) {
            mPd.setContentView(R.layout.gone);
        }
    }

    @Override
    public void onDetach() {
        mPd.dismiss();
        mPd = null;
        super.onDetach();
    }

    public final void cancelSort() {
        if (mSortTask != null) {
            mSortTask.cancel(true);
        }
    }

    private class SortTask extends AsyncTask<Void, Void, List<Integer>> {

        @Override
        protected List<Integer> doInBackground(Void... voids) {
            final Comparator<FileItem> comparator;
            switch (mSortBy) {
                case ExplorerActivity.SORT_BY_NAME_FOLDERS_FIRST:
                    comparator = FileUtils.nameFolderFirstComparator;
                    break;
                case ExplorerActivity.SORT_BY_NAME:
                    comparator = FileUtils.nameComparator;
                    break;
                case ExplorerActivity.SORT_BY_EXTENSION:
                    comparator = FileUtils.extensionComparator;
                    break;
                case ExplorerActivity.SORT_BY_SIZE_LOW:
                    comparator = FileUtils.sizeLowComparator;
                    break;
                case ExplorerActivity.SORT_BY_SIZE_HIGH:
                    comparator = FileUtils.sizeHighComparator;
                    break;
                case ExplorerActivity.SORT_BY_LAST_MODIFIED:
                    comparator = FileUtils.lastModifiedComparator;
                    break;
                default:
                    comparator = FileUtils.nameFolderFirstComparator;
                    break;
            }

            List<FileItem> sortedFileItems = new ArrayList<FileItem>(mFileItems);

            Collections.sort(sortedFileItems, comparator);

            List<Integer> result = new ArrayList<Integer>(mFileItems.size());
            for (FileItem fileItem : mFileItems) {
                result.add(sortedFileItems.indexOf(fileItem));
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<Integer> result) {
            if (isCancelled()) return;
            ((ExplorerActivity) getActivity()).doCompletedSorting(result);
            mSortTask = null;
        }
    }
}
