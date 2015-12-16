/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.todoroo.andlib.utility.AndroidUtilities;

import org.tasks.R;

import java.io.File;
import java.io.FilenameFilter;

import timber.log.Timber;

public class FilePickerBuilder extends AlertDialog.Builder implements DialogInterface.OnClickListener {

    public interface OnFilePickedListener {
        void onFilePicked(String filePath);
    }

    private OnFilePickedListener onFilePickedListener;
    private File path;
    private String[] files;
    private FilenameFilter filter;

    public FilePickerBuilder(Context ctx, int titleRes, File path, int theme) {
        super(ctx, theme);
        filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String s) {
                File file = new File(dir, s);
                return file.isFile();
            }
        };
        setTitle(ctx.getString(titleRes));
        setPath(path);
    }

    public FilePickerBuilder setOnFilePickedListener(OnFilePickedListener onFilePickedListener) {
        this.onFilePickedListener = onFilePickedListener;
        return this;
    }

    private void setPath(final File path) {
        if (path != null && path.exists()) {
            this.path = path;
            File[] filesAsFile = path.listFiles(filter);
            AndroidUtilities.sortFilesByDateDesc(filesAsFile);

            files = new String[filesAsFile.length];
            for(int i = 0; i < files.length; i++) {
                files[i] = filesAsFile[i].getName();
            }

            setItems(files, this);
        } else {
            Timber.e("Cannot access sdcard.");
            setMessage(R.string.DLG_error_sdcard + "sdcard");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (onFilePickedListener != null) {
            onFilePickedListener.onFilePicked(path.getAbsolutePath() + "/" + files[which]);
        }
    }
}
