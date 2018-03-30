package org.tasks.files;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import com.todoroo.astrid.utility.Constants;
import java.io.File;
import java.util.List;

public class FileHelper {

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
}
