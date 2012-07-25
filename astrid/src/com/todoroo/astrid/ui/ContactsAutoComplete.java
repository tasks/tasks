/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

import com.timsu.astrid.R;

public class ContactsAutoComplete extends AutoCompleteTextView {

    private boolean allowMultiple = false;
    private boolean completeTags = false;

    public ContactsAutoComplete(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);
        this.setThreshold(0);
        this.setUpContacts();
    }

    public ContactsAutoComplete(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.setThreshold(0);
        this.setUpContacts();

        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.ContactsAutoComplete);
        allowMultiple = a.getBoolean(R.styleable.ContactsAutoComplete_allowMultiple, false);
        completeTags = a.getBoolean(R.styleable.ContactsAutoComplete_completeTags, false);
    }

    public ContactsAutoComplete(final Context context) {
        super(context);
        this.setThreshold(0);
        this.setUpContacts();
    }

    // --- comma separating stuff

    private String previous = ""; //$NON-NLS-1$
    private String seperator = ", "; //$NON-NLS-1$
    private ContactListAdapter adapter;

    /**
     * This method filters out the existing text till the separator and launched
     * the filtering process again
     */
    @Override
    protected void performFiltering(final CharSequence text, final int keyCode) {
        String filterText = text.toString().trim();
        if(allowMultiple) {
            previous = filterText.substring(0,
                    filterText.lastIndexOf(getSeperator()) + 1);
            filterText = filterText.substring(filterText.lastIndexOf(getSeperator()) + 1);
        }

        if (!TextUtils.isEmpty(filterText))
            super.performFiltering(filterText, keyCode);
    }

    /**
     * After a selection, capture the new value and append to the existing text
     */
    @Override
    protected void replaceText(final CharSequence text) {
        if(allowMultiple)
            super.replaceText(previous + text + getSeperator());
        else
            super.replaceText(text);
    }

    // --- cursor stuff

    private void setUpContacts() {
        try {
            adapter = new ContactListAdapter((Activity) getContext(), null);
            adapter.setCompleteSharedTags(completeTags);
            setAdapter(adapter);
        } catch (VerifyError ve) {
            adapter = null;
        }
    }

    // --- getters and setters

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public String getSeperator() {
        return seperator;
    }

    public void setCompleteSharedTags(boolean value) {
        completeTags = value;
        if (adapter != null)
            adapter.setCompleteSharedTags(value);
    }

    public void setAllowMultiple(boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
    }

    public void setSeperator(final String seperator) {
        this.seperator = seperator;
    }

}
