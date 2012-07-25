/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import java.io.File;
import java.io.FilenameFilter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;

@SuppressWarnings("nls")
public class FilePickerBuilder extends AlertDialog.Builder implements DialogInterface.OnClickListener {

    public interface OnFilePickedListener {
        void onFilePicked(String filePath);
    }

    private final OnFilePickedListener callback;
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

            File[] filesAsFile = path.listFiles(filter);
            AndroidUtilities.sortFilesByDateDesc(filesAsFile);

            files = new String[filesAsFile.length];
            for(int i = 0; i < files.length; i++)
                files[i] = filesAsFile[i].getName();

            setItems(files, this);
        } else {
            Log.e("FilePicker", "Cannot access sdcard.");
            setMessage(R.string.DLG_error_sdcard + "sdcard");
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (callback != null) {
            callback.onFilePicked(path + "/" + files[i]);
        }
    }
}
