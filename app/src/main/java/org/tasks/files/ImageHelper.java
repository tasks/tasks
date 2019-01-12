package org.tasks.files;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import timber.log.Timber;

public class ImageHelper {

  private static int calculateInSampleSize(
      BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize value that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }

    return inSampleSize;
  }

  public static Bitmap sampleBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {
    ContentResolver contentResolver = context.getContentResolver();
    InputStream inputStream;
    try {
      inputStream = contentResolver.openInputStream(uri);
    } catch (FileNotFoundException e) {
      Timber.e(e);
      return null;
    }
    // First decode with inJustDecodeBounds=true to check dimensions
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeStream(inputStream, null, options);

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false;

    try {
      inputStream.close();
    } catch (IOException e) {
      Timber.e(e);
    }

    try {
      inputStream = contentResolver.openInputStream(uri);
      return BitmapFactory.decodeStream(inputStream, null, options);
    } catch (IOException e) {
      Timber.e(e);
      return null;
    } finally {
      try {
        inputStream.close();
      } catch (IOException e) {
        Timber.e(e);
      }
    }
  }
}
