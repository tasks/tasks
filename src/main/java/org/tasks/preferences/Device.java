package org.tasks.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;

import com.google.common.base.Joiner;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static java.util.Arrays.asList;

public class Device {

    private Context context;

    @Inject
    public Device(@ForApplication Context context) {
        this.context = context;
    }

    public boolean isPlayStoreAvailable() {
        try {
            PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
            for (PackageInfo packageInfo : packages) {
                if ("com.google.market".equals(packageInfo.packageName) || "com.android.vending".equals(packageInfo.packageName)) {
                    return true;
                }
            }
        } catch(Exception e) {
            Timber.e(e, e.getMessage());
        }
        return false;
    }

    public boolean hasCamera() {
        return context.getPackageManager().queryIntentActivities(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0).size() > 0;
    }

    public boolean hasGallery() {
        return new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {{
            setType("image/*");
        }}.resolveActivity(context.getPackageManager()) != null;
    }

    public boolean supportsLocationServices() {
        return context.getResources().getBoolean(R.bool.location_enabled);
    }

    public String getDebugInfo() {
        try {
            return Joiner.on("\n").join(asList(
                    "",
                    "----------",
                    "Tasks: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.FLAVOR + " build " + BuildConfig.VERSION_CODE + ")",
                    "Android: " + Build.VERSION.RELEASE + " (" + Build.DISPLAY + ")",
                    "Model: " + Build.MANUFACTURER + " " + Build.MODEL,
                    "Product: " + Build.PRODUCT + " (" + Build.DEVICE + ")",
                    "Kernel: " + System.getProperty("os.version") + " (" + Build.VERSION.INCREMENTAL + ")",
                    "----------",
                    ""
            ));
        } catch(Exception e) {
            Timber.e(e, e.getMessage());
        }
        return "";
    }
}
