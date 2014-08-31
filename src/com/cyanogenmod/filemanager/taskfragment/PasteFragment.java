package com.cyanogenmod.filemanager.taskfragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import com.cyanogenmod.filemanager.ExplorerActivity;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PasteFragment extends Fragment {

    public static final String ARG_CLIPBOARD_FILE_PATHS = "ARG_CLIPBOARD_FILE_PATHS";
    public static final String ARG_FILE_PATH = "ARG_FILE_PATH";
    public static final String ARG_CUT_MODE = "ARG_CUT_MODE";

    private ProgressDialog mPd;
    private PasteTask mPasteTask = null;
    private List<String> mClipboardFilePaths;
    private File mFile;
    private boolean mCutMode;

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
        mPd.setMessage(getString(R.string.pasting));
        mPd.show();
        mClipboardFilePaths = getArguments().getStringArrayList(ARG_CLIPBOARD_FILE_PATHS);
        mFile = new File(getArguments().getString(ARG_FILE_PATH));
        mCutMode = getArguments().getBoolean(ARG_CUT_MODE);
        if (mPasteTask == null) {
            mPasteTask = new PasteTask();
            mPasteTask.execute();
        }
    }

    @Override
    public void onDetach() {
        mPd.dismiss();
        mPd = null;
        super.onDetach();
    }

    private class PasteTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean result = true;
            for (String oldPath : mClipboardFilePaths) {
                final File oldFile = new File(oldPath);
                final String keepName = oldFile.getName();
                final String newPath = mFile.getPath() + "/" + keepName;
                if (oldPath.equals(newPath)) {
                    continue;
                }
                try {
                    FileUtils.copy(oldFile, new File(newPath));
                    if (mCutMode) {
                        FileUtils.deleteFile(oldFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    result = false;
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            ((ExplorerActivity) getActivity()).doCompletedPasting(result);
            mPasteTask = null;
        }
    }
}
