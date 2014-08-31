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
import java.util.ArrayList;

public class ZipActivity extends Activity {

    public static final String EXTRA_PARENT_FILE_PATH = "EXTRA_PARENT_FILE_PATH";
    public static final String EXTRA_ZIP_FILE_PATHS = "EXTRA_ZIP_FILE_PATHS";
    public static final String EXTRA_FINAL_ZIP_FILE_PATH = "EXTRA_FINAL_ZIP_FILE_PATH";

    private static final String ARG_PARENT_FILE_PATH = "ARG_PARENT_FILE_PATH";
    private static final String ARG_ZIP_FILE_PATHS = "ARG_ZIP_FILE_PATHS";
    private static final String ARG_ZIP_FILE_NAME_TYPED = "ARG_ZIP_FILE_NAME_TYPED";

    private EditText mNameEt;
    private Button mCancelButton;
    private Button mZipButton;
    private String mParentPath;
    private ArrayList<String> mZipFilePaths;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zip);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setResult(Activity.RESULT_CANCELED);
        mNameEt = (EditText) findViewById(R.id.zip_edit_text);
        mCancelButton = (Button) findViewById(R.id.cancel_zip);
        mZipButton = (Button) findViewById(R.id.confirm_zip);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        mZipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doZip();
            }
        });
        if (savedInstanceState == null) {
            final Intent intent = getIntent();
            mParentPath = intent.getStringExtra(EXTRA_PARENT_FILE_PATH);
            mZipFilePaths = intent.getStringArrayListExtra(EXTRA_ZIP_FILE_PATHS);
        } else {
            mParentPath = savedInstanceState.getString(ARG_PARENT_FILE_PATH);
            mZipFilePaths = savedInstanceState.getStringArrayList(ARG_ZIP_FILE_PATHS);
            final CharSequence typedName = savedInstanceState.getCharSequence(ARG_ZIP_FILE_NAME_TYPED);
            mNameEt.setText(typedName);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(ARG_PARENT_FILE_PATH, mParentPath);
        outState.putStringArrayList(ARG_ZIP_FILE_PATHS, mZipFilePaths);
        outState.putCharSequence(ARG_ZIP_FILE_NAME_TYPED, mNameEt.getText());
        super.onSaveInstanceState(outState);
    }

    private void doZip() {
        final CharSequence text = mNameEt.getText();
        if (text.length() <= 0) {
            Toast.makeText(this, R.string.empty_name, Toast.LENGTH_SHORT).show();
            return;
        }
        final String finalZipPath = mParentPath + "/" + text;
        if (new File(finalZipPath).exists()) {
            Toast.makeText(ZipActivity.this,
                    R.string.file_exists, Toast.LENGTH_LONG).show();
            return;
        }
        final Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_ZIP_FILE_PATHS, mZipFilePaths);
        intent.putExtra(EXTRA_FINAL_ZIP_FILE_PATH, finalZipPath);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
