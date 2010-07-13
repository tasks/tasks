package com.timsu.astrid.widget;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.timsu.astrid.R;

public class FilePickerBuilder extends AlertDialog.Builder implements DialogInterface.OnClickListener {

    public interface OnFilePickedListener {
        void onFilePicked(String filePath);
    }

    private OnFilePickedListener callback;
    private String[] files;
    private String path;
    private FilenameFilter filter;

    public FilePickerBuilder(Context ctx, String title, File path, OnFilePickedListener callback) {
        super(ctx);
        filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String s) {
                File file = new File(dir, s);
                return file.isFile();
            }
        };
        this.callback = callback;
        setTitle(title);
        setPath(path);
    }

    public void setFilter(FilenameFilter filter) {
        this.filter = filter;
    }

    private void setPath(File path) {
        if (path != null && path.exists()) {
            this.path = path.getAbsolutePath();
            // Reverse the order of the file list so newest timestamped file is first.
            List<String> fileList = Arrays.asList(path.list(filter));
            Collections.sort(fileList);
            Collections.reverse(fileList);
            files = (String[]) fileList.toArray();
            setItems(files, this);
        } else {
            Log.e("FilePicker", "Cannot access sdcard.");
            setMessage(R.string.error_sdcard + "sdcard");
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (callback != null) {
            callback.onFilePicked(path + "/" + files[i]);
        }
    }
}
