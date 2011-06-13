package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.People;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.TagDataService;

@SuppressWarnings({"nls", "deprecation"})
public class ContactListAdapter extends CursorAdapter {

    @Autowired TagDataService tagDataService;

    private final Activity activity;

    private static final String[] PEOPLE_PROJECTION = new String[] {
        People._ID,
        People.NAME,
        ContactMethods.DATA
    };

    private boolean completeSharedTags = false;

    public ContactListAdapter(Activity activity, Cursor c) {
        super(activity, c);
        this.activity = activity;
        mContent = activity.getContentResolver();
        DependencyInjectionService.getInstance().inject(this);
    }

    public void setCompleteSharedTags(boolean completeSharedTags) {
        this.completeSharedTags = completeSharedTags;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.contact_adapter_row, parent, false);
        bindView(view, context, cursor);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView text = (TextView) view.findViewById(android.R.id.text1);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);

        if(cursor.getColumnNames().length == PEOPLE_PROJECTION.length) {
            text.setText(convertToString(cursor));
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            Uri uri = ContentUris.withAppendedId(People.CONTENT_URI, cursor.getLong(0));
            imageView.setTag(uri);
            ContactImageTask ciTask = new ContactImageTask(imageView);
            ciTask.execute(uri);
        } else {
            int name = cursor.getColumnIndexOrThrow(TagData.NAME.name);
            text.setText(cursor.getString(name));
            imageView.setImageResource(R.drawable.med_tag);
        }
    }

    private class ContactImageTask extends AsyncTask<Uri, Void, Bitmap> {
        private Uri uri;
        private final ImageView imageView;


        public ContactImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            uri = params[0];
            return Contacts.People.loadContactPhoto(activity, uri, 0, null);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled())
                bitmap = null;
            if(imageView != null && uri.equals(imageView.getTag()) && bitmap != null)
                imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public String convertToString(Cursor cursor) {
        if(cursor.getColumnNames().length == PEOPLE_PROJECTION.length) {
            int name = cursor.getColumnIndexOrThrow(People.NAME);
            int email = cursor.getColumnIndexOrThrow(ContactMethods.DATA);
            if(cursor.isNull(name))
                return cursor.getString(email);
            return cursor.getString(name) + " <" + cursor.getString(email) +">";
        } else {
            int name = cursor.getColumnIndexOrThrow(TagData.NAME.name);
            return "#" + cursor.getString(name);
        }
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }

        StringBuilder buffer = new StringBuilder();
        String[] args = null;
        if(constraint != null && constraint.length() > 1) {
            constraint = constraint.toString().trim();
            buffer.append("UPPER(").append(People.NAME).append(") GLOB ?");
            buffer.append(" OR ");
            buffer.append("UPPER(").append(ContactMethods.DATA).append(") GLOB ?");
            args = new String[] { constraint.toString().toUpperCase() + "*",
                    constraint.toString().toUpperCase() + "*" };
        } else {
            buffer.append("0");
        }

        String sort = Contacts.People.DEFAULT_SORT_ORDER + " LIMIT 20";
        Cursor peopleCursor = mContent.query(Contacts.ContactMethods.CONTENT_EMAIL_URI,
                PEOPLE_PROJECTION, buffer.toString(), args,
                sort);
        activity.startManagingCursor(peopleCursor);

        if(!completeSharedTags)
            return peopleCursor;

        Criterion crit = Criterion.all;
        if(constraint != null)
            crit = Functions.upper(TagData.NAME).like("%" + constraint.toString().toUpperCase() + "%");
        else
            crit = Criterion.none;
        Cursor tagCursor = tagDataService.query(Query.select(TagData.ID, TagData.NAME, TagData.PICTURE, TagData.THUMB).
                where(Criterion.and(TagData.USER_ID.eq(0), TagData.MEMBER_COUNT.gt(0),
                        crit)).orderBy(Order.desc(TagData.NAME)));
        activity.startManagingCursor(tagCursor);

        return new MergeCursor(new Cursor[] { tagCursor, peopleCursor });
    }

    private final ContentResolver mContent;

    /**
     * debug method
     */
    public static void makeLotsOfContacts() {
        ContentResolver cr = ContextManager.getContext().getContentResolver();
        ContentValues personValues = new ContentValues();
        ContentValues emailValues = new ContentValues();
        for(int i = 0; i < 2000; i++) {
            personValues.clear();
            personValues.put(Contacts.People.NAME, "John " + i + " Doe");
            Uri newPersonUri = cr.insert(Contacts.People.CONTENT_URI, personValues);
            if (newPersonUri != null) {
                emailValues.clear();
                Uri emailUri = Uri.withAppendedPath(newPersonUri,
                        Contacts.People.ContactMethods.CONTENT_DIRECTORY);
                emailValues.put(Contacts.ContactMethods.KIND,
                        Contacts.KIND_EMAIL);
                emailValues.put(Contacts.ContactMethods.TYPE,
                        Contacts.ContactMethods.TYPE_HOME);
                emailValues.put(Contacts.ContactMethods.DATA,
                    "john." + i + ".doe@test.com");
                cr.insert(emailUri, emailValues);
            }
        }
    }
}