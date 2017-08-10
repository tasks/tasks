package org.tasks.widget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.DefaultFilterProvider;

import javax.inject.Inject;

public class ShortcutConfigActivity extends InjectingAppCompatActivity {

    private static final int REQUEST_FILTER = 1019;

    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject Tracker tracker;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = new Intent(this, FilterSelectionActivity.class);
        intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
        startActivityForResult(intent, REQUEST_FILTER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                tracker.reportEvent(Tracking.Events.WIDGET_ADD, getString(R.string.FSA_label));
                Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
                String filterId = defaultFilterProvider.getFilterPreferenceValue(filter);
                Intent shortcutIntent = TaskIntents.getTaskListByIdIntent(this, filterId);

                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, filter.listingTitle);
                Drawable launcher = ContextCompat.getDrawable(this, R.mipmap.ic_launcher);
                if (launcher instanceof BitmapDrawable) {
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, ((BitmapDrawable) launcher).getBitmap());
                } else if (launcher instanceof AdaptiveIconDrawable) {
                    Bitmap bitmap = Bitmap.createBitmap(launcher.getIntrinsicWidth(), launcher.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    launcher.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    launcher.draw(canvas);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
                }
                intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                setResult(RESULT_OK, intent);
            }

            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
