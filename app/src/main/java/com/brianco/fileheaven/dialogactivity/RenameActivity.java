package com.brianco.fileheaven.dialogactivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.brianco.fileheaven.R;

import java.io.File;

public class RenameActivity extends Activity {

    public static final String EXTRA_FILE_OLD_PATH = "EXTRA_FILE_OLD_PATH";
    public static final String EXTRA_FILE_OLD_NAME = "EXTRA_FILE_OLD_NAME";

    private static final String ARG_FILE_OLD_PATH = "ARG_FILE_OLD_PATH";
    private static final String ARG_FILE_NAME_TYPED = "ARG_FILE_NAME_TYPED";

    private File mOldFile;
    private EditText mRenameEt;
    private Button mCancelButton;
    private Button mRenameButton;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setResult(Activity.RESULT_CANCELED);
        mRenameEt = (EditText) findViewById(R.id.rename_edit_text);
        mCancelButton = (Button) findViewById(R.id.cancel_rename);
        mRenameButton = (Button) findViewById(R.id.confirm_rename);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        mRenameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doRename(mRenameEt.getText().toString());
            }
        });
        final String oldPath;
        if (savedInstanceState == null) {
            final Intent intent = getIntent();
            oldPath = intent.getStringExtra(EXTRA_FILE_OLD_PATH);
            final String oldName = intent.getStringExtra(EXTRA_FILE_OLD_NAME);
            mRenameEt.setText(oldName);
            final int startPos = oldName.lastIndexOf(".");
            if (startPos >= 0) {
                mRenameEt.setSelection(startPos);
            } else {
                mRenameEt.setSelection(oldName.length());
            }
        } else {
            oldPath = savedInstanceState.getString(ARG_FILE_OLD_PATH);
            final CharSequence typedName = savedInstanceState.getCharSequence(ARG_FILE_NAME_TYPED);
            mRenameEt.setText(typedName);
        }
        mOldFile = new File(oldPath);
        final int titleRes = mOldFile.isDirectory() ? R.string.rename_folder : R.string.rename_file;
        setTitle(titleRes);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(ARG_FILE_OLD_PATH, mOldFile.getPath());
        outState.putCharSequence(ARG_FILE_NAME_TYPED, mRenameEt.getText());
        super.onSaveInstanceState(outState);
    }

    private void doRename(final String newName) {
        if (newName.length() <= 0) {
            Toast.makeText(this, R.string.empty_name, Toast.LENGTH_SHORT).show();
            return;
        }
        final String oldPath = mOldFile.getPath();
        final String newPath = oldPath.substring(0, oldPath.lastIndexOf("/") + 1)
                + newName;
        final File newFile = new File(newPath);
        if (newFile.exists()) {
            Toast.makeText(this, R.string.file_exists, Toast.LENGTH_LONG).show();
        } else {
            if (mOldFile.renameTo(newFile)) {
                // TODO : green success ripple
                Toast.makeText(this,
                        R.string.rename_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        R.string.rename_failure, Toast.LENGTH_LONG).show();
            }
        }
        setResult(Activity.RESULT_OK);
        finish();
    }
}
