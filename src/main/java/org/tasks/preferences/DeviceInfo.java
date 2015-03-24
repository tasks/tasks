package org.tasks.preferences;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.api.client.repackaged.com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.BuildConfig;
import org.tasks.injection.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastFroyo;
import static java.util.Arrays.asList;

@Singleton
public class DeviceInfo {

    private static final Logger log = LoggerFactory.getLogger(DeviceInfo.class);

    private Context context;
    private Boolean isPlayStoreAvailable;
    private String debugInfo;

    @Inject
    public DeviceInfo(@ForApplication Context context) {
        this.context = context;
    }

    public boolean isPlayStoreAvailable() {
        if (isPlayStoreAvailable == null) {
            isPlayStoreAvailable = checkForPlayStore();
        }

        return isPlayStoreAvailable;
    }

    public boolean supportsBilling() {
        return atLeastFroyo() && isPlayStoreAvailable();
    }

    public String getDebugInfo() {
        if (debugInfo == null) {
            debugInfo = buildDebugString();
        }

        return debugInfo;
    }

    private String buildDebugString() {
        try {
            return Joiner.on("\n").join(asList(
                    "",
                    "----------",
                    "Tasks: " + BuildConfig.VERSION_NAME + " (build " + BuildConfig.VERSION_CODE + ")",
                    "Android: " + Build.VERSION.RELEASE,
                    "Model: " + Build.MANUFACTURER + " " + Build.MODEL,
                    "Product: " + Build.PRODUCT + " (" + Build.DEVICE + ")",
                    "Kernel: " + System.getProperty("os.version") + " (" + Build.VERSION.INCREMENTAL + ")",
                    "----------",
                    ""
            ));
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    private boolean checkForPlayStore() {
        try {
            PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
            for (PackageInfo packageInfo : packages) {
                if ("com.google.market".equals(packageInfo.packageName) || "com.android.vending".equals(packageInfo.packageName)) {
                    return true;
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
