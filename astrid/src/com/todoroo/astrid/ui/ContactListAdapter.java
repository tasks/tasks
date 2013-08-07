/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.TagDataService;

import org.astrid.R;

import java.io.InputStream;


public class ContactListAdapter extends CursorAdapter {

    @Autowired
    TagDataService tagDataService;

    private static final String[] PEOPLE_PROJECTION = new String[]{
            Email._ID, Email.CONTACT_ID, ContactsContract.Contacts.DISPLAY_NAME, Email.DATA
    };

    public ContactListAdapter(Activity activity, Cursor c) {
        super(activity, c);
        mContent = activity.getContentResolver();
        DependencyInjectionService.getInstance().inject(this);
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
        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        TextView text2 = (TextView) view.findViewById(android.R.id.text2);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);

        if (cursor.getColumnNames().length == PEOPLE_PROJECTION.length) {
            int name = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
            int email = cursor.getColumnIndexOrThrow(Email.DATA);
            if (cursor.isNull(name)) {
                text1.setText(cursor.getString(email));
                text2.setText("");
            } else {
                text1.setText(cursor.getString(name));
                text2.setText(cursor.getString(email));
            }
            imageView.setImageResource(R.drawable.icn_default_person_image);
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cursor.getLong(0));
            imageView.setTag(uri);
            ContactImageTask ciTask = new ContactImageTask(imageView);
            ciTask.execute(uri);
        } else {
            int name = cursor.getColumnIndexOrThrow(TagData.NAME.name);
            text1.setText(cursor.getString(name));
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
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(mContent, uri);
            if (input == null) {
                return null;
            }
            return BitmapFactory.decodeStream(input);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }
            if (imageView != null && uri.equals(imageView.getTag()) && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    @Override
    public String convertToString(Cursor cursor) {
        if (cursor.getColumnIndex(Email.DATA) > -1) {
            int name = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
            int email = cursor.getColumnIndexOrThrow(Email.DATA);
            if (cursor.isNull(name)) {
                return cursor.getString(email);
            }
            return cursor.getString(name) + " <" + cursor.getString(email) + ">";
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

        String filterParams = constraint == null ? "" : Uri.encode(constraint.toString());
        Uri uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, filterParams);
        String sort = Email.TIMES_CONTACTED + " DESC LIMIT 20";

        return mContent.query(uri, PEOPLE_PROJECTION, null, null, sort);
    }

    private final ContentResolver mContent;
}
