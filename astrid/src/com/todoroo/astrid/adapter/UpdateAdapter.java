/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.helper.AsyncImageView;

import edu.mit.mobile.android.imagecache.ImageCache;

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
    private final ImageCache imageCache;
    private final String linkColor;
    private final String fromView;

    public static final String USER_TABLE_ALIAS = "users_join"; //$NON-NLS-1$

    public static final StringProperty USER_PICTURE = User.PICTURE.cloneAs(USER_TABLE_ALIAS, "userPicture"); //$NON-NLS-1$
    private static final StringProperty USER_FIRST_NAME = User.FIRST_NAME.cloneAs(USER_TABLE_ALIAS, "userFirstName"); //$NON-NLS-1$
    private static final StringProperty USER_LAST_NAME = User.LAST_NAME.cloneAs(USER_TABLE_ALIAS, "userLastName"); //$NON-NLS-1$
    private static final StringProperty USER_NAME = User.NAME.cloneAs(USER_TABLE_ALIAS, "userName"); //$NON-NLS-1$

    public static final StringProperty ACTIVITY_TYPE_PROPERTY = new StringProperty(null, "'" + NameMaps.TABLE_ID_USER_ACTIVITY + "' as type");  //$NON-NLS-1$//$NON-NLS-2$
    public static final StringProperty HISTORY_TYPE_PROPERTY = new StringProperty(null, "'" + NameMaps.TABLE_ID_HISTORY + "'");  //$NON-NLS-1$ //$NON-NLS-2$
    public static final StringProperty PADDING_PROPERTY = new StringProperty(null, "'0'"); //$NON-NLS-1$

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

    public static final Property<?>[] HISTORY_PROPERTIES = {
        History.CREATED_AT,
        History.USER_UUID,
        History.COLUMN,
        History.TABLE_ID,
        History.OLD_VALUE,
        History.NEW_VALUE,
        History.TASK,
        History.USER_UUID,
        History.ID,
        HISTORY_TYPE_PROPERTY,
    };


    public static final int TYPE_PROPERTY_INDEX = USER_ACTIVITY_PROPERTIES.length - 1;

    private static final String TARGET_LINK_PREFIX = "$link_"; //$NON-NLS-1$
    private static final Pattern TARGET_LINK_PATTERN = Pattern.compile("\\" + TARGET_LINK_PREFIX + "(\\w*)");  //$NON-NLS-1$//$NON-NLS-2$
    private static final String TASK_LINK_TYPE = "task"; //$NON-NLS-1$

    public static final String FROM_TAG_VIEW = "from_tag"; //$NON-NLS-1$
    public static final String FROM_TASK_VIEW = "from_task"; //$NON-NLS-1$
    public static final String FROM_RECENT_ACTIVITY_VIEW = "from_recent_activity"; //$NON-NLS-1$

    private final User self;

    private final int color;
    private final int grayColor;

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
    public UpdateAdapter(Fragment fragment, int resource,
            Cursor c, boolean autoRequery,
            String fromView) {
        super(fragment.getActivity(), c, autoRequery);
        DependencyInjectionService.getInstance().inject(this);

        linkColor = getLinkColor(fragment);

        inflater = (LayoutInflater) fragment.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        imageCache = AsyncImageView.getImageCache();
        this.fromView = fromView;

        this.resource = resource;
        this.fragment = fragment;
        this.self = getSelfUser();

        TypedValue tv = new TypedValue();
        fragment.getActivity().getTheme().resolveAttribute(R.attr.asTextColor, tv, false);
        color = tv.data;

        fragment.getActivity().getTheme().resolveAttribute(R.attr.asDueDateColor, tv, false);
        grayColor = tv.data;
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
        if (val == null)
            val = ""; //$NON-NLS-1$
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
        public History history = new History();
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

        History history = mh.history;
        boolean isSelf;
        if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(type)) {
            readUserActivityProperties(cursor, update);
            isSelf = Task.USER_ID_SELF.equals(update.getValue(UserActivity.USER_UUID));
        } else {
            readHistoryProperties(cursor, history);
            isSelf = Task.USER_ID_SELF.equals(history.getValue(History.USER_UUID));
        }
        readUserProperties(cursor, user, self, isSelf);

        setFieldContentsAndVisibility(view, update, user, history, type);
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

    public static void readHistoryProperties(TodorooCursor<UserActivity> unionCursor, History history) {
        history.setValue(History.CREATED_AT, unionCursor.getLong(0));
        history.setValue(History.USER_UUID, unionCursor.getString(1));
        history.setValue(History.COLUMN, unionCursor.getString(2));
        history.setValue(History.TABLE_ID, unionCursor.getString(3));
        history.setValue(History.OLD_VALUE, unionCursor.getString(4));
        history.setValue(History.NEW_VALUE, unionCursor.getString(5));
        history.setValue(History.TASK, unionCursor.getString(6));
        history.setValue(History.USER_UUID, unionCursor.getString(7));
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
    public synchronized void setFieldContentsAndVisibility(View view, UserActivity activity, User user, History history, String type) {
        // picture
        if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(type)) {
            setupUserActivityRow(view, activity, user);
        } else if (NameMaps.TABLE_ID_HISTORY.equals(type)) {
            setupHistoryRow(view, history, user);
        }

    }

    private void setupUserActivityRow(View view, UserActivity activity, User user) {
        final AsyncImageView pictureView = (AsyncImageView)view.findViewById(R.id.picture); {
            if (user.containsNonNullValue(USER_PICTURE)) {
                String pictureUrl = user.getPictureUrl(USER_PICTURE, RemoteModel.PICTURE_THUMB);
                pictureView.setUrl(pictureUrl);
            } else {
                pictureView.setUrl(null);
            }
            pictureView.setVisibility(View.VISIBLE);
        }

        final AsyncImageView commentPictureView = (AsyncImageView)view.findViewById(R.id.comment_picture); {
            String pictureThumb = activity.getPictureUrl(UserActivity.PICTURE, RemoteModel.PICTURE_MEDIUM);
            String pictureFull = activity.getPictureUrl(UserActivity.PICTURE, RemoteModel.PICTURE_LARGE);
            Bitmap updateBitmap = null;
            if (TextUtils.isEmpty(pictureThumb))
                updateBitmap = activity.getPictureBitmap(UserActivity.PICTURE);
            setupImagePopupForCommentView(view, commentPictureView, pictureThumb, pictureFull, updateBitmap,
                    activity.getValue(UserActivity.MESSAGE), fragment, imageCache);
        }

        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(getUpdateComment((AstridActivity)fragment.getActivity(), activity, user, linkColor, fromView));
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

    private void setupHistoryRow(View view, History history, User user) {
        final AsyncImageView pictureView = (AsyncImageView)view.findViewById(R.id.picture); {
            if (user.containsNonNullValue(USER_PICTURE)) {
                String pictureUrl = user.getPictureUrl(USER_PICTURE, RemoteModel.PICTURE_THUMB);
                pictureView.setUrl(pictureUrl);
            } else {
                pictureView.setUrl(null);
            }
            pictureView.setVisibility(View.VISIBLE);
        }

        final AsyncImageView commentPictureView = (AsyncImageView)view.findViewById(R.id.comment_picture);
        commentPictureView.setVisibility(View.GONE);

        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(getHistoryComment((AstridActivity) fragment.getActivity(), history, user, linkColor, fromView));
            nameView.setTextColor(grayColor);
        }

        final TextView date = (TextView)view.findViewById(R.id.date); {
            CharSequence dateString = DateUtils.getRelativeTimeSpanString(history.getValue(History.CREATED_AT),
                    DateUtilities.now(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            date.setText(dateString);
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public static void setupImagePopupForCommentView(View view, AsyncImageView commentPictureView, final String pictureThumb, final String pictureFull, final Bitmap updateBitmap,
            final String message, final Fragment fragment, ImageCache imageCache) {
        if ((!TextUtils.isEmpty(pictureThumb) && !"null".equals(pictureThumb)) || updateBitmap != null) { //$NON-NLS-1$
            commentPictureView.setVisibility(View.VISIBLE);
            if (updateBitmap != null)
                commentPictureView.setImageBitmap(updateBitmap);
            else
                commentPictureView.setUrl(pictureThumb);

            if (pictureThumb != null && imageCache.contains(pictureThumb) && updateBitmap == null) {
                try {
                    commentPictureView.setDefaultImageBitmap(imageCache.get(pictureThumb));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }  else if (updateBitmap == null) {
                commentPictureView.setUrl(pictureThumb);
            }

            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog image = new AlertDialog.Builder(fragment.getActivity()).create();
                    AsyncImageView imageView = new AsyncImageView(fragment.getActivity());
                    imageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                    imageView.setDefaultImageResource(android.R.drawable.ic_menu_gallery);
                    if (updateBitmap != null)
                        imageView.setImageBitmap(updateBitmap);
                    else
                        imageView.setUrl(pictureFull);
                    image.setView(imageView);

                    image.setMessage(message);
                    image.setButton(fragment.getString(R.string.DLG_close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
                    image.show();
                }
            });
        } else {
            commentPictureView.setVisibility(View.GONE);
        }
    }

    public static String linkify (String string, String linkColor) {
        return String.format("<font color=%s>%s</font>", linkColor, string);  //$NON-NLS-1$
    }

    public static Spanned getUpdateComment(final AstridActivity context, UserActivity activity, User user, String linkColor, String fromView) {
        String userDisplay;
        if (activity.getValue(UserActivity.USER_UUID).equals(Task.USER_ID_SELF)) {
            userDisplay = context.getString(R.string.update_string_user_self);
        } else if (user == null) {
            userDisplay = context.getString(R.string.ENA_no_user);
        } else {
            userDisplay = user.getDisplayName(USER_NAME, USER_FIRST_NAME, USER_LAST_NAME);
        }
        if (TextUtils.isEmpty(userDisplay))
            userDisplay = context.getString(R.string.ENA_no_user);
        String targetName = activity.getValue(UserActivity.TARGET_NAME);
        String action = activity.getValue(UserActivity.ACTION);
        String message = activity.getValue(UserActivity.MESSAGE);

        int commentResource = 0;
        if (UserActivity.ACTION_TASK_COMMENT.equals(action)) {
            if (fromView.equals(FROM_TASK_VIEW) || TextUtils.isEmpty(targetName))
                commentResource = R.string.update_string_default_comment;
            else
                commentResource = R.string.update_string_task_comment;
        } else if (UserActivity.ACTION_TAG_COMMENT.equals(action)) {
            if (fromView.equals(FROM_TAG_VIEW) || TextUtils.isEmpty(targetName))
                commentResource = R.string.update_string_default_comment;
            else
                commentResource = R.string.update_string_tag_comment;
        }

        if (commentResource == 0)
            return Html.fromHtml(String.format("%s %s", userDisplay, message)); //$NON-NLS-1$

        String original = context.getString(commentResource, userDisplay, targetName, message);
        int taskLinkIndex = original.indexOf(TARGET_LINK_PREFIX);

        if (taskLinkIndex < 0)
            return Html.fromHtml(original);

        String[] components = original.split(" "); //$NON-NLS-1$
        SpannableStringBuilder builder = new SpannableStringBuilder();
        StringBuilder htmlStringBuilder = new StringBuilder();

        for (String comp : components) {
            Matcher m = TARGET_LINK_PATTERN.matcher(comp);
            if (m.find()) {
                builder.append(Html.fromHtml(htmlStringBuilder.toString()));
                htmlStringBuilder.setLength(0);

                String linkType = m.group(1);
                CharSequence link = getLinkSpan(context, activity, targetName, linkColor, linkType);
                if (link != null) {
                    builder.append(link);
                    if (!m.hitEnd()) {
                        builder.append(comp.substring(m.end()));
                    }
                    builder.append(' ');
                }
            } else {
                htmlStringBuilder.append(comp);
                htmlStringBuilder.append(' ');
            }
        }

        if (htmlStringBuilder.length() > 0)
            builder.append(Html.fromHtml(htmlStringBuilder.toString()));

        return builder;
    }

    @SuppressWarnings("nls")
    public static String getHistoryComment(final AstridActivity context, History history, User user, String linkColor, String fromView) {
        boolean hasTask = false;
        JSONArray taskAttrs = null;
        if (!TextUtils.isEmpty(history.getValue(History.TASK))) {
            try {
                taskAttrs = new JSONArray(history.getValue(History.TASK));
                hasTask = true;
            } catch (JSONException e) {
                //
            }
        }

        String item;
        String itemPosessive;
        if (FROM_TASK_VIEW.equals(fromView)) {
            item = context.getString(R.string.history_this_task);
        } else if (hasTask && taskAttrs != null) {
            item = taskAttrs.optString(1);
        } else {
            item = context.getString(R.string.history_this_list);
        }
        itemPosessive = item + "'s";

        String oldValue = history.getValue(History.OLD_VALUE);
        String newValue = history.getValue(History.NEW_VALUE);

        String result = "";
        String column = history.getValue(History.COLUMN);
        try {
            if (History.COL_TAG_ADDED.equals(column) || History.COL_TAG_REMOVED.equals(column)) {
                JSONArray tagObj = new JSONArray(newValue);
                String tagName = tagObj.getString(1);
                if (History.COL_TAG_ADDED.equals(column))
                    result = context.getString(R.string.history_tag_added, item, tagName);
                else
                    result = context.getString(R.string.history_tag_removed, item, tagName);

            } else if (History.COL_ATTACHMENT_ADDED.equals(column) || History.COL_ATTACHMENT_REMOVED.equals(column)) {
                JSONArray attachmentArray = new JSONArray(newValue);
                String attachmentName = attachmentArray.getString(0);
                if (History.COL_ATTACHMENT_ADDED.equals(column))
                    result = context.getString(R.string.history_attach_added, attachmentName, item);
                else
                    result = context.getString(R.string.history_attach_removed, attachmentName, item);
            } else if (History.COL_ACKNOWLEDGED.equals(column)) {
                result = context.getString(R.string.history_acknowledged, item);
            } else if (History.COL_SHARED_WITH.equals(column) || History.COL_UNSHARED_WITH.equals(column)) {
                JSONArray members = new JSONArray(newValue);
                String userId = history.getValue(History.USER_UUID);
                StringBuilder memberList = new StringBuilder();
                for (int i = 0; i < members.length(); i++) {
                    JSONObject m = members.getJSONObject(i);
                    memberList.append(userDisplay(context, userId, m));
                    if (i != members.length() - 1)
                        memberList.append(", ");
                }

                if (History.COL_SHARED_WITH.equals(column))
                    result = context.getString(R.string.history_shared_with, item, memberList);
                else
                    result = context.getString(R.string.history_unshared_with, item, memberList);
            } else if (History.COL_MEMBER_ADDED.equals(column) || History.COL_MEMBER_REMOVED.equals(column)) {
                JSONObject userValue = new JSONObject(newValue);
                if (history.getValue(History.USER_UUID).equals(userValue.optString("id")) && History.COL_MEMBER_REMOVED.equals(column))
                    result = context.getString(R.string.history_left_list, item);
                else {
                    String userDisplay = userDisplay(context, history.getValue(History.USER_UUID), userValue);
                    if (History.COL_MEMBER_ADDED.equals(column))
                        result = context.getString(R.string.history_added_user, userDisplay, item);
                    else
                        result = context.getString(R.string.history_removed_user, userDisplay, item);
                }
            } else if (History.COL_COMPLETED_AT.equals(column)) {
                if (!TextUtils.isEmpty(newValue) && !"null".equals(newValue)) {
                    result = context.getString(R.string.history_completed, item);
                } else {
                    result = context.getString(R.string.history_uncompleted, item);
                }
            } else if (History.COL_DELETED_AT.equals(column)) {
                if (!TextUtils.isEmpty(newValue) && !"null".equals(newValue)) {
                    result = context.getString(R.string.history_deleted, item);
                } else {
                    result = context.getString(R.string.history_undeleted, item);
                }
            } else if (History.COL_IMPORTANCE.equals(column)) {
                int oldPriority = AndroidUtilities.tryParseInt(oldValue, 0);
                int newPriority = AndroidUtilities.tryParseInt(newValue, 0);

                result = context.getString(R.string.history_importance_changed, itemPosessive, priorityString(oldPriority), priorityString(newPriority));
            } else if (History.COL_NOTES_LENGTH.equals(column)) {
                int oldLength = AndroidUtilities.tryParseInt(oldValue, 0);
                int newLength = AndroidUtilities.tryParseInt(newValue, 0);

                if (oldLength > 0 && newLength > oldLength)
                    result = context.getString(R.string.history_added_description_characters, (newLength - oldLength), itemPosessive);
                else if (newLength == 0)
                    result = context.getString(R.string.history_removed_description, itemPosessive);
                else if (oldLength > 0 && newLength < oldLength)
                    result = context.getString(R.string.history_removed_description_characters, (oldLength - newLength), itemPosessive);
                else if (oldLength > 0 && oldLength == newLength)
                    result = context.getString(R.string.history_updated_description, itemPosessive);
            } else if (History.COL_PUBLIC.equals(column)) {
                int value = AndroidUtilities.tryParseInt(newValue, 0);
                if (value > 0)
                    result = context.getString(R.string.history_made_public, item);
                else
                    result = context.getString(R.string.history_made_private, item);
            } else if (History.COL_DUE.equals(column)) {
                if (!TextUtils.isEmpty(oldValue) && !TextUtils.isEmpty(newValue)
                        && !"null".equals(oldValue) && !"null".equals(newValue))
                    result = context.getString(R.string.history_changed_due_date, itemPosessive, dateString(context, oldValue, newValue), dateString(context, newValue, oldValue));
                else if (!TextUtils.isEmpty(newValue) && !"null".equals(newValue))
                    result = context.getString(R.string.history_set_due_date, itemPosessive, dateString(context, newValue, DateUtilities.timeToIso8601(DateUtilities.now(), true)));
                else
                    result = context.getString(R.string.history_removed_due_date, itemPosessive);
            } else if (History.COL_REPEAT.equals(column)) {
                String repeatString = getRepeatString(context, newValue);
                if (!TextUtils.isEmpty(repeatString))
                    result = context.getString(R.string.history_changed_repeat, itemPosessive, repeatString);
                else
                    result = context.getString(R.string.history_removed_repeat, itemPosessive);
            } else if (History.COL_TASK_REPEATED.equals(column)) {
                result = context.getString(R.string.history_completed_repeating_task, item, dateString(context, newValue, oldValue));
            } else if (History.COL_TITLE.equals(column)) {
                if (!TextUtils.isEmpty(oldValue) && !"null".equals(oldValue))
                    result = context.getString(R.string.history_title_changed, itemPosessive, oldValue, newValue);
                else
                    result = context.getString(R.string.history_title_set, itemPosessive, newValue);
            } else if (History.COL_NAME.equals(column)) {
                if (!TextUtils.isEmpty(oldValue) && !"null".equals(oldValue))
                    result = context.getString(R.string.history_name_changed, oldValue, newValue);
                else
                    result = context.getString(R.string.history_name_set, newValue);
            } else if (History.COL_DESCRIPTION.equals(column)) {
                if (!TextUtils.isEmpty(oldValue) && !"null".equals(oldValue))
                    result = context.getString(R.string.history_description_changed, oldValue, newValue);
                else
                    result = context.getString(R.string.history_description_set, newValue);
            } else if (History.COL_PICTURE_ID.equals(column) || History.COL_DEFAULT_LIST_IMAGE_ID.equals(column)) {
                result = context.getString(R.string.history_changed_list_picture);
            } else if (History.COL_IS_SILENT.equals(column)) {
                int value = AndroidUtilities.tryParseInt(newValue, 0);
                if (value > 0)
                    result = context.getString(R.string.history_silenced, item);
                else
                    result = context.getString(R.string.history_unsilenced, item);
            } else if (History.COL_IS_FAVORITE.equals(column)) {
                int value = AndroidUtilities.tryParseInt(newValue, 0);
                if (value > 0)
                    result = context.getString(R.string.history_favorited, item);
                else
                    result = context.getString(R.string.history_unfavorited, item);
            } else if (History.COL_USER_ID.equals(column)) {
                String userId = history.getValue(History.USER_UUID);
                JSONObject userValue = new JSONObject(newValue);
                if (FROM_TAG_VIEW.equals(fromView) && !hasTask) {
                    if (!TextUtils.isEmpty(oldValue) && !"null".equals(oldValue))
                        result = context.getString(R.string.history_changed_list_owner, userDisplay(context, userId, userValue));
                    else
                        result = context.getString(R.string.history_created_this_list);
                } else if (!TextUtils.isEmpty(oldValue) && !"null".equals(oldValue) && Task.USER_ID_UNASSIGNED.equals(userValue))
                    result = context.getString(R.string.history_unassigned, item);
                else if (Task.USER_ID_UNASSIGNED.equals(oldValue) && userValue.optString("id").equals(ActFmPreferenceService.userId()))
                    result = context.getString(R.string.history_claimed, item);
                else if (!TextUtils.isEmpty(oldValue) && !"null".equals(oldValue))
                    result = context.getString(R.string.history_assigned_to, item, userDisplay(context, userId, userValue));
                else if (!userValue.optString("id").equals(ActFmPreferenceService.userId()) && !Task.USER_ID_UNASSIGNED.equals(userValue.optString("id")))
                    result = context.getString(R.string.history_created_for, item, userDisplay(context, userId, userValue));
                else
                    result = context.getString(R.string.history_created, item);
            } else {
                result = context.getString(R.string.history_default, column, newValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = context.getString(R.string.history_default, column, newValue);
        }

        if (TextUtils.isEmpty(result))
            result = context.getString(R.string.history_default, column, newValue);

        String userDisplay;
        if (history.getValue(History.USER_UUID).equals(Task.USER_ID_SELF) || history.getValue(History.USER_UUID).equals(ActFmPreferenceService.userId())) {
            userDisplay = context.getString(R.string.update_string_user_self);
        } else if (user == null) {
            userDisplay = context.getString(R.string.ENA_no_user);
        } else {
            userDisplay = user.getDisplayName(USER_NAME, USER_FIRST_NAME, USER_LAST_NAME);
        }

        return userDisplay + " " + result;
    }

    private static String dateString(Context context, String value, String other) {
        boolean includeYear = (!TextUtils.isEmpty(other) && !value.substring(0, 4).equals(other.substring(0, 4)));
        boolean hasTime = DateUtilities.isoStringHasTime(value);

        long time = 0;
        try {
            time = DateUtilities.parseIso8601(value);
            Date date = new Date(time);
            String result = DateUtilities.getDateString(context, date, includeYear);
            if (hasTime)
                result += ", " + DateUtilities.getTimeString(context, date, false); //$NON-NLS-1$
            return result;
        } catch (ParseException e) {
            return value;
        }

    }

    private static final HashMap<String, Integer> INTERVAL_LABELS = new HashMap<String, Integer>();
    static {
        INTERVAL_LABELS.put("DAILY", R.string.repeat_days); //$NON-NLS-1$
        INTERVAL_LABELS.put("WEEKDAYS", R.string.repeat_weekdays); //$NON-NLS-1$
        INTERVAL_LABELS.put("WEEKLY", R.string.repeat_weeks); //$NON-NLS-1$
        INTERVAL_LABELS.put("MONTHLY", R.string.repeat_months); //$NON-NLS-1$
        INTERVAL_LABELS.put("YEARLY", R.string.repeat_years); //$NON-NLS-1$
        INTERVAL_LABELS.put("HOURLY", R.string.repeat_hours); //$NON-NLS-1$
        INTERVAL_LABELS.put("MINUTELY", R.string.repeat_minutes); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private static final String[] SORTED_WEEKDAYS = { "SU", "MO", "TU", "WE", "TH", "FR", "SA" };

    @SuppressWarnings("nls")
    private static String getRepeatString(Context context, String value) {
        if (TextUtils.isEmpty(value) || "null".equals(value))
            return null;

        try {
            JSONObject repeat = new JSONObject(value);
            boolean weekdays = false;
            if (repeat.has("freq")) {
                String freq = repeat.getString("freq");
                int interval = repeat.getInt("interval");
                JSONArray byDay = repeat.optJSONArray("byday");
                String[] byDayStrings = null;
                if (byDay != null) {
                    byDayStrings = new String[byDay.length()];
                    for (int i = 0; i < byDay.length(); i++) {
                        byDayStrings[i] = byDay.getString(i);
                    }
                }
                String result = "";
                if ("WEEKLY".equals(freq) && byDay != null && byDayStrings != null) {
                    Arrays.sort(byDayStrings);
                    StringBuilder daysString = new StringBuilder();
                    daysString.append("[");
                    for (String s : byDayStrings) {
                        daysString.append("\"").append(s).append("\"").append(",");
                    }
                    daysString.deleteCharAt(daysString.length() - 1);
                    daysString.append("]");

                    if (daysString.toString().equals("[\"FR\",\"MO\",\"TH\",\"TU\",\"WE\"]")) {
                        result = context.getString(R.string.repeat_weekdays);
                        weekdays = true;
                    }
                }

                if (!weekdays) {
                    if (interval == 1) {
                        result = context.getString(INTERVAL_LABELS.get(freq));
                        result = result.substring(0, result.length() - 1);
                    } else {
                        result = interval + " " + context.getString(INTERVAL_LABELS.get(freq));
                    }
                }

                result = context.getString(R.string.history_repeat_every, result);
                if ("WEEKLY".equals(freq) && !weekdays && byDay != null && byDay.length() > 0 && byDayStrings != null) {
                    Arrays.sort(byDayStrings, new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            int lhIndex = AndroidUtilities.indexOf(SORTED_WEEKDAYS, lhs);
                            int rhIndex = AndroidUtilities.indexOf(SORTED_WEEKDAYS, rhs);
                            if (lhIndex < rhIndex)
                                return -1;
                            else if (lhIndex > rhIndex)
                                return 1;
                            else
                                return 0;
                        }
                    });

                    StringBuilder byDayDisplay = new StringBuilder();
                    for (String s : byDayStrings) {
                        byDayDisplay.append(s).append(", ");
                    }
                    byDayDisplay.delete(byDayDisplay.length() - 2, byDayDisplay.length());

                    result += (" " + context.getString(R.string.history_repeat_on, byDayDisplay.toString()));
                }

                if ("COMPLETION".equals(repeat.optString("from")))
                    result += (" " + context.getString(R.string.history_repeat_from_completion));

                return result;
            } else {
                return null;
            }
        } catch (JSONException e) {
            return null;
        }
    }

    @SuppressWarnings("nls")
    private static String userDisplay(Context context, String historyUserId, JSONObject userJson) {
        try {
            String id = userJson.getString("id");
            String name = userJson.getString("name");

            if (historyUserId.equals(id) && ActFmPreferenceService.userId().equals(id))
                return context.getString(R.string.history_yourself);
            else if (ActFmPreferenceService.userId().equals(id))
                return context.getString(R.string.history_you);
            else if (RemoteModel.isValidUuid(id))
                return name;
            else return context.getString(R.string.history_a_deleted_user);
        } catch (JSONException e) {
            return context.getString(R.string.ENA_no_user).toLowerCase();
        }
    }

    @SuppressWarnings("nls")
    private static final String[] PRIORITY_STRINGS = { "!!!", "!!", "!", "o" };
    private static String priorityString(int priority) {
        return PRIORITY_STRINGS[priority];
    }

    private static CharSequence getLinkSpan(final AstridActivity activity, UserActivity update, String targetName, String linkColor, String linkType) {
        if (TASK_LINK_TYPE.equals(linkType)) {
            final String taskId = update.getValue(UserActivity.TARGET_ID);
            if (RemoteModel.isValidUuid(taskId)) {
                SpannableString taskSpan = new SpannableString(targetName);
                taskSpan.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (activity != null) // TODO: This shouldn't happen, but sometimes does
                            activity.onTaskListItemClicked(taskId);
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                    }
                }, 0, targetName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                return taskSpan;
            } else {
                return Html.fromHtml(linkify(targetName, linkColor));
            }
        }
        return null;
    }
}
