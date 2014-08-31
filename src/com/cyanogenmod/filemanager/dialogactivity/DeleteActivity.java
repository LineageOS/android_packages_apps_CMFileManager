package com.cyanogenmod.filemanager.dialogactivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeleteActivity extends Activity {

    public static final String EXTRA_FILES_TO_DELETE = "EXTRA_FILES_TO_DELETE";

    private static final String KEY_FILES_TO_DELETE = "KEY_FILES_TO_DELETE";

    private ArrayList<String> mFilePaths;
    private List<File> mFilesToDelete;
    private ListView mListView;
    private Button mCancelButton;
    private Button mDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete);
        setResult(Activity.RESULT_CANCELED);
        mListView = (ListView) findViewById(R.id.list_files_to_delete);
        mCancelButton = (Button) findViewById(R.id.cancel_delete);
        mDeleteButton = (Button) findViewById(R.id.confirm_delete);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteFiles();
            }
        });
        if (savedInstanceState == null) {
            final Intent intent = getIntent();
            mFilePaths = intent.getStringArrayListExtra(EXTRA_FILES_TO_DELETE);
        } else {
            mFilePaths = savedInstanceState.getStringArrayList(KEY_FILES_TO_DELETE);
        }
        mFilesToDelete = new ArrayList<File>(mFilePaths.size());
        for (String path : mFilePaths) {
            mFilesToDelete.add(new File(path));
        }
        if (mFilesToDelete.isEmpty()) {
            throw new RuntimeException("You can't delete 0 files.");
        }
        if (mFilesToDelete.size() == 1) {
            setTitle(Html.fromHtml(getString(R.string.confirm_delete_single,
                    mFilesToDelete.get(0).getName())));
            mListView.setVisibility(View.GONE);
            mDeleteButton.setText(R.string.delete);
        } else {
            setTitle(getString(R.string.confirm_delete_multiple, mFilesToDelete.size()));
            mListView.setVisibility(View.VISIBLE);
            mDeleteButton.setText(R.string.delete_all);
            final ArrayList<String> fileNames = new ArrayList<String>(mFilesToDelete.size());
            for (File file : mFilesToDelete) {
                fileNames.add(file.getName());
            }
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this, R.layout.list_item_delete, fileNames);
            mListView.setAdapter(adapter);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArrayList(KEY_FILES_TO_DELETE, mFilePaths);
        super.onSaveInstanceState(outState);
    }

    private void deleteFiles() {
        boolean result = true;
        for (File file : mFilesToDelete) {
            if (!FileUtils.deleteFile(file)) {
                result = false;
            }
        }
        if (result) {
            if (mFilesToDelete.size() == 1) {
                Toast.makeText(this,
                        getString(R.string.delete_success_single, mFilesToDelete.get(0).getName()),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        getString(R.string.delete_success_multiple, mFilesToDelete.size()),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.error_deleting, Toast.LENGTH_LONG).show();
        }
        setResult(Activity.RESULT_OK);
        finish();
    }
}
