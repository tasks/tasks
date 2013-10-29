/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;

import org.tasks.R;

/**
 * Adapter for displaying a user's activity
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class UpdateAdapter extends CursorAdapter {

    // --- instance variables

    protected final Fragment fragment;
    private final int resource;
    private final LayoutInflater inflater;
    private final String linkColor;
    private final String fromView;

    public static final String USER_TABLE_ALIAS = "users_join"; //$NON-NLS-1$

    public static final StringProperty USER_PICTURE = User.PICTURE.cloneAs(USER_TABLE_ALIAS, "userPicture"); //$NON-NLS-1$
    private static final StringProperty USER_FIRST_NAME = User.FIRST_NAME.cloneAs(USER_TABLE_ALIAS, "userFirstName"); //$NON-NLS-1$
    private static final StringProperty USER_LAST_NAME = User.LAST_NAME.cloneAs(USER_TABLE_ALIAS, "userLastName"); //$NON-NLS-1$
    private static final StringProperty USER_NAME = User.NAME.cloneAs(USER_TABLE_ALIAS, "userName"); //$NON-NLS-1$

    public static final StringProperty ACTIVITY_TYPE_PROPERTY = new StringProperty(null, "'" + NameMaps.TABLE_ID_USER_ACTIVITY + "' as type");  //$NON-NLS-1$//$NON-NLS-2$

    public static final Property<?>[] USER_PROPERTIES = {
        USER_PICTURE,
        USER_FIRST_NAME,
        USER_LAST_NAME,
        USER_NAME
    };

    public static final Property<?>[] USER_ACTIVITY_PROPERTIES = {
        UserActivity.CREATED_AT,
        UserActivity.UUID,
        UserActivity.ACTION,
        UserActivity.MESSAGE,
        UserActivity.TARGET_ID,
        UserActivity.TARGET_NAME,
        UserActivity.PICTURE,
        UserActivity.USER_UUID,
        UserActivity.ID,
        ACTIVITY_TYPE_PROPERTY,
    };

    public static final int TYPE_PROPERTY_INDEX = USER_ACTIVITY_PROPERTIES.length - 1;

    public static final String FROM_TAG_VIEW = "from_tag"; //$NON-NLS-1$
    public static final String FROM_TASK_VIEW = "from_task"; //$NON-NLS-1$
    public static final String FROM_RECENT_ACTIVITY_VIEW = "from_recent_activity"; //$NON-NLS-1$

    private final User self;

    private final int color;

    /**
     * Constructor
     *
     * @param resource
     *            layout resource to inflate
     * @param c
     *            database cursor
     * @param autoRequery
     *            whether cursor is automatically re-queried on changes
     */
    public UpdateAdapter(Fragment fragment, int resource,
            Cursor c, boolean autoRequery,
            String fromView) {
        super(fragment.getActivity(), c, autoRequery);
        DependencyInjectionService.getInstance().inject(this);

        linkColor = getLinkColor(fragment);

        inflater = (LayoutInflater) fragment.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        this.fromView = fromView;

        this.resource = resource;
        this.fragment = fragment;
        this.self = getSelfUser();

        TypedValue tv = new TypedValue();
        fragment.getActivity().getTheme().resolveAttribute(R.attr.asTextColor, tv, false);
        color = tv.data;

        fragment.getActivity().getTheme().resolveAttribute(R.attr.asDueDateColor, tv, false);
    }

    public static User getSelfUser() {
        User self = new User();
        readPreferenceToUser(self, USER_FIRST_NAME, ActFmPreferenceService.PREF_FIRST_NAME);
        readPreferenceToUser(self, USER_LAST_NAME, ActFmPreferenceService.PREF_LAST_NAME);
        readPreferenceToUser(self, USER_NAME, ActFmPreferenceService.PREF_NAME);
        readPreferenceToUser(self, USER_PICTURE, ActFmPreferenceService.PREF_PICTURE);
        return self;
    }

    private static void readPreferenceToUser(User u, StringProperty prop, String prefKey) {
        String val = Preferences.getStringValue(prefKey);
        if (val == null) {
            val = ""; //$NON-NLS-1$
        }
        u.setValue(prop, val);
    }

    public static String getLinkColor(Fragment f) {
        TypedValue colorType = new TypedValue();
        f.getActivity().getTheme().resolveAttribute(R.attr.asDetailsColor, colorType, false);
        return "#" + Integer.toHexString(colorType.data).substring(2); //$NON-NLS-1$
    }

    /* ======================================================================
     * =========================================================== view setup
     * ====================================================================== */

    private class ModelHolder {
        public UserActivity activity = new UserActivity();
        public User user = new User();
    }

    /** Creates a new view for use in the list view */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup view = (ViewGroup)inflater.inflate(resource, parent, false);

        view.setTag(new ModelHolder());

        // populate view content
        bindView(view, context, cursor);

        return view;
    }

    /** Populates a view with content */
    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<UserActivity> cursor = (TodorooCursor<UserActivity>)c;
        ModelHolder mh = ((ModelHolder) view.getTag());

        String type = cursor.getString(TYPE_PROPERTY_INDEX);

        UserActivity update = mh.activity;
        update.clear();

        User user = mh.user;
        user.clear();

        boolean isSelf;
        readUserActivityProperties(cursor, update);
        isSelf = Task.USER_ID_SELF.equals(update.getValue(UserActivity.USER_UUID));
        readUserProperties(cursor, user, self, isSelf);

        setFieldContentsAndVisibility(view, update, type);
    }

    public static void readUserActivityProperties(TodorooCursor<UserActivity> unionCursor, UserActivity activity) {
        activity.setValue(UserActivity.CREATED_AT, unionCursor.getLong(0));
        activity.setValue(UserActivity.UUID, unionCursor.getString(1));
        activity.setValue(UserActivity.ACTION, unionCursor.getString(2));
        activity.setValue(UserActivity.MESSAGE, unionCursor.getString(3));
        activity.setValue(UserActivity.TARGET_ID, unionCursor.getString(4));
        activity.setValue(UserActivity.TARGET_NAME, unionCursor.getString(5));
        activity.setValue(UserActivity.PICTURE, unionCursor.getString(6));
        activity.setValue(UserActivity.USER_UUID, unionCursor.getString(7));
    }

    public static void readUserProperties(TodorooCursor<UserActivity> joinCursor, User user, User self, boolean isSelf) {
        if (isSelf) {
            user.mergeWith(self.getSetValues());
        } else {
            user.setValue(USER_FIRST_NAME, joinCursor.get(USER_FIRST_NAME));
            user.setValue(USER_LAST_NAME, joinCursor.get(USER_LAST_NAME));
            user.setValue(USER_NAME, joinCursor.get(USER_NAME));
            user.setValue(USER_PICTURE, joinCursor.get(USER_PICTURE));
        }

    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view, UserActivity activity, String type) {
        // picture
        if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(type)) {
            setupUserActivityRow(view, activity);
        }
    }

    private void setupUserActivityRow(View view, UserActivity activity) {
        final ImageView commentPictureView = (ImageView)view.findViewById(R.id.comment_picture); {
            String pictureThumb = activity.getPictureUrl(UserActivity.PICTURE, RemoteModel.PICTURE_MEDIUM);
            Bitmap updateBitmap = null;
            if (TextUtils.isEmpty(pictureThumb)) {
                updateBitmap = activity.getPictureBitmap(UserActivity.PICTURE);
            }
            setupImagePopupForCommentView(view, commentPictureView, pictureThumb, updateBitmap,
                    activity.getValue(UserActivity.MESSAGE), fragment);
        }

        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(getUpdateComment(activity));
            nameView.setMovementMethod(new LinkMovementMethod());
            nameView.setTextColor(color);
        }


        // date
        final TextView date = (TextView)view.findViewById(R.id.date); {
            CharSequence dateString = DateUtils.getRelativeTimeSpanString(activity.getValue(UserActivity.CREATED_AT),
                    DateUtilities.now(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            date.setText(dateString);
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public static void setupImagePopupForCommentView(View view, ImageView commentPictureView, final String pictureThumb, final Bitmap updateBitmap,
            final String message, final Fragment fragment) {
        if ((!TextUtils.isEmpty(pictureThumb) && !"null".equals(pictureThumb)) || updateBitmap != null) { //$NON-NLS-1$
            commentPictureView.setVisibility(View.VISIBLE);
            if (updateBitmap != null) {
                commentPictureView.setImageBitmap(updateBitmap);
            }

            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog image = new AlertDialog.Builder(fragment.getActivity()).create();
                    ImageView imageView = new ImageView(fragment.getActivity());
                    imageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    if (updateBitmap != null) {
                        imageView.setImageBitmap(updateBitmap);
                    }
                    image.setView(imageView);

                    image.setMessage(message);
                    image.setButton(fragment.getString(R.string.DLG_close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    image.show();
                }
            });
        } else {
            commentPictureView.setVisibility(View.GONE);
        }
    }

    public static Spanned getUpdateComment(UserActivity activity) {
        String message = activity.getValue(UserActivity.MESSAGE);
        return Html.fromHtml(message);
    }
}
