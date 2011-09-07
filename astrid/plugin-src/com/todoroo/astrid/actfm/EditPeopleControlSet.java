package com.todoroo.astrid.actfm;

import greendroid.widget.AsyncImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService.JsonHelper;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.ui.PeopleContainer;
import com.todoroo.astrid.ui.PeopleContainer.OnAddNewPersonListener;
import com.todoroo.astrid.utility.Flags;

public class EditPeopleControlSet implements TaskEditControlSet {

    public static final String EXTRA_TASK_ID = "task"; //$NON-NLS-1$

    private Task task;

    private final ArrayList<Metadata> nonSharedTags = new ArrayList<Metadata>();

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired ExceptionService exceptionService;

    @Autowired TagDataService tagDataService;

    private final PeopleContainer sharedWithContainer;

    private final CheckBox cbFacebook;

    private final CheckBox cbTwitter;

    private final Spinner assignedSpinner;

    private final EditText assignedCustom;

    private final ArrayList<AssignedToUser> spinnerValues = new ArrayList<AssignedToUser>();

    private final Activity activity;

    private String saveToast = null;

    private final int loginRequestCode;

    static {
        AstridDependencyInjector.initialize();
    }

    // --- UI initialization

    public EditPeopleControlSet(Activity activity, int loginRequestCode) {
        DependencyInjectionService.getInstance().inject(this);
        this.activity = activity;
        this.loginRequestCode = loginRequestCode;

        sharedWithContainer = (PeopleContainer) activity.findViewById(R.id.share_container);
        assignedCustom = (EditText) activity.findViewById(R.id.assigned_custom);
        assignedSpinner = (Spinner) activity.findViewById(R.id.assigned_spinner);
        cbFacebook = (CheckBox) activity.findViewById(R.id.checkbox_facebook);
        cbTwitter = (CheckBox) activity.findViewById(R.id.checkbox_twitter);

        sharedWithContainer.addPerson(""); //$NON-NLS-1$
        setUpListeners();
    }

    @Override
    public void readFromTask(Task sourceTask) {
        task = sourceTask;
        setUpData();
    }

    @SuppressWarnings("nls")
    private void setUpData() {
        try {
            JSONObject sharedWith;
            if(task.getValue(Task.SHARED_WITH).length() > 0)
                sharedWith = new JSONObject(task.getValue(Task.SHARED_WITH));
            else
                sharedWith = new JSONObject();

            cbFacebook.setChecked(sharedWith.optBoolean("fb", false));
            cbTwitter.setChecked(sharedWith.optBoolean("tw", false));

            final ArrayList<JSONObject> sharedPeople = new ArrayList<JSONObject>();
            JSONArray people = sharedWith.optJSONArray("p");
            if(people != null) {
                for(int i = 0; i < people.length(); i++) {
                    String person = people.getString(i);
                    TextView textView = sharedWithContainer.addPerson(person);
                    textView.setEnabled(false);
                    sharedPeople.add(PeopleContainer.createUserJson(textView));
                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<JSONObject> collaborators = new ArrayList<JSONObject>();
                    TodorooCursor<Metadata> tags = TagService.getInstance().getTags(task.getId());
                    try {
                        Metadata metadata = new Metadata();
                        for(tags.moveToFirst(); !tags.isAfterLast(); tags.moveToNext()) {
                            metadata.readFromCursor(tags);
                            final String tag = metadata.getValue(TagService.TAG);
                            TagData tagData = tagDataService.getTag(tag, TagData.MEMBER_COUNT, TagData.MEMBERS, TagData.USER);
                            if(tagData != null && tagData.getValue(TagData.MEMBER_COUNT) > 0) {
                                JSONArray members = new JSONArray(tagData.getValue(TagData.MEMBERS));
                                for(int i = 0; i < members.length(); i++) {
                                    JSONObject user = members.getJSONObject(i);
                                    user.put("tag", tag);
                                    sharedPeople.add(user);
                                    collaborators.add(user);
                                }
                                if(!TextUtils.isEmpty(tagData.getValue(TagData.USER))) {
                                    JSONObject user = new JSONObject(tagData.getValue(TagData.USER));
                                    user.put("tag", tag);
                                    sharedPeople.add(user);
                                    collaborators.add(user);
                                }
                            } else {
                                nonSharedTags.add((Metadata) metadata.clone());
                            }
                        }

                        if(collaborators.size() > 0)
                            buildCollaborators(collaborators);
                        buildAssignedToSpinner(sharedPeople);
                    } catch (JSONException e) {
                        exceptionService.reportError("json-reading-data", e);
                    } finally {
                        tags.close();
                    }
                }
            }).start();

        } catch (JSONException e) {
            exceptionService.reportError("json-reading-data", e);
        }
    }

    @SuppressWarnings("nls")
    private void buildCollaborators(final ArrayList<JSONObject> sharedPeople) throws JSONException {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                HashSet<Long> userIds = new HashSet<Long>();
                LinearLayout collaborators = (LinearLayout) activity.findViewById(R.id.collaborators);

                for(JSONObject person : sharedPeople) {
                    if(person == null)
                        continue;
                    long id = person.optLong("id", -1);
                    if(id == 0 || id == ActFmPreferenceService.userId() || (id > -1 && userIds.contains(id)))
                        continue;
                    userIds.add(id);

                    View contact = activity.getLayoutInflater().inflate(R.layout.contact_adapter_row, collaborators, false);
                    AsyncImageView icon = (AsyncImageView) contact.findViewById(R.id.icon);
                    TextView name = (TextView) contact.findViewById(android.R.id.text1);
                    TextView tag = (TextView) contact.findViewById(android.R.id.text2);

                    icon.setUrl(person.optString("picture"));
                    name.setText(person.optString("name"));
                    name.setTextAppearance(activity, R.style.TextAppearance_Medium);
                    tag.setText(activity.getString(R.string.actfm_EPA_list, person.optString("tag")));
                    tag.setTextAppearance(activity, android.R.style.TextAppearance);

                    collaborators.addView(contact);
                }
            }
        });
    }

    private class AssignedToUser {
        public String label;
        public JSONObject user;

        public AssignedToUser(String label, JSONObject user) {
            super();
            this.label = label;
            this.user = user;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @SuppressWarnings("nls")
    private void buildAssignedToSpinner(ArrayList<JSONObject> sharedPeople) throws JSONException {
        HashSet<Long> userIds = new HashSet<Long>();
        HashSet<String> emails = new HashSet<String>();
        HashMap<String, AssignedToUser> names = new HashMap<String, AssignedToUser>();

        if(task.getValue(Task.USER_ID) != 0) {
            JSONObject user = new JSONObject(task.getValue(Task.USER));
            sharedPeople.add(0, user);
        }

        JSONObject myself = new JSONObject();
        myself.put("id", 0L);
        sharedPeople.add(0, myself);

        // de-duplicate by user id and/or email
        spinnerValues.clear();
        for(int i = 0; i < sharedPeople.size(); i++) {
            JSONObject person = sharedPeople.get(i);
            if(person == null)
                continue;
            long id = person.optLong("id", -1);
            if(id == ActFmPreferenceService.userId() || (id > -1 && userIds.contains(id)))
                continue;
            userIds.add(id);

            String email = person.optString("email");
            if(!TextUtils.isEmpty(email) && emails.contains(email))
                continue;
            emails.add(email);

            String name = person.optString("name");
            if(id == 0)
                name = activity.getString(R.string.actfm_EPA_assign_me);
            AssignedToUser atu = new AssignedToUser(name, person);
            spinnerValues.add(atu);
            if(names.containsKey(name)) {
                AssignedToUser user = names.get(name);
                if(user != null && user.user.has("email")) {
                    user.label += " (" + user.user.optString("email") + ")";
                    names.put(name, null);
                }
                if(!TextUtils.isEmpty("email"))
                    atu.label += " (" + email + ")";
            } else if(TextUtils.isEmpty(name)) {
                if(!TextUtils.isEmpty("email"))
                    atu.label = email;
                else
                    spinnerValues.remove(atu);
            } else
                names.put(name, atu);
        }

        spinnerValues.add(new AssignedToUser(activity.getString(R.string.actfm_EPA_assign_custom), null));

        final ArrayAdapter<AssignedToUser> usersAdapter = new ArrayAdapter<AssignedToUser>(activity,
                android.R.layout.simple_spinner_item, spinnerValues);
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assignedSpinner.setAdapter(usersAdapter);
            }
        });
    }

    private void setUpListeners() {
        final View assignedClear = activity.findViewById(R.id.assigned_clear);

        assignedSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v,
                    int index, long id) {
                if(index == spinnerValues.size() - 1) {
                    assignedCustom.setVisibility(View.VISIBLE);
                    assignedClear.setVisibility(View.VISIBLE);
                    assignedSpinner.setVisibility(View.GONE);
                    assignedCustom.requestFocus();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //
            }
        });

        assignedClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                assignedCustom.setVisibility(View.GONE);
                assignedClear.setVisibility(View.GONE);
                assignedCustom.setText(""); //$NON-NLS-1$
                assignedSpinner.setVisibility(View.VISIBLE);
                assignedSpinner.setSelection(0);
            }
        });

        sharedWithContainer.setOnAddNewPerson(new OnAddNewPersonListener() {
            @Override
            public void textChanged(String text) {
                activity.findViewById(R.id.share_additional).setVisibility(View.VISIBLE);
                if(text.indexOf('@') > -1) {
                    activity.findViewById(R.id.tag_label).setVisibility(View.VISIBLE);
                    activity.findViewById(R.id.tag_name).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // --- events

    @Override
    public String writeToModel(Task model) {
        // do nothing, we use a separate method
        return null;
    }

    /**
     * Save sharing settings
     * @param toast toast to show after saving is finished
     * @return false if login is required & save should be halted
     */
    @SuppressWarnings("nls")
    public boolean saveSharingSettings(String toast) {
        if(task == null)
            return false;

        saveToast = toast;
        boolean dirty = false;
        try {
            JSONObject userJson = null;
            if(assignedCustom.getVisibility() == View.VISIBLE)
                userJson = PeopleContainer.createUserJson(assignedCustom);
            else if(assignedSpinner.getSelectedItem() != null)
                userJson = ((AssignedToUser) assignedSpinner.getSelectedItem()).user;

            if(userJson == null || userJson.optLong("id", -1) == 0) {
                dirty = task.getValue(Task.USER_ID) == 0L ? dirty : true;
                task.setValue(Task.USER_ID, 0L);
                if(!TextUtils.isEmpty(task.getValue(Task.USER)))
                    task.setValue(Task.USER, "{}");
            } else {
                String user = userJson.toString();
                dirty = task.getValue(Task.USER).equals(user) ? dirty : true;
                task.setValue(Task.USER_ID, userJson.optLong("id", -1));
                task.setValue(Task.USER, user);
            }

            JSONObject sharedWith = parseSharedWithAndTags();
            dirty = sharedWith.has("p");
            if(!TextUtils.isEmpty(task.getValue(Task.SHARED_WITH)) || sharedWith.length() != 0)
                task.setValue(Task.SHARED_WITH, sharedWith.toString());

            if(dirty)
                taskService.save(task);

            if(dirty && !actFmPreferenceService.isLoggedIn()) {
                activity.startActivityForResult(new Intent(activity, ActFmLoginActivity.class),
                        loginRequestCode);
                return false;
            }

            if(dirty)
                shareTask(sharedWith);
            else
                showSaveToast();

            return true;
        } catch (JSONException e) {
            exceptionService.displayAndReportError(activity, "save-people", e);
        } catch (ParseSharedException e) {
            e.view.setTextColor(Color.RED);
            e.view.requestFocus();
            DialogUtilities.okDialog(activity, e.message, null);
        }
        return false;
    }

    private void showSaveToast() {
        int length = saveToast.indexOf('\n') > -1 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(activity, saveToast, length).show();
    }

    private class ParseSharedException extends Exception {
        private static final long serialVersionUID = -4135848250086302970L;
        public TextView view;
        public String message;

        public ParseSharedException(TextView view, String message) {
            this.view = view;
            this.message = message;
        }
    }

    @SuppressWarnings("nls")
    private JSONObject parseSharedWithAndTags() throws
            JSONException, ParseSharedException {
        JSONObject sharedWith = new JSONObject();
        if(cbFacebook.isChecked())
            sharedWith.put("fb", true);
        if(cbTwitter.isChecked())
            sharedWith.put("tw", true);

        JSONArray peopleList = new JSONArray();
        for(int i = 0; i < sharedWithContainer.getChildCount(); i++) {
            TextView textView = sharedWithContainer.getTextView(i);
            textView.setTextAppearance(activity, android.R.style.TextAppearance_Medium_Inverse);
            String text = textView.getText().toString();

            if(text.length() == 0)
                continue;

            if(text.indexOf('@') == -1)
                throw new ParseSharedException(textView,
                        activity.getString(R.string.actfm_EPA_invalid_email, text));
            peopleList.put(text);
        }
        if(peopleList.length() > 0)
            sharedWith.put("p", peopleList);

        return sharedWith;
    }

    @SuppressWarnings("nls")
    private void shareTask(final JSONObject sharedWith) {
        final JSONArray emails = sharedWith.optJSONArray("p");

        final ProgressDialog pd = DialogUtilities.progressDialog(activity,
                activity.getString(R.string.DLG_please_wait));
        new Thread() {
            @Override
            public void run() {
                ActFmInvoker invoker = new ActFmInvoker(actFmPreferenceService.getToken());
                try {
                    if(task.getValue(Task.REMOTE_ID) == 0) {
                        actFmSyncService.pushTask(task.getId());
                        task.setValue(Task.REMOTE_ID, taskService.fetchById(task.getId(),
                                Task.REMOTE_ID).getValue(Task.REMOTE_ID));
                    }
                    if(task.getValue(Task.REMOTE_ID) == 0) {
                        DialogUtilities.okDialog(activity, "We had an error saving " +
                                "this task to Astrid.com. Could you let us know why this happened?", null);
                        return;
                    }

                    Object[] args = buildSharingArgs(emails);
                    JSONObject result = invoker.invoke("task_share", args);

                    sharedWith.remove("p");
                    task.setValue(Task.SHARED_WITH, sharedWith.toString());
                    task.setValue(Task.DETAILS_DATE, 0L);

                    readTagData(result.getJSONArray("tags"));
                    JsonHelper.readUser(result.getJSONObject("assignee"),
                            task, Task.USER_ID, Task.USER);
                    Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                    taskService.save(task);

                    int count = result.optInt("shared", 0);
                    if(count > 0) {
                        saveToast += "\n" +
                            activity.getString(R.string.actfm_EPA_emailed_toast,
                            activity.getResources().getQuantityString(R.plurals.Npeople, count, count));
                        StatisticsService.reportEvent(StatisticsConstants.ACTFM_TASK_SHARED);
                    }

                    Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
                    ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

                    DialogUtilities.dismissDialog(activity, pd);
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            showSaveToast();
                            activity.finish();
                        }
                    });
                } catch (IOException e) {
                    DialogUtilities.okDialog(activity,
                            activity.getString(R.string.SyP_ioerror),
                            android.R.drawable.ic_dialog_alert, e.toString(), null);
                } catch (JSONException e) {
                    DialogUtilities.okDialog(activity,
                            activity.getString(R.string.SyP_ioerror),
                            android.R.drawable.ic_dialog_alert, e.toString(), null);
                } finally {
                    DialogUtilities.dismissDialog(activity, pd);
                }
            }

        }.start();
    }

    @SuppressWarnings("nls")
    private void readTagData(JSONArray tags) throws JSONException {
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        for(int i = 0; i < tags.length(); i++) {
            JSONObject tagObject = tags.getJSONObject(i);
            TagData tagData = tagDataService.getTag(tagObject.getString("name"), TagData.ID);
            if(tagData == null)
                tagData = new TagData();
            ActFmSyncService.JsonHelper.tagFromJson(tagObject, tagData);
            tagDataService.save(tagData);

            Metadata tagMeta = new Metadata();
            tagMeta.setValue(Metadata.KEY, TagService.KEY);
            tagMeta.setValue(TagService.TAG, tagData.getValue(TagData.NAME));
            tagMeta.setValue(TagService.REMOTE_ID, tagData.getValue(TagData.REMOTE_ID));
            metadata.add(tagMeta);
        }

        metadataService.synchronizeMetadata(task.getId(), metadata, MetadataCriteria.withKey(TagService.KEY));
    }

    @SuppressWarnings("nls")
    protected Object[] buildSharingArgs(JSONArray emails) throws JSONException {
        ArrayList<Object> values = new ArrayList<Object>();
        long currentTaskID = task.getValue(Task.REMOTE_ID);
        values.add("id");
        values.add(currentTaskID);

        if(emails != null) {
            for(int i = 0; i < emails.length(); i++) {
                String email = emails.optString(i);
                if(email == null || email.indexOf('@') == -1)
                    continue;
                values.add("emails[]");
                values.add(email);
            }
        }

        values.add("assignee");
        if(task.getValue(Task.USER_ID) == 0) {
            values.add("");
        } else {
            if(task.getValue(Task.USER_ID) > 0)
                values.add(task.getValue(Task.USER_ID));
            else {
                JSONObject user = new JSONObject(task.getValue(Task.USER));
                values.add(user.getString("email"));
            }
        }

        String message = ((TextView) activity.findViewById(R.id.message)).getText().toString();
        if(!TextUtils.isEmpty(message) && activity.findViewById(R.id.share_additional).getVisibility() == View.VISIBLE) {
            values.add("message");
            values.add(message);
        }

        String tag = ((TextView) activity.findViewById(R.id.tag_name)).getText().toString();
        if(!TextUtils.isEmpty(tag)) {
            values.add("tag");
            values.add(tag);
        }

        return values.toArray(new Object[values.size()]);
    }

    /** Resume save
     * @param data */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == loginRequestCode && resultCode == Activity.RESULT_OK)
            saveSharingSettings(saveToast);
    }
}
