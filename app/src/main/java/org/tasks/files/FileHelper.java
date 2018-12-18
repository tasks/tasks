package org.tasks.files;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.todoroo.astrid.utility.Constants;

import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class FileHelper {

  public static void newFilePicker(Activity activity, int rc, @Nullable Uri initial, String... mimeTypes) {
    if (atLeastKitKat()) {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      if (initial != null) {
        intent.setData(initial);
      }
      if (mimeTypes.length == 1) {
        intent.setType(mimeTypes[0]);
      } else {
        intent.setType("*/*");
        if (mimeTypes.length > 1) {
          intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
      }
      activity.startActivityForResult(intent, rc);
    } else {
      Intent intent = new Intent(activity, FileExplore.class);
      if (initial != null) {
        intent.putExtra(
            FileExplore.EXTRA_START_PATH,
            new File(initial.getPath()));
      }
      activity.startActivityForResult(intent, rc);
    }
  }

  public static void newDirectoryPicker(Activity activity, int rc, @Nullable Uri initial) {
    if (atLeastLollipop()) {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
      intent.addFlags(
          Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
              | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.putExtra("android.content.extra.SHOW_ADVANCED",true);
      activity.startActivityForResult(intent, rc);
    } else {
      Intent intent = new Intent(activity, FileExplore.class);
      intent.putExtra(FileExplore.EXTRA_DIRECTORY_MODE, true);
      if (initial != null) {
        intent.putExtra(
            FileExplore.EXTRA_START_PATH,
            new File(initial.getPath()));
      }
      activity.startActivityForResult(intent, rc);
    }
  }

  public static InputStream fromUri(Context context, Uri uri) {
    try {
      switch (uri.getScheme()) {
        case "content":
          return context.getContentResolver().openInputStream(uri);
        case "file":
          return new FileInputStream(new File(uri.getPath()));
        default:
          throw new IllegalArgumentException("Unhandled scheme: " + uri.getScheme());
      }
    } catch (FileNotFoundException e) {
      Timber.e(e);
      return null;
    }
  }

  public static String getPathFromUri(Activity activity, Uri uri) {
    String[] projection = {MediaStore.Images.Media.DATA};
    Cursor cursor = activity.managedQuery(uri, projection, null, null, null);

    if (cursor != null) {
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
    } else {
      return uri.getPath();
    }
  }

  public static Intent getReadableActionView(Context context, String path, String type) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    Uri uri =
        FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, new File(path));
    intent.setDataAndType(uri, type);
    grantReadPermissions(context, intent, uri);
    return intent;
  }

  public static void grantReadPermissions(Context context, Intent intent, Uri uri) {
    if (atLeastLollipop()) {
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } else {
      if (atLeastLollipop()) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } else {
        List<ResolveInfo> resolveInfoList =
            context
                .getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfoList) {
          context.grantUriPermission(
              resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
      }
    }
  }

  public static Uri newFile(Context context, Uri destination, String mimeType, String filename)
      throws IOException {
    switch (destination.getScheme()) {
      case "content":
        DocumentFile tree = DocumentFile.fromTreeUri(context, destination);
        DocumentFile f1 = tree.createFile(mimeType, filename);
        if (f1 == null) {
          throw new FileNotFoundException("Failed to create " + filename);
        }
        return f1.getUri();
      case "file":
        File dir = new File(destination.getPath());
        if (!dir.exists() && !dir.mkdirs()) {
          throw new IOException("Failed to create %s" + dir.getAbsolutePath());
        }
        File f2 = new File(dir.getAbsolutePath() + File.separator + filename);
        if (f2.createNewFile()) {
          return Uri.fromFile(f2);
        }
        throw new FileNotFoundException("Failed to create " + filename);
      default:
        throw new IllegalArgumentException("Unknown URI scheme: " + destination.getScheme());
    }
  }
}
