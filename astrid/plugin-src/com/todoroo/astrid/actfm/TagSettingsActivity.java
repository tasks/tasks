/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.dao.TagMetadataDao.TagMetadataCriteria;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagMemberMetadata;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.ui.PeopleContainer;
import com.todoroo.astrid.ui.PeopleContainer.ParseSharedException;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.ResourceDrawableCache;
import com.todoroo.astrid.welcome.HelpInfoPopover;

import edu.mit.mobile.android.imagecache.ImageCache;

public class TagSettingsActivity extends SherlockFragmentActivity {

    public static final String TOKEN_NEW_FILTER = "newFilter"; //$NON-NLS-1$

    private static final int MENU_SAVE_ID = R.string.TEA_menu_save;
    private static final int MENU_DISCARD_ID = R.string.TEA_menu_discard;

    public static final int REQUEST_ACTFM_LOGIN = 3;

    public static final String TOKEN_AUTOPOPULATE_MEMBERS = "autopopulateMembers"; //$NON-NLS-1$

    public static final String TOKEN_AUTOPOPULATE_NAME = "autopopulateName"; //$NON-NLS-1$

    private static final String MEMBERS_IN_PROGRESS = "members"; //$NON-NLS-1$

    private TagData tagData;
    private Filter filter; // Used for creating shortcuts, only initialized if necessary

    @Autowired TagService tagService;

    @Autowired TagDataService tagDataService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired ExceptionService exceptionService;

    @Autowired UserDao userDao;

    @Autowired TagMetadataDao tagMetadataDao;

    private PeopleContainer tagMembers;
    private AsyncImageView picture;
    private EditText tagName;
    private EditText tagDescription;
    private CheckBox isSilent;
    private Bitmap setBitmap;
    private final ImageCache imageCache;

    private boolean isNewTag = false;
    private boolean isDialog;

    public TagSettingsActivity() {
        DependencyInjectionService.getInstance().inject(this);
        imageCache = AsyncImageView.getImageCache();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupForDialogOrFullscreen();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tag_settings_activity);

        if (isDialog) {
            LayoutParams params = getWindow().getAttributes();
            params.width = LayoutParams.FILL_PARENT;
            params.height = LayoutParams.WRAP_CONTENT;

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            if ((metrics.widthPixels / metrics.density) >= AndroidUtilities.MIN_TABLET_HEIGHT)
                params.width = (3 * metrics.widthPixels) / 5;
            else if ((metrics.widthPixels / metrics.density) >= AndroidUtilities.MIN_TABLET_WIDTH)
                params.width = (4 * metrics.widthPixels) / 5;
            getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        }

        tagData = getIntent().getParcelableExtra(TagViewFragment.EXTRA_TAG_DATA);
        if (tagData == null) {
            isNewTag = true;
            tagData = new TagData();
            tagData.setValue(TagData.UUID, UUIDHelper.newUUID());
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.header_title_view);
        }

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
                            updateMembers(members, RemoteModel.NO_UUID);
                        }
                    });
                }
            }).start();
        }
        showCollaboratorsPopover();

    }

    private void setupForDialogOrFullscreen() {
        isDialog = AstridPreferences.useTabletLayout(this);
        if (isDialog) {
            setTheme(ThemeService.getDialogTheme());
            if (AndroidUtilities.getSdkVersion() < 14)
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            ThemeService.applyTheme(this);
            ActionBar actionBar = getSupportActionBar();
            if (Preferences.getBoolean(R.string.p_save_and_cancel, false)) {
                if (ThemeService.getTheme() == R.style.Theme_White_Alt)
                    actionBar.setLogo(R.drawable.ic_menu_save_blue_alt);
                else
                    actionBar.setLogo(R.drawable.ic_menu_save);
            } else {
                actionBar.setLogo(null);
            }
        }
    }

    private void showCollaboratorsPopover() {
        if (!Preferences.getBoolean(R.string.p_showed_collaborators_help, false)) {
            View members = findViewById(R.id.members_container);
            HelpInfoPopover.showPopover(this, members, R.string.help_popover_collaborators, null);
            Preferences.setBoolean(R.string.p_showed_collaborators_help, true);
        }
    }

    protected void setUpSettingsPage() {
        if (isDialog) {
            findViewById(R.id.save_and_cancel).setVisibility(View.VISIBLE);
            findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            findViewById(R.id.save).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveSettings();
                }
            });
        }

        tagMembers = (PeopleContainer) findViewById(R.id.members_container);
        tagName = (EditText) findViewById(R.id.tag_name);
        tagDescription = (EditText) findViewById(R.id.tag_description);
        picture = (AsyncImageView) findViewById(R.id.picture);
        isSilent = (CheckBox) findViewById(R.id.tag_silenced);
        isSilent.setChecked(tagData.getFlag(TagData.FLAGS, TagData.FLAG_SILENT));

        Button leaveListButton = (Button) findViewById(R.id.leave_list);
        leaveListButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showDeleteDialog(tagData);
            }
        });
        if (isNewTag) {
            leaveListButton.setVisibility(View.GONE);
        }
        else if (tagData.getValue(TagData.MEMBER_COUNT) > 0) {
            leaveListButton.setText(getString(R.string.tag_leave_button));
        }
        if(actFmPreferenceService.isLoggedIn()) {
            findViewById(R.id.tag_silenced_container).setVisibility(View.VISIBLE);
        }

        picture.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(getResources(), TagService.getDefaultImageIDForTag(tagData.getUuid())));
        picture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ActFmCameraModule.showPictureLauncher(TagSettingsActivity.this, null);
            }
        });

        if (isNewTag) {
            findViewById(R.id.create_shortcut_container).setVisibility(View.GONE);
        } else {
            findViewById(R.id.create_shortcut_container).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filter == null) {
                        filter = TagFilterExposer.filterFromTagData(TagSettingsActivity.this, tagData);
                    }
                    filter.listingIcon = picture.getImageBitmap();
                    FilterListFragment.showCreateShortcutDialog(TagSettingsActivity.this, ShortcutActivity.createIntent(filter), filter);
                }
            });
        }

        refreshSettingsPage();

        String autopopulateMembers = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_MEMBERS);
        if (!TextUtils.isEmpty(autopopulateMembers)) {
            updateMembers(autopopulateMembers, RemoteModel.NO_UUID);
            getIntent().removeExtra(TOKEN_AUTOPOPULATE_MEMBERS);
        }

        String autopopulateName = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_NAME);
        if (!TextUtils.isEmpty(autopopulateName)) {
            tagName.setText(autopopulateName);
            getIntent().removeExtra(TOKEN_AUTOPOPULATE_NAME);
        }
    }

    @SuppressWarnings("nls")
    private void saveSettings() {
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
                service.rename(tagData.getUuid(), newName);
            } else { // Rename list--check for existing name
                newName = service.getTagWithCase(newName);
                tagName.setText(newName);
                if (!newName.equals(oldName)) {
                    tagData.setValue(TagData.NAME, newName);
                    service.rename(tagData.getUuid(), newName);
                } else {
                    nameChanged = false;
                }
            }
        }
        //handles description part
        String newDesc = tagDescription.getText().toString();

        tagData.setValue(TagData.TAG_DESCRIPTION, newDesc);

        if (setBitmap != null) {
            JSONObject pictureJson = RemoteModel.PictureHelper.savePictureJson(this, setBitmap);
            if (pictureJson != null)
                tagData.setValue(TagData.PICTURE, pictureJson.toString());
        }

        JSONArray members;
        try {
            members = tagMembers.parseSharedWithAndTags(this, true).optJSONArray("p");
        } catch (JSONException e) {
            exceptionService.displayAndReportError(this, "save-people", e);
            return;
        } catch (ParseSharedException e) {
            if(e.view != null) {
                e.view.setTextColor(Color.RED);
                e.view.requestFocus();
            }
            DialogUtilities.okDialog(this, e.message, null);
            return;
        }
        if (members == null)
            members = new JSONArray();

        if(members.length() > 0 && !actFmPreferenceService.isLoggedIn()) {
            if(newName.length() > 0 && oldName.length() == 0) {
                tagDataService.save(tagData);
            }

            DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    startActivityForResult(new Intent(TagSettingsActivity.this, ActFmLoginActivity.class),
                            REQUEST_ACTFM_LOGIN);
                }
            };

            DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {

                    tagMembers.removeAllViews();
                    tagMembers.addPerson("", "", false); //$NON-NLS-1$
                }
            };
            DialogUtilities.okCancelCustomDialog(TagSettingsActivity.this, getString(R.string.actfm_EPA_login_button),
                    getString(R.string.actfm_TVA_login_to_share), R.string.actfm_EPA_login_button,
                    R.string.actfm_EPA_dont_share_button, android.R.drawable.ic_dialog_alert,
                    okListener, cancelListener);

            return;

        }

        int oldMemberCount = tagData.getValue(TagData.MEMBER_COUNT);
        if (members.length() > oldMemberCount) {
            StatisticsService.reportEvent(StatisticsConstants.ACTFM_LIST_SHARED);
        }
        tagData.setValue(TagData.MEMBER_COUNT, members.length());
        tagData.setFlag(TagData.FLAGS, TagData.FLAG_SILENT, isSilent.isChecked());

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tagName.getWindowToken(), 0);

        tagDataService.save(tagData);
        tagMetadataDao.synchronizeMembers(tagData, tagData.getValue(TagData.MEMBERS), tagData.getUuid(), members);

        if (isNewTag) {
            setResult(RESULT_OK, new Intent().putExtra(TOKEN_NEW_FILTER,
                    TagFilterExposer.filterFromTagData(TagSettingsActivity.this, tagData)));
        } else {
            setResult(RESULT_OK);
        }

        refreshSettingsPage();
        finish();
    }

    private void saveTagPictureLocally(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            String tagPicture = RemoteModel.PictureHelper.getPictureHash(tagData);
            imageCache.put(tagPicture, bitmap);
            tagData.setValue(TagData.PICTURE, tagPicture);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        finishWithAnimation(!isDialog);
    }

    private void finishWithAnimation(boolean backAnimation) {
        super.finish();
        if (backAnimation) {
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
        }
    }

    private void refreshSettingsPage() {
        tagName.setText(tagData.getValue(TagData.NAME));
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            View customView = ab.getCustomView();
            TextView titleView = (TextView) customView.findViewById(R.id.title);
            if (isNewTag) {
                titleView.setText(getString(R.string.tag_new_list));
            } else {
                titleView.setText(getString(R.string.tag_settings_title));
            }
        } else {
            if (isNewTag) {
                setTitle(getString(R.string.tag_new_list));
            } else {
                setTitle(getString(R.string.tag_settings_title));
            }
        }

        String imageUrl = tagData.getPictureUrl(TagData.PICTURE, RemoteModel.PICTURE_MEDIUM);
        Bitmap imageBitmap = null;
        if (TextUtils.isEmpty(imageUrl))
            imageBitmap = tagData.getPictureBitmap(TagData.PICTURE);

        if (imageBitmap != null)
            picture.setImageBitmap(imageBitmap);
        else
            picture.setUrl(imageUrl);
        if (!isNewTag) {
            ImageView shortcut = (ImageView) findViewById(R.id.create_shortcut);
            shortcut.setImageBitmap(FilterListFragment.superImposeListIcon(this, picture.getImageBitmap(), tagData.getUuid()));
        }

        String peopleJson = tagData.getValue(TagData.MEMBERS);
        updateMembers(peopleJson, tagData.getUuid());

        tagDescription.setText(tagData.getValue(TagData.TAG_DESCRIPTION));

    }

    @SuppressWarnings("nls")
    private void updateMembers(String peopleJson, String tagUuid) {
        tagMembers.removeAllViews();
        JSONArray people = null;
        try {
            people = new JSONArray(peopleJson);
        } catch (JSONException e) {
            if (!RemoteModel.isUuidEmpty(tagUuid)) {
                people = new JSONArray();
                TodorooCursor<User> members = userDao.query(Query.select(User.PROPERTIES)
                        .where(User.UUID.in(Query.select(TagMemberMetadata.USER_UUID)
                                .from(TagMetadata.TABLE).where(Criterion.and(TagMetadataCriteria.byTagAndWithKey(tagUuid, TagMemberMetadata.KEY), TagMetadata.DELETION_DATE.eq(0))))));
                try {
                    User user = new User();
                    for (members.moveToFirst(); !members.isAfterLast(); members.moveToNext()) {
                        user.clear();
                        user.readFromCursor(members);
                        try {
                            JSONObject userJson = new JSONObject();
                            ActFmSyncService.JsonHelper.jsonFromUser(userJson, user);
                            people.put(userJson);
                        } catch (JSONException e2) {
                            //
                        }
                    }
                } finally {
                    members.close();
                }

                TodorooCursor<TagMetadata> emailMembers = tagMetadataDao.query(Query.select(TagMemberMetadata.USER_UUID)
                        .where(Criterion.and(TagMetadataCriteria.byTagAndWithKey(tagUuid, TagMemberMetadata.KEY),
                                TagMetadata.DELETION_DATE.eq(0),
                                TagMemberMetadata.USER_UUID.like("%@%"))));
                try {
                    TagMetadata m = new TagMetadata();
                    for (emailMembers.moveToFirst(); !emailMembers.isAfterLast(); emailMembers.moveToNext()) {
                        m.clear();
                        m.readFromCursor(emailMembers);

                        try {
                            JSONObject userJson = new JSONObject();
                            userJson.put("email", m.getValue(TagMemberMetadata.USER_UUID));
                            people.put(userJson);
                        } catch (JSONException e2) {
                            //
                        }
                    }
                } finally {
                    emailMembers.close();
                }

                User u = userDao.fetch(tagData.getValue(TagData.USER_ID), User.PROPERTIES);
                if (u != null) {
                    try {
                        JSONObject owner = new JSONObject();
                        ActFmSyncService.JsonHelper.jsonFromUser(owner, u);
                        owner.put("owner", true);
                        people.put(owner);
                    } catch (JSONException e2) {
                        //
                    }
                }
            }

        }

        if (people != null) {
            try {
                tagMembers.fromJSONArray(people);
            } catch (JSONException e) {
                Log.e("tag-settings", "Error parsing tag members: " + people, e);
            }
        }

        tagMembers.addPerson("", "", false); //$NON-NLS-1$
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
                setBitmap = bitmap;
                saveTagPictureLocally(bitmap);
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
        if (Preferences.getBoolean(R.string.p_save_and_cancel, false)) {
            item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.TEA_menu_discard);
            item.setIcon(ThemeService.getDrawable(R.drawable.ic_menu_close));
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        if (isDialog) {
            item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.TEA_menu_save);
            item.setIcon(ThemeService.getDrawable(R.drawable.ic_menu_save));
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (tagName.getText().length() == 0) {
            finish();
        } else {
            saveSettings();
        }
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
        case android.R.id.home:
            saveSettings();
            if (!isFinishing())
                finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void showDeleteDialog(TagData td) {
        if(td == null)
            return;

        int string;
        if (td.getValue(TagData.MEMBER_COUNT) > 0)
            string = R.string.DLG_leave_this_shared_tag_question;
        else
            string = R.string.DLG_delete_this_tag_question;


        DialogUtilities.okCancelDialog(this, getString(string, td.getValue(TagData.NAME)),
                new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteTag();
            }
        }, null );
    }

    protected void deleteTag() {
        Intent result = tagService.deleteOrLeaveTag(this, tagData.getValue(TagData.NAME), tagData.getUuid());
        setResult(Activity.RESULT_OK, result);
        finish();
    }

}
