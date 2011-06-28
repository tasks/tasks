package com.todoroo.astrid.tags;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagService.Tag;

public class TagsPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "tags"; //$NON-NLS-1$

    @Autowired TagDataService tagDataService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Tags", "Todoroo",
                "Provides tagging support for tasks.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }


    /**
     * Create new tag data
     * @param activity
     */
    public void showNewTagDialog(final Activity activity) {
        DependencyInjectionService.getInstance().inject(this);

        FrameLayout frame = new FrameLayout(activity);
        final EditText editText = new EditText(activity);
        frame.addView(editText);
        frame.setPadding(10, 0, 10, 0);

        DialogUtilities.viewDialog(activity,
                activity.getString(R.string.tag_new_list),
                frame, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Tag tag = new Tag(editText.getText().toString(), 0, 0);
                        FilterWithCustomIntent filter = TagFilterExposer.filterFromTag(activity,
                                tag, TaskCriteria.isActive(), actFmPreferenceService.isLoggedIn());
                        filter.start(activity);
                    }
                }, null);
    }

}
