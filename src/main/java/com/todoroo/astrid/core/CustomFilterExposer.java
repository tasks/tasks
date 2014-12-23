/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Exposes Astrid's built in filters to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CustomFilterExposer {

    private static final String TOKEN_FILTER_ID = "id"; //$NON-NLS-1$
    private static final String TOKEN_FILTER_NAME = "name"; //$NON-NLS-1$

    private final StoreObjectDao storeObjectDao;
    private final Context context;

    @Inject
    public CustomFilterExposer(@ForApplication Context context, StoreObjectDao storeObjectDao) {
        this.context = context;
        this.storeObjectDao = storeObjectDao;
    }

    public List<Filter> getFilters() {
        final List<Filter> list = new ArrayList<>();

        storeObjectDao.getSavedFilters(new Callback<StoreObject>() {
            @Override
            public void apply(StoreObject savedFilter) {
                Filter f = SavedFilter.load(savedFilter);

                Intent deleteIntent = new Intent(context, DeleteActivity.class);
                deleteIntent.putExtra(TOKEN_FILTER_ID, savedFilter.getId());
                deleteIntent.putExtra(TOKEN_FILTER_NAME, f.title);
                f.contextMenuLabels = new String[] { context.getString(R.string.BFE_Saved_delete) };
                f.contextMenuIntents = new Intent[] { deleteIntent };
                list.add(f);

            }
        });

        return list;
    }

    /**
     * Simple activity for deleting stuff
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class DeleteActivity extends InjectingActivity {

        @Inject StoreObjectDao storeObjectDao;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(android.R.style.Theme_Dialog);
            super.onCreate(savedInstanceState);

            final long id = getIntent().getLongExtra(TOKEN_FILTER_ID, -1);
            if(id == -1) {
                finish();
                return;
            }
            final String name = getIntent().getStringExtra(TOKEN_FILTER_NAME);

            DialogUtilities.okCancelDialog(this,
                    getString(R.string.DLG_delete_this_item_question, name),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            storeObjectDao.delete(id);
                            setResult(RESULT_OK);
                            finish();
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    });
        }
    }
}
