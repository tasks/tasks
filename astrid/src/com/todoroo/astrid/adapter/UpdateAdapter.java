package com.todoroo.astrid.adapter;

import greendroid.widget.AsyncImageView;

import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.data.Update;

/**
 * Adapter for displaying a user's goals as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class UpdateAdapter extends CursorAdapter {

    // --- instance variables

    protected final ListActivity activity;
    private final int resource;
    private final LayoutInflater inflater;

    /**
     * Constructor
     *
     * @param activity
     * @param resource
     *            layout resource to inflate
     * @param c
     *            database cursor
     * @param autoRequery
     *            whether cursor is automatically re-queried on changes
     * @param onCompletedTaskListener
     *            goal listener. can be null
     */
    public UpdateAdapter(ListActivity activity, int resource,
            Cursor c, boolean autoRequery,
            OnCompletedTaskListener onCompletedTaskListener) {
        super(activity, c, autoRequery);
        DependencyInjectionService.getInstance().inject(this);
    
        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    
        this.resource = resource;
        this.activity = activity;
    }

    /* ======================================================================
     * =========================================================== view setup
     * ====================================================================== */

    /** Creates a new view for use in the list view */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup view = (ViewGroup)inflater.inflate(resource, parent, false);

        view.setTag(new Update());

        // populate view content
        bindView(view, context, cursor);

        return view;
    }

    /** Populates a view with content */
    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<Update> cursor = (TodorooCursor<Update>)c;
        Update update = ((Update) view.getTag());
        update.clear();
        update.readFromCursor(cursor);

        setFieldContentsAndVisibility(view, update);
    }

    /** Helper method to set the contents and visibility of each field */
    @SuppressWarnings("nls")
    public synchronized void setFieldContentsAndVisibility(View view, Update update) {
        JSONObject user = ActFmPreferenceService.userFromModel(update);
        Resources r = activity.getResources();

        // picture
        final AsyncImageView pictureView = (AsyncImageView)view.findViewById(R.id.picture); {
            String pictureUrl = user.optString("picture");
            pictureView.setUrl(pictureUrl);
        }

        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            String nameValue = user.optString("name");
            if(update.getValue(Update.ACTION_CODE).equals("task_comment"))
                nameValue = r.getString(R.string.UAd_title_comment, nameValue,
                        update.getValue(Update.TARGET_NAME));
            nameView.setText(nameValue);
        }

        // description
        final TextView descriptionView = (TextView)view.findViewById(R.id.description); {
            String description = update.getValue(Update.ACTION);
            String message = update.getValue(Update.MESSAGE);
            if(update.getValue(Update.ACTION_CODE).equals("task_comment") ||
                    update.getValue(Update.ACTION_CODE).equals("tag_comment"))
                description = message;
            else if(!TextUtils.isEmpty(message))
                description += " " + message;
            descriptionView.setText(description);
        }

        // date
        final TextView date = (TextView)view.findViewById(R.id.date); {
            CharSequence dateString = DateUtils.getRelativeTimeSpanString(update.getValue(Update.CREATION_DATE),
                    DateUtilities.now(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            date.setText(dateString);
        }

    }

}
