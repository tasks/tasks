/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
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
    private final int resource;
    private final LayoutInflater inflater;
    private final ImageDiskCache imageCache;
    private final String linkColor;
    private final String fromView;

    private static final String TARGET_LINK_PREFIX = "$link_"; //$NON-NLS-1$
    private static final Pattern TARGET_LINK_PATTERN = Pattern.compile("\\" + TARGET_LINK_PREFIX + "(\\w*)");  //$NON-NLS-1$//$NON-NLS-2$
    private static final String TASK_LINK_TYPE = "task"; //$NON-NLS-1$

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

        UserActivity update = mh.activity;
        update.clear();
        update.readFromCursor(cursor);

        User user = mh.user;
        user.clear();
        user.readPropertiesFromCursor(cursor);

        setFieldContentsAndVisibility(view, update, user);
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view, UserActivity activity, User user) {
        // picture
        final AsyncImageView pictureView = (AsyncImageView)view.findViewById(R.id.picture); {
            String pictureUrl = user.getPictureUrl(User.PICTURE, RemoteModel.PICTURE_THUMB);
            pictureView.setUrl(pictureUrl);
        }

        final AsyncImageView commentPictureView = (AsyncImageView)view.findViewById(R.id.comment_picture); {
            String updatePicture = activity.getPictureUrl(UserActivity.PICTURE, RemoteModel.PICTURE_THUMB);
            Bitmap updateBitmap = null;
            if (TextUtils.isEmpty(updatePicture))
                updateBitmap = activity.getPictureBitmap(UserActivity.PICTURE);
            setupImagePopupForCommentView(view, commentPictureView, updatePicture, updateBitmap,
                    activity.getValue(UserActivity.MESSAGE), fragment, imageCache);
        }

        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(getUpdateComment((AstridActivity)fragment.getActivity(), activity, user, linkColor, fromView));
            nameView.setMovementMethod(new LinkMovementMethod());
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

    public static void setupImagePopupForCommentView(View view, AsyncImageView commentPictureView, final String updatePicture, final Bitmap updateBitmap,
            final String message, final Fragment fragment, ImageDiskCache imageCache) {
        if ((!TextUtils.isEmpty(updatePicture) && !"null".equals(updatePicture)) || updateBitmap != null) { //$NON-NLS-1$
            commentPictureView.setVisibility(View.VISIBLE);
            if (updateBitmap != null)
                commentPictureView.setImageBitmap(updateBitmap);
            else
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
                    if (updateBitmap != null)
                        imageView.setImageBitmap(updateBitmap);
                    else
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

    public static Spanned getUpdateComment(final AstridActivity context, UserActivity activity, User user, String linkColor, String fromView) {
        String userDisplay;
        if (activity.getValue(UserActivity.USER_UUID).equals(Task.USER_ID_SELF)) {
            userDisplay = context.getString(R.string.update_string_user_self);
        } else if (user == null) {
            userDisplay = context.getString(R.string.ENA_no_user);
        } else {
            userDisplay = user.getDisplayName();
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
