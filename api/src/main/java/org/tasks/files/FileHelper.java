package org.tasks.files;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileHelper {
    public static File getExternalFilesDir(Context context, String type) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String directory = String.format("%s/Android/data/%s/files/%s", Environment.getExternalStorageDirectory(), context.getPackageName(), type);
            File file = new File(directory);
            if (file.isDirectory() || file.mkdirs()) {
                return file;
            }
        }

        return null;
    }

    public static String getPathFromUri(Activity activity, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.managedQuery(uri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else {
            return uri.getPath();
        }
    }

    public static void copyFile(InputStream inputStream, String to) throws IOException {
        FileOutputStream fos = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) != -1) {
            fos.write(buf, 0, len);
        }
        fos.close();
    }
}
