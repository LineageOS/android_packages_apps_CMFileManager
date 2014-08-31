package com.cyanogenmod.filemanager.taskfragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;

import com.cyanogenmod.filemanager.ExplorerActivity;
import com.cyanogenmod.filemanager.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    public static final String ARG_FILE_PATH = "ARG_FILE_PATH";
    public static final String ARG_QUERY = "ARG_QUERY";

    private ProgressDialog mPd;
    private SearchTask mSearchTask = null;
    private Spanned mMessage;
    private File mFile;
    private String mFileName;
    private String mQuery;

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
        mPd.show();
        final String filePath = getArguments().getString(ARG_FILE_PATH);
        mFile = new File(filePath);
        final String rootPath = Environment.getExternalStorageDirectory().getPath();
        mFileName = filePath.equals(rootPath) ? getString(R.string.sdcard_short_name)
                : mFile.getName();
        mQuery = getArguments().getString(ARG_QUERY);
        if (mSearchTask == null) {
            mSearchTask = new SearchTask();
            mSearchTask.execute();
        } else {
            mPd.setMessage(mMessage);
        }
    }

    @Override
    public void onDetach() {
        mPd.dismiss();
        mPd = null;
        super.onDetach();
    }

    private void setSpannedMessage(final int num) {
        mMessage = Html.fromHtml(getResources().getQuantityString(R.plurals.searching,
                num, mQuery, mFileName, num));
    }

    private class SearchTask extends AsyncTask<Void, Void, List<File>> {

        @Override
        protected void onPreExecute() {
            setSpannedMessage(0);
            mPd.setMessage(mMessage);
        }

        @Override
        protected List<File> doInBackground(Void... voids) {
            List<File> fileList = new ArrayList<File>();
            searchFile(fileList, mFile, mQuery);
            return fileList;
        }

        @Override
        protected void onPostExecute(List<File> result) {
            ((ExplorerActivity) getActivity()).doCompletedSearching(result);
            mSearchTask = null;
        }

        private void searchFile(final List<File> fileList,
                                final File file, final String query) {
            if (file.getName().toLowerCase().contains(query.toLowerCase())
                    && !mFile.getPath().equals(file.getPath())) {
                fileList.add(file);
                setSpannedMessage(fileList.size());
                publishProgress();
            }
            if (file.isDirectory()) {
                for (File subFile : file.listFiles()) {
                    searchFile(fileList, subFile, query);
                }
            }
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
            mPd.setMessage(mMessage);
        }
    }
}
