package com.cyanogenmod.filemanager.controllers;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ui.IconRenderer;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

import java.util.List;

/**
 * This is meant to make this section more transferable
 */
public class MStarUController {

    public interface OnClickListener {
        void onItemClick(FileSystemObject fso);
        void onDetailsClick(FileSystemObject fso);
    }

    private class ViewHolder {
        /* package */ ImageView fileImage;
        /* package */ TextView fileName;
        /* package */ TextView parent;
        /* package */ View details;

        FileSystemObject fso;

        public ViewHolder(View row) {
            fileImage = (ImageView)row.findViewById(R.id.file_image);
            fileName = (TextView)row.findViewById(R.id.file_name);
            parent = (TextView)row.findViewById(R.id.file_parent);
            details = row.findViewById(R.id.file_details);
        }
    }

    private View.OnClickListener mOnItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MStarUController.ViewHolder h =
                    (MStarUController.ViewHolder)view.getTag(R.id.tag_viewholder);

            if (mOnClickListener != null) {
                switch (view.getId()) {
                    case R.id.file_details:
                        mOnClickListener.onDetailsClick(h.fso);
                        break;
                    default:
                        mOnClickListener.onItemClick(h.fso);
                        break;

                }
            }
        }
    };

    private ViewGroup mStarUGroup;

    private ViewGroup mStarUEmpty;

    private MStarUController.OnClickListener mOnClickListener;

    private Context mContext;

    private IconHolder mImageLoader;

    public MStarUController(Context context, View root, MStarUController.OnClickListener l) {
        mContext = context;
        mStarUGroup = (ViewGroup)root.findViewById(R.id.mstaru_list);
        mStarUEmpty = (ViewGroup)root.findViewById(R.id.mstaru_empty);
        // This is kind of crap since it won't pick up changes if we leave the screen and come back
        FileManagerSettings displayThumbsPref = FileManagerSettings.SETTINGS_DISPLAY_THUMBS;
        final boolean displayThumbs =
                Preferences.getSharedPreferences().getBoolean(
                        displayThumbsPref.getId(),
                        ((Boolean)displayThumbsPref.getDefaultValue()).booleanValue());
        mImageLoader = new IconHolder(context, displayThumbs);
        mOnClickListener = l;
    }

    public void replaceData(List<FileSystemObject> files) {
        if (files == null || files.isEmpty()) {
            mStarUGroup.setVisibility(View.GONE);
            mStarUEmpty.setVisibility(View.VISIBLE);
            return;
        }
        mStarUGroup.setVisibility(View.VISIBLE);
        mStarUEmpty.setVisibility(View.GONE);

        int size = Math.min(files.size(), mStarUGroup.getChildCount());
        int i = 0;
        for (; i < size; i++) {
            final View row = mStarUGroup.getChildAt(i);
            final FileSystemObject fso = files.get(i);

            row.setVisibility(View.VISIBLE);
            MStarUController.ViewHolder h =
                    (MStarUController.ViewHolder)row.getTag(R.id.tag_viewholder);

            if (h == null) {
                h = new MStarUController.ViewHolder(row);
                row.setTag(R.id.tag_viewholder, h);
                row.setOnClickListener(mOnItemClickListener);
                h.details.setOnClickListener(mOnItemClickListener);
                h.details.setTag(R.id.tag_viewholder, h);
            }
            h.fso = fso;

            final int mimeTypeIconId = MimeTypeHelper.getIcon(mContext, fso);
            IconHolder.ICallback callback = new IconRenderer(mContext, mimeTypeIconId);
            mImageLoader.loadDrawable(h.fileImage, fso, mimeTypeIconId, callback);

            h.fileName.setText(fso.getName());
            h.parent.setText(fso.getParent());
        }
        size = mStarUGroup.getChildCount();
        for (; i < size; i++) {
            View row = mStarUGroup.getChildAt(i);
            row.setVisibility(View.GONE);
        }
    }
}
