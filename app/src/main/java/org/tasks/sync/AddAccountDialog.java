package org.tasks.sync;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import org.tasks.R;
import org.tasks.caldav.CaldavAccountSettingsActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.etesync.EteSyncAccountSettingsActivity;
import org.tasks.preferences.fragments.SynchronizationKt;
import org.tasks.themes.DrawableUtil;

public class AddAccountDialog {
  public static void showAddAccountDialog(Activity activity, DialogBuilder dialogBuilder) {
    String[] services = activity.getResources().getStringArray(R.array.synchronization_services);
    String[] descriptions =
        activity.getResources().getStringArray(R.array.synchronization_services_description);
    TypedArray typedArray =
        activity.getResources().obtainTypedArray(R.array.synchronization_services_icons);
    int[] icons = new int[typedArray.length()];
    for (int i = 0; i < icons.length; i++) {
      icons[i] = typedArray.getResourceId(i, 0);
    }
    typedArray.recycle();
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(
            activity, R.layout.simple_list_item_2_themed, R.id.text1, services) {
          @NonNull
          @Override
          public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.<TextView>findViewById(R.id.text1).setText(services[position]);
            view.<TextView>findViewById(R.id.text2).setText(descriptions[position]);
            ImageView icon = view.findViewById(R.id.image_view);
            icon.setImageDrawable(DrawableUtil.getWrapped(getContext(), icons[position]));
            if (position == 1) {
              icon.getDrawable().setTint(getContext().getColor(R.color.icon_tint));
            }
            return view;
          }
        };

    dialogBuilder
        .newDialog()
        .setTitle(R.string.choose_synchronization_service)
        .setSingleChoiceItems(
            adapter,
            -1,
            (dialog, which) -> {
              switch (which) {
                case 0:
                  activity.startActivityForResult(
                      new Intent(activity, GtasksLoginActivity.class),
                      SynchronizationKt.REQUEST_GOOGLE_TASKS);
                  break;
                case 1:
                  activity.startActivityForResult(
                      new Intent(activity, CaldavAccountSettingsActivity.class),
                      SynchronizationKt.REQUEST_CALDAV_SETTINGS);
                  break;
                case 2:
                  activity.startActivityForResult(
                      new Intent(activity, EteSyncAccountSettingsActivity.class),
                      SynchronizationKt.REQUEST_CALDAV_SETTINGS);
                  break;
              }
              dialog.dismiss();
            })
        .setNeutralButton(
            R.string.help,
            (dialog, which) ->
                activity.startActivity(
                    new Intent(
                        Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.help_url_sync)))))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
