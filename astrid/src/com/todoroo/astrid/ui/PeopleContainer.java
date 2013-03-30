/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.utility.ResourceDrawableCache;

public class PeopleContainer extends LinearLayout {

    private boolean completeTags = false;

    protected OnAddNewPersonListener onAddNewPerson = null;

    protected Resources resources;

    // --- accessors and boilerplate

    public PeopleContainer(Context arg0, AttributeSet attrs) {
        super(arg0, attrs);
        resources = arg0.getResources();
        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.ContactsAutoComplete);
        completeTags = a.getBoolean(R.styleable.ContactsAutoComplete_completeTags, false);
    }

    public PeopleContainer(Context context) {
        super(context);
    }

    public interface OnAddNewPersonListener {
        public void textChanged(String text);
    }

    public void setOnAddNewPerson(OnAddNewPersonListener onAddNewPerson) {
        this.onAddNewPerson = onAddNewPerson;
    }

    // --- methods

    public TextView addPerson() {
        return addPerson("", "", false); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** Adds a tag to the tag field */
    public TextView addPerson(String person, String image, boolean hideRemove) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // check if already exists
        for(int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            TextView matching = (TextView) view.findViewById(R.id.text1);
            if(matching.getText().equals(person))
                return matching;
        }

        final View tagItem = inflater.inflate(R.layout.contact_edit_row, null);
        if(person.length() == 0)
            addView(tagItem, getChildCount());
        else
            addView(tagItem);
        final ContactsAutoComplete textView = (ContactsAutoComplete)tagItem.
            findViewById(R.id.text1);
        textView.setText(person);
        textView.setHint(R.string.actfm_person_hint);

        if(completeTags) {
            textView.setCompleteSharedTags(true);
            textView.setHint(R.string.actfm_person_or_tag_hint);
        }

        final ImageButton removeButton = (ImageButton)tagItem.findViewById(R.id.button1);
        if (hideRemove)
            removeButton.setVisibility(View.GONE);
        else
            removeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    TextView lastView = getLastTextView();
                    if(lastView == textView && textView.getText().length() == 0)
                        return;

                    if(getChildCount() > 1)
                        removeView(tagItem);
                    else {
                        textView.setText(""); //$NON-NLS-1$
                        textView.setEnabled(true);
                    }
                }
            });

        final AsyncImageView imageView = (AsyncImageView)tagItem.
            findViewById(R.id.icon);
        imageView.setUrl(image);
        if (TextUtils.isEmpty(textView.getText())) {
            imageView.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_add_contact));
            removeButton.setVisibility(View.GONE);
        } else {
            imageView.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_default_person_image));
            if (!hideRemove)
                removeButton.setVisibility(View.VISIBLE);
        }


        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                //
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //
            }
            @SuppressWarnings("nls")
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if(count > 0 && getLastTextView() == textView) {
                    addPerson("", "", false);
                }
                if (TextUtils.isEmpty(textView.getText())) {
                    imageView.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_add_contact));
                    removeButton.setVisibility(View.GONE);
                }
                else {
                    imageView.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_default_person_image));
                    removeButton.setVisibility(View.VISIBLE);
                }

                if(onAddNewPerson != null)
                    onAddNewPerson.textChanged(s.toString());
            }
        });

        textView.setOnEditorActionListener(new OnEditorActionListener() {
            @SuppressWarnings("nls")
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
                if(actionId != EditorInfo.IME_NULL)
                    return false;
                if(getLastTextView().getText().length() != 0) {
                    addPerson("", "", false);
                }
                return true;
            }
        });

        return textView;
    }

    /**
     * Get tags container last text view. might be null
     * @return
     */
    private TextView getLastTextView() {
        for(int i = getChildCount() - 1; i >= 0; i--) {
            View lastItem = getChildAt(i);
            TextView lastText = (TextView) lastItem.findViewById(R.id.text1);
            if(lastText.isEnabled())
                return lastText;
        }
        return null;
    }

    public TextView getTextView(int index) {
        View item = getChildAt(index);
        return (TextView) item.findViewById(R.id.text1);
    }

    /**
     *
     * @return json array of people
     */
    public JSONArray toJSONArray() {
        JSONArray people = new JSONArray();
        for(int i = 0; i < getChildCount(); i++) {
            TextView textView = getTextView(i);
            JSONObject person = PeopleContainer.createUserJson(textView);
            if(person != null) {
                String email = person.optString("email"); //$NON-NLS-1$
                if (email.indexOf('@') != -1)
                    people.put(person);
            }
        }
        return people;
    }

    @SuppressWarnings("nls")
    public JSONObject parseSharedWithAndTags(Activity activity, boolean peopleAsJSON) throws
    JSONException, ParseSharedException {
        JSONObject sharedWith = new JSONObject();

        HashSet<String> addedEmails = new HashSet<String>();
        HashSet<Long> addedIds = new HashSet<Long>();
        JSONArray peopleList = new JSONArray();
        for(int i = 0; i < getChildCount(); i++) {
            TextView textView = getTextView(i);
            String text = textView.getText().toString();

            if(text.length() == 0)
                continue;

            if(text.indexOf('@') == -1 && textView.isEnabled())
                throw new ParseSharedException(textView,
                        activity.getString(R.string.actfm_EPA_invalid_email, text));
            if (peopleAsJSON) {
                JSONObject person = PeopleContainer.createUserJson(textView);
                if (person != null) {
                    if (person.optBoolean("owner")) //$NON-NLS-1$
                        continue;
                    String email = person.optString("email");
                    Long id = person.optLong("id", -1);
                    if (!TextUtils.isEmpty(email) && !addedEmails.contains(email)) {
                        addedEmails.add(email);
                        if (id > 0)
                            addedIds.add(id);
                        peopleList.put(person);
                    } else if (id > 0 && !addedIds.contains(id)) {
                        addedIds.add(id);
                        peopleList.put(person);
                    }
                }
            } else if (!addedEmails.contains(text)) {
                addedEmails.add(text);
                peopleList.put(text);
            }
        }
        if(peopleList.length() > 0)
            sharedWith.put("p", peopleList);

        return sharedWith;
    }

    public static class ParseSharedException extends Exception {
        private static final long serialVersionUID = -4135848250086302970L;
        public TextView view;
        public String message;

        public ParseSharedException(TextView view, String message) {
            this.view = view;
            this.message = message;
        }
    }

    /**
     * Add people from JSON Array
     * @param people
     */
    @SuppressWarnings("nls")
    public void fromJSONArray(JSONArray people) throws JSONException {
        for(int i = 0; i < people.length(); i++) {
            JSONObject person = people.getJSONObject(i);
            TextView textView = null;
            String imageURL = person.optString("picture", "");
            boolean owner = person.optBoolean("owner");
            boolean hideRemove = owner;
            String name = "";

            if(person.has("id") && ActFmPreferenceService.userId().equals(person.getString("id"))) {
                name = Preferences.getStringValue(ActFmPreferenceService.PREF_NAME);
                hideRemove = true;
            } else if(!TextUtils.isEmpty(person.optString("name")) && !"null".equals(person.optString("name"))) {
                name = person.getString("name");
            } else if(!TextUtils.isEmpty(person.optString("email")) && !"null".equals(person.optString("email"))) {
                name = person.getString("email");
            }

            if (owner)
                name = name + " " + ContextManager.getString(R.string.actfm_list_owner);

            textView = addPerson(name, imageURL, hideRemove);

            if(textView != null) {
                textView.setTag(person);
                textView.setEnabled(false);
            }
        }
    }

    /**
     * Warning: user json may not have a valid email address.
     * @param textView
     * @return
     */
    @SuppressWarnings("nls")
    public static JSONObject createUserJson(TextView textView) {
        if(textView.isEnabled() == false)
            return (JSONObject) textView.getTag();

        String text = textView.getText().toString().trim();
        if(text.length() == 0)
            return null;

        JSONObject user = new JSONObject();
        int bracket = text.lastIndexOf('<');
        try {
            if(bracket > -1) {
                user.put("name", text.substring(0, bracket - 1).trim());
                user.put("email", text.substring(bracket + 1, text.length() - 1).trim());
            } else {
                user.put("email", text);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return user;
    }

}
