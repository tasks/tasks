package com.todoroo.astrid.actfm;

import greendroid.widget.AsyncImageView;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.ui.PeopleContainer;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.welcome.HelpInfoPopover;

public class TagSettingsActivity extends FragmentActivity {

    private static final int MENU_SAVE_ID = R.string.TEA_menu_save;
    private static final int MENU_DISCARD_ID = R.string.TEA_menu_discard;

    protected static final int REQUEST_ACTFM_LOGIN = 3;

    private static final String MEMBERS_IN_PROGRESS = "members"; //$NON-NLS-1$

    private TagData tagData;

    @Autowired TagDataService tagDataService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    private PeopleContainer tagMembers;
    private AsyncImageView picture;
    private EditText tagName;
    private EditText tagDescription;
    private CheckBox isSilent;

    boolean isNewTag = false;

    public TagSettingsActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tag_settings_activity);
        tagData = getIntent().getParcelableExtra(TagViewActivity.EXTRA_TAG_DATA);
        if (tagData == null) {
            isNewTag = true;
            tagData = new TagData();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.header_title_view);

        setUpSettingsPage();

        if(savedInstanceState != null && savedInstanceState.containsKey(MEMBERS_IN_PROGRESS)) {
            final String members = savedInstanceState.getString(MEMBERS_IN_PROGRESS);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AndroidUtilities.sleepDeep(500);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateMembers(members);
                        }
                    });
                }
            }).start();
        }
        showCollaboratorsPopover();

    }

    private void showCollaboratorsPopover() {
        if (!Preferences.getBoolean(R.string.p_showed_collaborators_help, false)) {
            View members = findViewById(R.id.members_container);
            HelpInfoPopover.showPopover(this, members, R.string.help_popover_collaborators, null);
            Preferences.setBoolean(R.string.p_showed_collaborators_help, true);
        }
    }

    protected void setUpSettingsPage() {
        tagMembers = (PeopleContainer) findViewById(R.id.members_container);
        tagName = (EditText) findViewById(R.id.tag_name);
        tagDescription = (EditText) findViewById(R.id.tag_description);
        picture = (AsyncImageView) findViewById(R.id.picture);
        isSilent = (CheckBox) findViewById(R.id.tag_silenced);
        isSilent.setChecked(tagData.getFlag(TagData.FLAGS, TagData.FLAG_SILENT));

        if(actFmPreferenceService.isLoggedIn()) {
            picture.setVisibility(View.VISIBLE);
            findViewById(R.id.picture_label).setVisibility(View.VISIBLE);
            findViewById(R.id.listSettingsMore).setVisibility(View.VISIBLE);
        }

        picture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ActFmCameraModule.showPictureLauncher(TagSettingsActivity.this, null);
            }
        });

        refreshSettingsPage();
    }

    private void saveSettings() {
        setResult(RESULT_OK);
        String oldName = tagData.getValue(TagData.NAME);
        String newName = tagName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            return;
        }

        boolean nameChanged = !oldName.equals(newName);
        TagService service = TagService.getInstance();
        if (nameChanged) {
            if (oldName.equalsIgnoreCase(newName)) { // Change the capitalization of a list manually
                tagData.setValue(TagData.NAME, newName);
                service.renameCaseSensitive(oldName, newName);
                tagData.setFlag(TagData.FLAGS, TagData.FLAG_EMERGENT, false);
            } else { // Rename list--check for existing name
                newName = service.getTagWithCase(newName);
                tagName.setText(newName);
                if (!newName.equals(oldName)) {
                    tagData.setValue(TagData.NAME, newName);
                    service.rename(oldName, newName);
                    tagData.setFlag(TagData.FLAGS, TagData.FLAG_EMERGENT, false);
                } else {
                    nameChanged = false;
                }
            }
        }
        //handles description part
        String newDesc = tagDescription.getText().toString();

        tagData.setValue(TagData.TAG_DESCRIPTION, newDesc);

        JSONArray members = tagMembers.toJSONArray();

        if(members.length() > 0 && !actFmPreferenceService.isLoggedIn()) {
            if(newName.length() > 0 && oldName.length() == 0) {
                tagDataService.save(tagData);
            }
            startActivityForResult(new Intent(this, ActFmLoginActivity.class),
                        REQUEST_ACTFM_LOGIN);
            return;
        }

        int oldMemberCount = tagData.getValue(TagData.MEMBER_COUNT);
        if (members.length() > oldMemberCount) {
            StatisticsService.reportEvent(StatisticsConstants.ACTFM_LIST_SHARED);
        }
        tagData.setValue(TagData.MEMBERS, members.toString());
        tagData.setValue(TagData.MEMBER_COUNT, members.length());
        tagData.setFlag(TagData.FLAGS, TagData.FLAG_SILENT, isSilent.isChecked());

        if(actFmPreferenceService.isLoggedIn())
            Flags.set(Flags.TOAST_ON_SAVE);
        else
            Toast.makeText(this, R.string.tag_list_saved, Toast.LENGTH_LONG).show();

        tagDataService.save(tagData);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tagName.getWindowToken(), 0);

        if (isNewTag) {
            Intent intent = new Intent(this, TagViewWrapperActivity.class);
            intent.putExtra(TagViewActivity.EXTRA_TAG_NAME, newName);
            intent.putExtra(TagViewActivity.TOKEN_FILTER, TagFilterExposer.filterFromTagData(this, tagData));
            super.finish();
            startActivity(intent);
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
            return;
        }



        refreshSettingsPage();
        finish();
    }

    @Override
    public void finish() {
        finishWithAnimation(true);
    }

    private void finishWithAnimation(boolean backAnimation) {
        super.finish();
        if (backAnimation) {
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
        } else {
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
        }
    }

    @SuppressWarnings("nls")
    private void refreshSettingsPage() {
        tagName.setText(tagData.getValue(TagData.NAME));
        ActionBar ab = getSupportActionBar();
        View customView = ab.getCustomView();
        TextView titleView = (TextView) customView.findViewById(R.id.title);
        if (isNewTag) {
            titleView.setText(getString(R.string.tag_new_list));
        } else {
            titleView.setText(this.getString(R.string.tag_settings_title, tagData.getValue(TagData.NAME)));
        }
        picture.setUrl(tagData.getValue(TagData.PICTURE));
        setTitle(tagData.getValue(TagData.NAME));

        TextView ownerLabel = (TextView) findViewById(R.id.tag_owner);
        try {
            if(tagData.getFlag(TagData.FLAGS, TagData.FLAG_EMERGENT)) {
                ownerLabel.setText(String.format("<%s>", getString(R.string.actfm_TVA_tag_owner_none)));
            } else if(tagData.getValue(TagData.USER_ID) == 0) {
                ownerLabel.setText(Preferences.getStringValue(ActFmPreferenceService.PREF_NAME));
            } else {
                JSONObject owner = new JSONObject(tagData.getValue(TagData.USER));
                ownerLabel.setText(owner.getString("name"));
            }
        } catch (JSONException e) {
            Log.e("tag-view-activity", "json error refresh owner", e);
            ownerLabel.setText("<error>");
        }

        String peopleJson = tagData.getValue(TagData.MEMBERS);
        updateMembers(peopleJson);

        tagDescription.setText(tagData.getValue(TagData.TAG_DESCRIPTION));

    }

    @SuppressWarnings("nls")
    private void updateMembers(String peopleJson) {
        tagMembers.removeAllViews();
        if(!TextUtils.isEmpty(peopleJson)) {
            try {
                JSONArray people = new JSONArray(peopleJson);
                tagMembers.fromJSONArray(people);
            } catch (JSONException e) {
                Log.e("tag-view-activity", "json error refresh members - " + peopleJson, e);
            }
        }

        tagMembers.addPerson(""); //$NON-NLS-1$
    }

    private void uploadTagPicture(final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = actFmSyncService.setTagPicture(tagData.getValue(TagData.REMOTE_ID), bitmap);
                    tagData.setValue(TagData.PICTURE, url);
                    Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                    tagDataService.save(tagData);
                } catch (IOException e) {
                    DialogUtilities.okDialog(TagSettingsActivity.this, e.toString(), null);
                }
            }
        }).start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(tagMembers.getChildCount() > 1) {
            JSONArray members = tagMembers.toJSONArray();
            outState.putString(MEMBERS_IN_PROGRESS, members.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        CameraResultCallback callback = new CameraResultCallback() {
            @Override
            public void handleCameraResult(Bitmap bitmap) {
                picture.setImageBitmap(bitmap);
                uploadTagPicture(bitmap);
            }
        };
        if (ActFmCameraModule.activityResult(this, requestCode, resultCode, data, callback)) {
            // Handled
        } else if(requestCode == REQUEST_ACTFM_LOGIN && resultCode == Activity.RESULT_OK) {
            saveSettings();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item;
        item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.TEA_menu_discard);
        item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.TEA_menu_save);
        item.setIcon(android.R.drawable.ic_menu_save);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case MENU_DISCARD_ID:
            finish();
            break;
        case MENU_SAVE_ID:
            saveSettings();
            break;
        }
        return super.onOptionsItemSelected(item);
    }







}
