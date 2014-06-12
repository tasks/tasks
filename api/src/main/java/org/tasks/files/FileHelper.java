package org.tasks.files;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FileHelper {
    public static File getExternalFilesDir(Context context, String type) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String filesDir = Environment.getExternalStorageDirectory() + "/Android/data/" + context.getPackageName() + "/files/";
            return new File(type == null ? filesDir : filesDir + "/" + type);
        }

        return null;
    }
}
