package com.todoroo.astrid.gtasks;

import java.io.IOException;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.api.GtasksService;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;

public class GtasksListAdder {

    @Autowired GtasksPreferenceService gtasksPreferenceService;
    @Autowired GtasksListService gtasksListService;

    public void showNewListDialog(final Activity activity) {
        DependencyInjectionService.getInstance().inject(this);

        FrameLayout frame = new FrameLayout(activity);
        final EditText editText = new EditText(activity);
        frame.addView(editText);
        frame.setPadding(10, 0, 10, 0);

        DialogUtilities.viewDialog(activity,
                activity.getString(R.string.gtasks_FEx_create_list_dialog),
                frame, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (gtasksPreferenceService.isLoggedIn() && ! gtasksPreferenceService.isOngoing()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String token = gtasksPreferenceService.getToken();
                                    token = GtasksTokenValidator.validateAuthToken(token);
                                    GtasksService service = new GtasksService(token);
                                    try {
                                        String title = editText.getText().toString();
                                        if (TextUtils.isEmpty(title)) //Don't create a list without a title
                                            return;
                                        StoreObject newList = gtasksListService.addNewList(service.createGtaskList(title));
                                        if (newList != null) {
                                            FilterWithCustomIntent listFilter = (FilterWithCustomIntent) GtasksFilterExposer.filterFromList(activity, newList);
                                            listFilter.start(activity);
                                        }

                                    } catch (IOException e) {
                                        DialogUtilities.okDialog(activity, activity.getString(R.string.gtasks_FEx_create_list_error), null);
                                    }
                                }
                            }).start();
                        }
                    }
                }, null);
    }

}
