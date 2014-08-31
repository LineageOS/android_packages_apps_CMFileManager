package com.cyanogenmod.filemanager;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.net.URI;

/*
 * You must null out the Drawable (.image) before passing a FileItem to another Context!
 * That is your job when using this class!
 * Be careful with Parcelable here.
 */
public class FileItem extends File implements Parcelable {

    public Drawable image = null;
    public long size = -1;

    public FileItem(final File file) {
        this(file.getPath());
    }

    public FileItem(File dir, String name) {
        super(dir, name);
    }

    public FileItem(String path) {
        super(path);
    }

    public FileItem(String dirPath, String name) {
        super(dirPath, name);
    }

    public FileItem(URI uri) {
        super(uri);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (this == other) return true;
        if (!(other instanceof File)) return false;
        final File o = (File) other;
        return this.getPath().equals(o.getPath());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(size);
        out.writeString(getPath());
    }

    public static final Parcelable.Creator<FileItem> CREATOR
            = new Parcelable.Creator<FileItem>() {
        public FileItem createFromParcel(Parcel in) {
            final long size = in.readLong();
            final String path = in.readString();
            final FileItem fileItem = new FileItem(path);
            fileItem.size = size;
            return fileItem;
        }

        public FileItem[] newArray(int size) {
            return new FileItem[size];
        }
    };
}
