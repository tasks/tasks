package com.todoroo.astrid.adapter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.helper.ImageDiskCache;

/**
 * Adapter for displaying a user's activity
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class UpdateAdapter extends CursorAdapter {

    // --- instance variables

    protected final Fragment fragment;
    protected final AstridActivity activity;
    private final int resource;
    private final LayoutInflater inflater;
    private final ImageDiskCache imageCache;
    private final String linkColor;
    private final String fromView;

    private static final String TARGET_LINK_PREFIX = "$link_"; //$NON-NLS-1$
    private static final Pattern TARGET_LINK_PATTERN = Pattern.compile("\\" + TARGET_LINK_PREFIX + "(\\w*)");  //$NON-NLS-1$//$NON-NLS-2$
    private static final String TASK_LINK_TYPE = "task"; //$NON-NLS-1$

    private static final String UPDATE_FRIENDS = "friends";  //$NON-NLS-1$
    private static final String UPDATE_REQUEST_FRIENDSHIP = "request_friendship"; //$NON-NLS-1$
    private static final String UPDATE_CONFIRMED_FRIENDSHIP = "confirmed_friendship"; //$NON-NLS-1$
    private static final String UPDATE_TASK_CREATED = "task_created"; //$NON-NLS-1$
    private static final String UPDATE_TASK_COMPLETED = "task_completed"; //$NON-NLS-1$
    private static final String UPDATE_TASK_UNCOMPLETED = "task_uncompleted"; //$NON-NLS-1$
    private static final String UPDATE_TASK_TAGGED = "task_tagged"; //$NON-NLS-1$
    private static final String UPDATE_TASK_ASSIGNED = "task_assigned"; //$NON-NLS-1$
    public static final String UPDATE_TASK_COMMENT = "task_comment"; //$NON-NLS-1$
    private static final String UPDATE_TAG_COMMENT = "tag_comment"; //$NON-NLS-1$
    private static final String UPDATE_TAG_CREATED = "tag_created"; //$NON-NLS-1$
    public static final String FROM_TAG_VIEW = "from_tag"; //$NON-NLS-1$
    public static final String FROM_TASK_VIEW = "from_task"; //$NON-NLS-1$
    public static final String FROM_RECENT_ACTIVITY_VIEW = "from_recent_activity"; //$NON-NLS-1$

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
        imageCache = ImageDiskCache.getInstance();
        this.fromView = fromView;

        this.resource = resource;
        this.fragment = fragment;
        this.activity = (AstridActivity) fragment.getActivity();
    }

    public static String getLinkColor(Fragment f) {
        TypedValue colorType = new TypedValue();
        f.getActivity().getTheme().resolveAttribute(R.attr.asDetailsColor, colorType, false);
        return "#" + Integer.toHexString(colorType.data).substring(2); //$NON-NLS-1$
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

        // picture
        final AsyncImageView pictureView = (AsyncImageView)view.findViewById(R.id.picture); {
            String pictureUrl = user.optString("picture");
            pictureView.setUrl(pictureUrl);
        }

        final AsyncImageView commentPictureView = (AsyncImageView)view.findViewById(R.id.comment_picture); {
            final String updatePicture = update.getValue(Update.PICTURE);
            setupImagePopupForCommentView(view, commentPictureView, updatePicture,
                    update.getValue(Update.MESSAGE), fragment, imageCache);
        }

        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(getUpdateComment(activity, update, user, linkColor, fromView));
            nameView.setMovementMethod(new LinkMovementMethod());
        }


        // date
        final TextView date = (TextView)view.findViewById(R.id.date); {
            CharSequence dateString = DateUtils.getRelativeTimeSpanString(update.getValue(Update.CREATION_DATE),
                    DateUtilities.now(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            date.setText(dateString);
        }

    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public static void setupImagePopupForCommentView(View view, AsyncImageView commentPictureView, final String updatePicture,
            final String message, final Fragment fragment, ImageDiskCache imageCache) {
        if (!TextUtils.isEmpty(updatePicture) && !"null".equals(updatePicture)) { //$NON-NLS-1$
            commentPictureView.setVisibility(View.VISIBLE);
            commentPictureView.setUrl(updatePicture);

            if(imageCache.contains(updatePicture)) {
                try {
                    commentPictureView.setDefaultImageBitmap(imageCache.get(updatePicture));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                commentPictureView.setUrl(updatePicture);
            }

            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog image = new AlertDialog.Builder(fragment.getActivity()).create();
                    AsyncImageView imageView = new AsyncImageView(fragment.getActivity());
                    imageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                    imageView.setDefaultImageResource(android.R.drawable.ic_menu_gallery);
                    imageView.setUrl(updatePicture);
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

    @SuppressWarnings("nls")
    public static Spanned getUpdateComment (final AstridActivity activity, Update update, JSONObject user, String linkColor, String fromView) {
        if (user == null) {
            user = ActFmPreferenceService.userFromModel(update);
        }
        JSONObject otherUser = null;
        try {
            otherUser = new JSONObject(update.getValue(Update.OTHER_USER));
        } catch (JSONException e) {
            otherUser = new JSONObject();
        }

        return getUpdateComment(activity, update, update.getValue(Update.ACTION_CODE),
                user.optString("name"), update.getValue(Update.TARGET_NAME),
                update.getValue(Update.MESSAGE), otherUser.optString("name"),
                update.getValue(Update.ACTION), linkColor, fromView);
    }

    public static Spanned getUpdateComment (final AstridActivity activity, Update update, String actionCode, String user, String targetName,
            String message, String otherUser, String action, String linkColor, String fromView) {
        if (TextUtils.isEmpty(user)) {
            user = ContextManager.getString(R.string.ENA_no_user);
        }

        String userLink = linkify(user, linkColor);
        String targetNameLink = linkify(targetName, linkColor);
        String otherUserLink = linkify(otherUser, linkColor);

        int commentResource = 0;
        if (actionCode.equals(UPDATE_FRIENDS)) {
            commentResource = R.string.update_string_friends;
        }
        else if (actionCode.equals(UPDATE_REQUEST_FRIENDSHIP)) {
            commentResource = R.string.update_string_request_friendship;
        }
        else if (actionCode.equals(UPDATE_CONFIRMED_FRIENDSHIP)) {
            commentResource = R.string.update_string_confirmed_friendship;
        }
        else if (actionCode.equals(UPDATE_TASK_CREATED)) {
            if (fromView.equals(FROM_TAG_VIEW))
                commentResource = R.string.update_string_task_created_on_list;
            else if (fromView.equals(FROM_RECENT_ACTIVITY_VIEW))
                commentResource = R.string.update_string_task_created_global;
            else
                commentResource = R.string.update_string_task_created;
        }
        else if (actionCode.equals(UPDATE_TASK_COMPLETED)) {
            commentResource = R.string.update_string_task_completed;
        }
        else if (actionCode.equals(UPDATE_TASK_UNCOMPLETED)) {
            commentResource = R.string.update_string_task_uncompleted;
        }
        else if (actionCode.equals(UPDATE_TASK_TAGGED) && !TextUtils.isEmpty(otherUser)) {
            if (fromView.equals(FROM_TAG_VIEW))
                commentResource = R.string.update_string_task_tagged_list;
            else
                commentResource = R.string.update_string_task_tagged;
        }
        else if (actionCode.equals(UPDATE_TASK_ASSIGNED) && !TextUtils.isEmpty(otherUser)) {
            commentResource = R.string.update_string_task_assigned;
        }
        else if (actionCode.equals(UPDATE_TASK_COMMENT)) {
            if (fromView.equals(FROM_TASK_VIEW) || TextUtils.isEmpty(targetName))
                commentResource = R.string.update_string_default_comment;
            else
                commentResource = R.string.update_string_task_comment;
        }
        else if (actionCode.equals(UPDATE_TAG_COMMENT)) {
            if (fromView.equals(FROM_TAG_VIEW)  || TextUtils.isEmpty(targetName))
                commentResource = R.string.update_string_default_comment;
            else
                commentResource = R.string.update_string_tag_comment;

        }
        else if (actionCode.equals(UPDATE_TAG_CREATED)) {
            if (fromView.equals(FROM_RECENT_ACTIVITY_VIEW))
                commentResource = R.string.update_string_tag_created_global;
            else
                commentResource = R.string.update_string_tag_created;
        }

        if (commentResource == 0) {
            return Html.fromHtml(String.format("%s %s", userLink, action)); //$NON-NLS-1$
        }

        String original = ContextManager.getString(commentResource, userLink, targetNameLink, message, otherUserLink);
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
                CharSequence link = getLinkSpan(activity, update, actionCode, user,
                        targetName, message, otherUser, action, linkColor, linkType);
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

    private static CharSequence getLinkSpan(final AstridActivity activity, Update update, String actionCode, String user, String targetName,
            String message, String otherUser, String action, String linkColor, String linkType) {
        if (TASK_LINK_TYPE.equals(linkType)) {
            long taskId = update.getValue(Update.TASK_LOCAL);
            if (taskId <= 0) {
                Task local = PluginServices.getTaskService().fetchByRemoteId(update.getValue(Update.TASK), Task.ID);
                if (local != null)
                    taskId = local.getId();
            }

            final long taskIdToUse = taskId;

            if (taskId > 0) {
                SpannableString taskSpan = new SpannableString(targetName);
                taskSpan.setSpan(new ClickableSpan() {
                    @Override
                  public void onClick(View widget) {
                      activity.onTaskListItemClicked(taskIdToUse);
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
