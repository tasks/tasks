package com.todoroo.astrid.ui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
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
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;

public class PeopleContainer extends LinearLayout {

    private boolean completeTags = false;

    protected OnAddNewPersonListener onAddNewPerson = null;

    // --- accessors and boilerplate

    public PeopleContainer(Context arg0, AttributeSet attrs) {
        super(arg0, attrs);

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

    /** Adds a tag to the tag field */
    public TextView addPerson(String person) {
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
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if(count > 0 && getLastTextView() == textView) {
                    addPerson(""); //$NON-NLS-1$
                }

                if(onAddNewPerson != null)
                    onAddNewPerson.textChanged(s.toString());
            }
        });

        textView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
                if(actionId != EditorInfo.IME_NULL)
                    return false;
                if(getLastTextView().getText().length() != 0) {
                    addPerson(""); //$NON-NLS-1$
                }
                return true;
            }
        });

        ImageButton removeButton = (ImageButton)tagItem.findViewById(R.id.button1);
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

        JSONArray peopleList = new JSONArray();
        for(int i = 0; i < getChildCount(); i++) {
            TextView textView = getTextView(i);
            textView.setTextAppearance(activity, android.R.style.TextAppearance_Medium_Inverse);
            String text = textView.getText().toString();

            if(text.length() == 0)
                continue;

            if(text.indexOf('@') == -1 && textView.isEnabled())
                throw new ParseSharedException(textView,
                        activity.getString(R.string.actfm_EPA_invalid_email, text));
            if (peopleAsJSON) {
                JSONObject person = PeopleContainer.createUserJson(textView);
                if (person != null)
                    peopleList.put(person);
            } else {
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

            if(person.has("id") && person.getLong("id") == ActFmPreferenceService.userId())
                textView = addPerson(Preferences.getStringValue(ActFmPreferenceService.PREF_NAME));
            else if(!TextUtils.isEmpty(person.optString("name")) && !"null".equals(person.optString("name")))
                textView = addPerson(person.getString("name"));
            else if(!TextUtils.isEmpty(person.optString("email")) && !"null".equals(person.optString("email")))
                textView = addPerson(person.getString("email"));

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
