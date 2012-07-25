/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;

public class GtasksListAdder extends Activity {

    @Autowired GtasksPreferenceService gtasksPreferenceService;
    @Autowired GtasksListService gtasksListService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showNewListDialog(this);
    }

    private void showNewListDialog(final Activity activity) {
        DependencyInjectionService.getInstance().inject(this);

        FrameLayout frame = new FrameLayout(activity);
        final EditText editText = new EditText(activity);
        frame.addView(editText);
        frame.setPadding(10, 0, 10, 0);

        DialogInterface.OnClickListener onClick = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (gtasksPreferenceService.isLoggedIn() && ! gtasksPreferenceService.isOngoing()) {
                    final ProgressDialog pd = DialogUtilities.progressDialog(GtasksListAdder.this,
                            GtasksListAdder.this.getString(R.string.gtasks_FEx_creating_list));
                    pd.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String token = gtasksPreferenceService.getToken();
                            try {
                                token = GtasksTokenValidator.validateAuthToken(activity, token);
                                GtasksInvoker service = new GtasksInvoker(token);
                                String title = editText.getText().toString();
                                if (TextUtils.isEmpty(title)) //Don't create a list without a title
                                    return;
                                StoreObject newList = gtasksListService.addNewList(service.createGtaskList(title));
                                if (newList != null) {
                                    FilterWithCustomIntent listFilter = (FilterWithCustomIntent) GtasksFilterExposer.filterFromList(activity, newList);
                                    listFilter.start(activity, 0);
                                }

                            } catch (IOException e) {
                                DialogUtilities.okDialog(activity, activity.getString(R.string.gtasks_FEx_create_list_error), null);
                            } finally {
                                pd.dismiss();
                                finish();
                            }
                        }
                    }).start();
                }
            }
        };

        DialogInterface.OnClickListener onCancel = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        DialogUtilities.viewDialog(activity,
                activity.getString(R.string.gtasks_FEx_create_list_dialog),
                frame, onClick, onCancel);
    }

}
