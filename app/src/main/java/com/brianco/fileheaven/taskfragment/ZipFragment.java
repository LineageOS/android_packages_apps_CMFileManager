package com.brianco.fileheaven.taskfragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import com.brianco.fileheaven.ExplorerActivity;
import com.brianco.fileheaven.R;
import com.brianco.fileheaven.util.FileUtils;

import java.io.IOException;
import java.util.List;

public class ZipFragment extends Fragment {

    public static final String ARG_FILE_PATHS = "ARG_FILE_PATHS";
    public static final String ARG_FINAL_ZIP_PATH = "ARG_FINAL_ZIP_PATH";

    private ProgressDialog mPd;
    private ZipTask mZipTask = null;
    private List<String> mFilePaths;
    private String mZipPath;

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
        mPd.setMessage(getString(R.string.zipping));
        mPd.show();
        mFilePaths = getArguments().getStringArrayList(ARG_FILE_PATHS);
        mZipPath = getArguments().getString(ARG_FINAL_ZIP_PATH);
        if (mZipTask == null) {
            mZipTask = new ZipTask();
            mZipTask.execute();
        }
    }

    @Override
    public void onDetach() {
        mPd.dismiss();
        mPd = null;
        super.onDetach();
    }

    private class ZipTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                FileUtils.zip(mFilePaths, mZipPath);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            ((ExplorerActivity) getActivity()).doCompletedZipping(result, mZipPath);
            mZipTask = null;
        }
    }
}
