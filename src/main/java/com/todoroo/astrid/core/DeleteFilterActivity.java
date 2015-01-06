package com.todoroo.astrid.core;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;

import javax.inject.Inject;

/**
 * Simple activity for deleting stuff
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class DeleteFilterActivity extends InjectingActivity {

    public static final String TOKEN_STORE_OBJECT = "store_object";

    @Inject StoreObjectDao storeObjectDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Dialog);
        super.onCreate(savedInstanceState);

        final long id = getIntent().getLongExtra(CustomFilterExposer.TOKEN_FILTER_ID, -1);
        if (id == -1) {
            finish();
            return;
        }
        final StoreObject storeObject = storeObjectDao.getById(id);
        final Filter filter = SavedFilter.load(storeObject);
        final String name = filter.title;

        DialogUtilities.okCancelDialog(this,
                getString(R.string.DLG_delete_this_item_question, name),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        storeObjectDao.delete(id);
                        setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_FILTER_DELETED) {{
                            putExtra(TOKEN_STORE_OBJECT, storeObject);
                        }});
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
