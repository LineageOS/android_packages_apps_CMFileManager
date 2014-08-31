package com.brianco.fileheaven;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.brianco.fileheaven.util.FileUtils;
import com.brianco.fileheaven.util.ImageUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ExplorerAdapter extends ArrayAdapter<FileItem> {

    private final ExplorerActivity mContext;
    private boolean mHasUpHeader;
    private boolean mIsInCheckedMode;
    private final List<FileItem> mFileList;
    private final List<FileItem> mFileImagesSetList;
    private final View.OnClickListener mBinaryClickListener;
    private final View.OnClickListener mDirectoryClickListener;
    private final View.OnClickListener mUpClickListener;
    private final View.OnClickListener mItemOverflowClickListener;
    private final View.OnLongClickListener mLongCheckedListener;
    private final View.OnClickListener mCheckedModeClickListener;

    public static class ViewHolder {

        public final TextView fileTitle;
        public final TextView fileSize;
        public final ImageView fileImage;
        public final ImageView itemOverflow;
        public int position;
        public AsyncTask imageTask;
        public AsyncTask sizeTask;
        public FileItem file;

        public ViewHolder(final View view) {
            fileTitle = (TextView) view.findViewById(R.id.file_title);
            fileSize = (TextView) view.findViewById(R.id.file_size);
            fileImage = (ImageView) view.findViewById(R.id.file_image);
            itemOverflow = (ImageView) view.findViewById(R.id.item_overflow);
        }
    }

    public ExplorerAdapter(final ExplorerActivity context, final boolean hasUpHeader,
                           final List<FileItem> fileList,
                           final View.OnClickListener binaryClickListener,
                           final View.OnClickListener directoryClickListener,
                           final View.OnClickListener upClickListener,
                           final View.OnClickListener itemOverflowClickListener,
                           final View.OnLongClickListener longCheckedListener,
                           final View.OnClickListener checkedModeClickListener) {
        super(context, R.layout.file_item, fileList);
        mContext = context;
        mHasUpHeader = hasUpHeader;
        mFileList = fileList;
        mFileImagesSetList = new ArrayList<FileItem>(fileList.size());
        mBinaryClickListener = binaryClickListener;
        mDirectoryClickListener = directoryClickListener;
        mUpClickListener = upClickListener;
        mItemOverflowClickListener = itemOverflowClickListener;
        mLongCheckedListener = longCheckedListener;
        mCheckedModeClickListener = checkedModeClickListener;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.file_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            // we take care of cancelling the AsyncTask in ExplorerFragment
            // (mListView.setRecyclerListener)
        }
        // to end ripple animation
        convertView.getBackground().setVisible(false, false);
        if (mHasUpHeader && position == 0) {
            holder.fileTitle.setText(R.string.parent_directory_symbol);
            holder.fileSize.setText(null);
            holder.sizeTask = null;
            holder.fileImage.setImageResource(R.drawable.ic_folder_up);
            holder.fileImage.setImageAlpha(255);
            holder.imageTask = null;
            if (mIsInCheckedMode) {
                holder.fileImage.setOnClickListener(null);
                convertView.setOnClickListener(null);
            } else {
                holder.fileImage.setOnClickListener(mUpClickListener);
                convertView.setOnClickListener(mUpClickListener);
            }
            holder.itemOverflow.setVisibility(View.INVISIBLE);
            holder.itemOverflow.setOnClickListener(null);
            convertView.setOnLongClickListener(null);
            return convertView;
        }
        holder.position = position;
        final int index = mHasUpHeader ? position - 1: position;
        holder.file = mFileList.get(index);
        final String fileName = holder.file.getName();
        final String filePath = holder.file.getPath();
        holder.fileImage.setImageAlpha(FileUtils.isHidden(fileName)
                ? FileUtils.HIDDEN_FILE_ALPHA : 255);
        holder.fileTitle.setText(fileName);
        holder.fileSize.setText(R.string.calculating);
        if (holder.file.size < 0) {
            holder.sizeTask = new SizeTask(holder.fileSize, holder.file).execute();
        } else {
            holder.fileSize.setText(
                    FileUtils.getReadableSize(mContext, holder.file, holder.file.size));
        }
        if (holder.file.isDirectory()) {
            holder.fileImage.setImageResource(R.drawable.ic_folder);
            holder.imageTask = null;
            holder.fileImage.setOnClickListener(null);
            if (mIsInCheckedMode) {
                convertView.setOnClickListener(mCheckedModeClickListener);
                convertView.setOnLongClickListener(null);
            } else {
                convertView.setOnClickListener(mDirectoryClickListener);
                convertView.setOnLongClickListener(mLongCheckedListener);
            }
        } else {
            final String fileExtension = FileUtils.getExtension(fileName).toLowerCase();
            if (FileUtils.EXTENSION_APK.equals(fileExtension)) {
                holder.fileImage.setImageResource(R.drawable.apk_extension);
                if (holder.file.image == null) {
                    holder.imageTask = new LoadApkTask(holder.fileTitle, holder.fileImage, filePath,
                            holder.file, parent.getChildCount())
                            .execute();
                } else {
                    holder.imageTask = null;
                    holder.fileImage.setImageDrawable(holder.file.image);
                }
                holder.fileImage.setOnClickListener(null);
            } else if (FileUtils.EXTENSION_PNG.equals(fileExtension)
                    || FileUtils.EXTENSION_JPG.equals(fileExtension)
                    || FileUtils.EXTENSION_JPEG.equals(fileExtension)
                    || FileUtils.EXTENSION_GIF.equals(fileExtension)) {
                holder.fileImage.setImageResource(R.drawable.ic_action_picture);
                holder.fileImage.setOnClickListener(null);
                if (holder.file.image == null) {
                    holder.imageTask = new LoadPictureTask(holder.fileImage, filePath,
                            holder.file, parent.getChildCount()).execute();
                } else {
                    holder.imageTask = null;
                    holder.fileImage.setImageDrawable(holder.file.image);
                    holder.fileImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final Rect startBounds = new Rect();
                            view.getGlobalVisibleRect(startBounds);
                            mContext.zoomImage(holder.file.image, startBounds);
                        }
                    });
                }
            }  else if (FileUtils.EXTENSION_ZIP.equals(fileExtension)) {
                holder.fileImage.setImageResource(R.drawable.ic_zip_file);
                holder.imageTask = null;
                holder.fileImage.setOnClickListener(null);
            }  else if (FileUtils.EXTENSION_PDF.equals(fileExtension)) {
                holder.fileImage.setImageResource(R.drawable.ic_file_pdf);
                holder.imageTask = null;
                holder.fileImage.setOnClickListener(null);
            }  else if (FileUtils.EXTENSION_MP4.equals(fileExtension)) {
                holder.fileImage.setImageResource(R.drawable.ic_video);
                holder.imageTask = null;
                holder.fileImage.setOnClickListener(null);
            } else {
                holder.fileImage.setImageResource(R.drawable.ic_file);
                holder.imageTask = null;
                holder.fileImage.setOnClickListener(null);
            }
            if (mIsInCheckedMode) {
                convertView.setOnClickListener(mCheckedModeClickListener);
                convertView.setOnLongClickListener(null);
            } else {
                convertView.setOnClickListener(mBinaryClickListener);
                convertView.setOnLongClickListener(mLongCheckedListener);
            }
        }
        holder.itemOverflow.setVisibility(View.VISIBLE);
        holder.itemOverflow.setOnClickListener(mItemOverflowClickListener);
        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return mHasUpHeader ? position - 1 : position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        final int header = mHasUpHeader ? 1 : 0;
        return mFileList.size() + header;
    }

    public void setHasUpHeader(final boolean hasUpHeader) {
        mHasUpHeader = hasUpHeader;
    }

    public File getFile(int position) {
        final int index = mHasUpHeader ? position - 1 : position;
        return mFileList.get(index);
    }

    public void setCheckedMode(final boolean isInCheckedMode) {
        mIsInCheckedMode = isInCheckedMode;
        notifyDataSetChanged();
    }

    private class SizeTask extends AsyncTask<Void, Void, Long> {
        private final WeakReference<TextView> textViewWeakReference;
        private final FileItem mFile;

        public SizeTask(final TextView textView, final FileItem file) {
            // Use a WeakReference to ensure the TextView can be garbage collected
            textViewWeakReference = new WeakReference<TextView>(textView);
            mFile = file;
        }

        @Override
        protected Long doInBackground(Void... params) {
            return FileUtils.getSize(mFile);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Long result) {
            if (textViewWeakReference != null) {
                final TextView textView = textViewWeakReference.get();
                if (textView != null) {
                    textView.setText(FileUtils.getReadableSize(mContext, mFile, result));
                    mFile.size = result;
                }
            }
        }
    }

    private class LoadPictureTask extends AsyncTask<Void, Void, Drawable> {
        private final WeakReference<ImageView> imageViewReference;
        private final String mPicturePath;
        private final FileItem mFile;
        private final int mKillCount;

        public LoadPictureTask(final ImageView imageView, final String picturePath,
                               final FileItem file, final int killCount) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            mPicturePath = picturePath;
            mFile = file;
            mKillCount = killCount;
        }

        @Override
        protected Drawable doInBackground(Void... params) {
            final int imageSize = (int) mContext.getResources()
                    .getDimension(R.dimen.picture_preview_large);
            return new BitmapDrawable(mContext.getResources(),
                    ImageUtils.decodeSampledBitmapFromFile(
                            mPicturePath, imageSize, imageSize));
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(final Drawable result) {
            if (imageViewReference != null && result != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageDrawable(result);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final Rect startBounds = new Rect();
                            view.getGlobalVisibleRect(startBounds);
                            mContext.zoomImage(result, startBounds);
                        }
                    });
                    setFileImage(mFile, result, mKillCount);
                }
            }
        }
    }

    private class LoadApkTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<TextView> textViewWeakReference;
        private final WeakReference<ImageView> imageViewReference;
        private final String mApkPath;
        private final PackageManager mPackageManager;
        private final FileItem mFile;
        private final int mKillCount;
        private Drawable appIcon;
        private String appName;

        public LoadApkTask(final TextView textView,
                           final ImageView imageView, final String apkPath,
                           final FileItem file, final int killCount) {
            // Use a WeakReference to ensure the Views can be garbage collected
            textViewWeakReference = new WeakReference<TextView>(textView);
            imageViewReference = new WeakReference<ImageView>(imageView);
            mApkPath = apkPath;
            mPackageManager = mContext.getPackageManager();
            mFile = file;
            mKillCount = killCount;
        }

        @Override
        protected Void doInBackground(Void... params) {
            getAppInfo();
            return null;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Void result) {
            if (imageViewReference != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageDrawable(appIcon);
                    setFileImage(mFile, appIcon, mKillCount);
                }
            }
            if (textViewWeakReference != null) {
                final TextView textView = textViewWeakReference.get();
                if (textView != null) {
                    textView.setText(Html.fromHtml(mContext.getString(R.string.apk_app_name,
                            textView.getText(), appName)));
                }
            }
        }

        private void getAppInfo() {
            PackageInfo pi = mPackageManager.getPackageArchiveInfo(mApkPath, 0);
            // the secret are these two lines....
            pi.applicationInfo.sourceDir       = mApkPath;
            pi.applicationInfo.publicSourceDir = mApkPath;
            appIcon = pi.applicationInfo.loadIcon(mPackageManager);
            appName = (String) pi.applicationInfo.loadLabel(mPackageManager);
        }
    }

    private void setFileImage(final FileItem file, final Drawable image, final int killCount) {
        file.image = image;
        mFileImagesSetList.add(file);
        // check memory
        final Runtime info = Runtime.getRuntime();
        final long freeSize = info.freeMemory();
        final long totalSize = info.maxMemory();
        if ((double) freeSize / totalSize <= 0.3) {
            for (int i = 0; i < killCount; i++) {
                if (mFileImagesSetList.size() <= 0) return;
                mFileImagesSetList.get(0).image = null;
                mFileImagesSetList.remove(0);
            }
        }
    }
}
