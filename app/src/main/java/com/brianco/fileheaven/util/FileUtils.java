package com.brianco.fileheaven.util;

import android.content.Context;

import com.brianco.fileheaven.FileItem;
import com.brianco.fileheaven.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static final int HIDDEN_FILE_ALPHA = 145;
    public static final String EXTENSION_APK = ".apk";
    public static final String EXTENSION_PNG = ".png";
    public static final String EXTENSION_JPG = ".jpg";
    public static final String EXTENSION_JPEG = ".jpeg";
    public static final String EXTENSION_GIF = ".gif";
    public static final String EXTENSION_MP4 = ".mp4";
    public static final String EXTENSION_ZIP = ".zip";
    public static final String EXTENSION_PDF = ".pdf";

    /*
     * Remember to call extension.toLowerCase() if you need to.
     */
    public static String getExtension(String path) {
        if (path == null) {
            return null;
        }
        int dot = path.lastIndexOf(".");
        if (dot >= 0) {
            return path.substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    public static boolean isHidden(String fileName) {
        return fileName.startsWith(".");
    }

    public static ArrayList<String> getPaths(Collection<File> files) {
        final ArrayList<String> pathList = new ArrayList<String>(files.size());
        for (File file : files) {
            pathList.add(file.getPath());
        }
        return pathList;
    }

    public static ArrayList<File> getFiles(Collection<String> paths) {
        final ArrayList<File> fileList = new ArrayList<File>(paths.size());
        for (String path : paths) {
            fileList.add(new File(path));
        }
        return fileList;
    }

    public static long getSize(File file) {
        long size;
        if (file.isDirectory()) {
            size = 0;
            final File[] fileArr = file.listFiles();
            if (fileArr != null) {
                for (File child : fileArr) {
                    size += getSize(child);
                }
            }
        } else {
            size = file.length();
        }
        return size;
    }

    public static String getReadableSize(final Context context, final File file, final long size) {
        if (size <= 0) {
            final File[] subFiles = file.listFiles();
            if (subFiles == null) {
                return context.getString(R.string.empty_file);
            } else {
                return context.getResources().getQuantityString(R.plurals.empty_folder,
                        subFiles.length, subFiles.length);
            }
        }
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups))
                + " " + units[digitGroups];
    }

    // If targetLocation does not exist, it will be created.
    public static void copy(File sourceLocation , File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }

            for (String child : sourceLocation.list()) {
                copy(new File(sourceLocation, child),
                        new File(targetLocation, child));
            }
        } else {

            // make sure the directory we plan to store the recording in exists
            File directory = targetLocation.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteFile(child);
            }
        }
        return file.delete();
    }

    /*
     * Please note that zipping directories is not currently supported.
     */
    public static void zip(List<String> files, String zipFile) throws IOException {
        BufferedInputStream origin;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[1024];
            for (String file : files) {
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, 1024);
                try {
                    ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, 1024)) != -1) {
                        out.write(data, 0, count);
                    }
                }
                finally {
                    origin.close();
                }
            }
        }
        finally {
            out.close();
        }
    }

    public static void unzip(String zipFile, String location) throws IOException {
        File f = new File(location);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                String path = location + ze.getName();

                if (ze.isDirectory()) {
                    File unzipFile = new File(path);
                    if (!unzipFile.isDirectory()) {
                        unzipFile.mkdirs();
                    }
                }
                else {
                    FileOutputStream fout = new FileOutputStream(path, false);
                    try {
                        for (int c = zin.read(); c != -1; c = zin.read()) {
                            fout.write(c);
                        }
                        zin.closeEntry();
                    }
                    finally {
                        fout.close();
                    }
                }
            }
        }
        finally {
            zin.close();
        }
    }

    public static ArrayList<FileItem> getFileItemList(List<File> fileList) {
        if (fileList == null) {
            return new ArrayList<FileItem>(0);
        }
        final ArrayList<FileItem> fileItemList = new ArrayList<FileItem>(fileList.size());
        for (File file : fileList) {
            fileItemList.add(new FileItem(file));
        }
        return fileItemList;
    }

    public static List<FileItem> getFileItemList(File[] fileArr) {
        if (fileArr == null) {
            return new ArrayList<FileItem>(0);
        }
        final List<FileItem> fileItemList = new ArrayList<FileItem>(fileArr.length);
        for (File file : fileArr) {
            fileItemList.add(new FileItem(file));
        }
        return fileItemList;
    }

    public static Comparator<FileItem> nameFolderFirstComparator = new Comparator<FileItem>() {
        @Override
        public int compare(FileItem file0, FileItem file1) {
            if (file0.isDirectory() && !file1.isDirectory()) {
                return -1;
            } else if (!file0.isDirectory() && file1.isDirectory()) {
                return 1;
            } else {
                return file0.getName().toLowerCase().compareTo(file1.getName().toLowerCase());
            }
        }
    };

    public static Comparator<FileItem> nameComparator = new Comparator<FileItem>() {
        @Override
        public int compare(FileItem file0, FileItem file1) {
            return file0.getName().toLowerCase().compareTo(file1.getName().toLowerCase());
        }
    };

    public static Comparator<FileItem> extensionComparator = new Comparator<FileItem>() {
        @Override
        public int compare(FileItem file0, FileItem file1) {
            final String file0Name = file0.getName().toLowerCase();
            final String file1Name = file1.getName().toLowerCase();
            // handle directories
            if (file0.isDirectory() && !file1.isDirectory()) {
                return -1;
            } else if (!file0.isDirectory() && file1.isDirectory()) {
                return 1;
            } else if (file0.isDirectory()) {
                return file0Name.compareTo(file1Name);
            }
            // handle binaries
            final String file0Extension = getExtension(file0Name).toLowerCase();
            final String file1Extension = getExtension(file1Name).toLowerCase();
            final int extensionComparison = file0Extension.compareTo(file1Extension);
            if (extensionComparison != 0) {
                return extensionComparison;
            } else {
                return file0Name.compareTo(file1Name);
            }
        }
    };

    public static Comparator<FileItem> sizeLowComparator = new Comparator<FileItem>() {
        @Override
        public int compare(FileItem file0, FileItem file1) {
            if (file0.size < 0) {
                file0.size = FileUtils.getSize(file0);
            }
            if (file1.size < 0) {
                file1.size = FileUtils.getSize(file1);
            }
            if (file0.size == file1.size) {
                return nameFolderFirstComparator.compare(file0, file1);
            } else if (file0.size > file1.size) {
                return 1;
            } else {
                return  -1;
            }
            // below will have issues with large longs
            // return (int) (file0.size - file1.size);
        }
    };

    public static Comparator<FileItem> sizeHighComparator = new Comparator<FileItem>() {
        @Override
        public int compare(FileItem file0, FileItem file1) {
            if (file0.size < 0) {
                file0.size = FileUtils.getSize(file0);
            }
            if (file1.size < 0) {
                file1.size = FileUtils.getSize(file1);
            }
            if (file0.size == file1.size) {
                return nameFolderFirstComparator.compare(file0, file1);
            } else if (file1.size > file0.size) {
                return 1;
            } else {
                return  -1;
            }
        }
    };

    public static Comparator<FileItem> lastModifiedComparator = new Comparator<FileItem>() {
        @Override
        public int compare(FileItem file0, FileItem file1) {
            final long l = file1.lastModified() - file0.lastModified();
            if (l == 0) {
                return nameFolderFirstComparator.compare(file0, file1);
            } else if (l > 0) {
                return 1;
            } else {
                return -1;
            }
        }
    };
}
