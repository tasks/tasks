package org.tasks.files;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileHelper {
    public static File getExternalFilesDir(Context context, String type) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String filesDir = Environment.getExternalStorageDirectory() + "/Android/data/" + context.getPackageName() + "/files/";
            return new File(type == null ? filesDir : filesDir + "/" + type);
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

    public static void copyFile(String from, String to) throws IOException {
        FileChannel source = new FileInputStream(from).getChannel();
        FileChannel destination = new FileOutputStream(to).getChannel();
        destination.transferFrom(source, 0, source.size());
        destination.close();
        source.close();
    }
}
