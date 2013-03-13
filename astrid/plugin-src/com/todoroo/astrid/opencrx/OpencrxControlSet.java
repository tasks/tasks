/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.opencrx;

import java.util.ArrayList;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.ui.PopupControlSet;

/**
 * Control Set for managing contact/creator assignments in OpenCRX
 *
 * @author Andrey Marchenko <igendou@gmail.com>
 *
 */
public class OpencrxControlSet extends PopupControlSet {

    /**
     * Class that represents OpenCRX ActivityCreator. Duplicates some functionality of OpenCRX plugin.
     *
     */
    @SuppressWarnings("unused")
    private static class OpencrxActivityCreator {
        /** type*/
        public static final String TYPE = "opencrx-creator"; //$NON-NLS-1$

        /** hashed creator id in opencrx */
        public static final LongProperty REMOTE_ID = new LongProperty(StoreObject.TABLE,
                StoreObject.ITEM.name);

        /** creator name */
        public static final StringProperty NAME = new StringProperty(StoreObject.TABLE,
                StoreObject.VALUE1.name);

        /**
         * String ID in OpenCRX system (ActivityCreator)
         */
        public static final StringProperty CRX_ID = new StringProperty(StoreObject.TABLE,
                StoreObject.VALUE3.name);

        // data class-part
        private final long id;
        private final String name;

        private final String crxId;

        public OpencrxActivityCreator (StoreObject creatorData) {
            this(creatorData.getValue(REMOTE_ID),creatorData.getValue(NAME),
                   creatorData.containsValue(CRX_ID) ? creatorData.getValue(CRX_ID) : ""); //$NON-NLS-1$
        }

        public OpencrxActivityCreator(long id, String name, String crxId) {
            this.id = id;
            this.name = name;
            this.crxId = crxId;
        }
        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCrxId() {
            return crxId;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Class that represents OpenCRX Contact. Duplicates some functionality of OpenCRX plugin.
     *
     */
    @SuppressWarnings("unused")
    private static class OpencrxContact {
        public static final String TYPE = "opencrx-contacts"; //$NON-NLS-1$

        /** hash contact id in opencrx */
        public static final LongProperty REMOTE_ID = new LongProperty(StoreObject.TABLE,
                StoreObject.ITEM.name);

        /** contact first name */
        public static final StringProperty FIRST_NAME = new StringProperty(StoreObject.TABLE,
                StoreObject.VALUE1.name);

        /** contact last name */
        public static final StringProperty LAST_NAME = new StringProperty(StoreObject.TABLE,
                StoreObject.VALUE2.name);

        /** id in OpenCRX as string */
        public static final StringProperty CRX_ID = new StringProperty(StoreObject.TABLE,
                StoreObject.VALUE3.name);

        private final long id;

        private final String email;

        private final String firstname;

        private final String lastname;

        private final String crxId;

        public OpencrxContact(long id, String email, String firstname,
                String lastname, String crxId) {
            this.id = id;
            this.email = email;
            this.firstname = firstname;
            this.lastname = lastname;
            this.crxId = crxId;
        }

        public OpencrxContact(StoreObject userData){
            this(userData.getValue(REMOTE_ID), "", userData.getValue(FIRST_NAME), userData.getValue(LAST_NAME), userData.getValue(CRX_ID) ); //$NON-NLS-1$
        }

        public String getEmail() {
            return email;
        }
        public String getFirstname() {
            return firstname;
        }
        public String getLastname() {
            return lastname;
        }
        public String getCrxId() {
            return crxId;
        }
        public long getId() {
            return id;
        }

        @Override
        public String toString() {
            String displayString = ""; //$NON-NLS-1$
            boolean hasFirstname = false;
            boolean hasLastname = false;
            if (firstname != null && firstname.length() > 0) {
                displayString += firstname;
                hasFirstname = true;
            }
            if (lastname != null && lastname.length() > 0)
                hasLastname = true;
            if (hasFirstname && hasLastname)
                displayString += " "; //$NON-NLS-1$
            if (hasLastname)
                displayString += lastname;

            if (!hasFirstname && !hasLastname && email != null
                    && email.length() > 0)
                displayString += email;
            return displayString;
        }
    }


    // --- instance variables

    private Spinner assignedToSelector;
    private Spinner creatorSelector;

    private AutoCompleteTextView assignedToTextInput;
    private AutoCompleteTextView creatorTextInput;

    private ArrayList<OpencrxContact> users = null;
    private ArrayList<OpencrxActivityCreator> dashboards = null;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private StoreObjectDao storeObjectDao;

    public OpencrxControlSet(final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void afterInflate() {
      //View view = LayoutInflater.from(activity).inflate(R.layout.opencrx_control, parent, true);

        this.assignedToSelector = (Spinner) getView().findViewById(R.id.opencrx_TEA_task_assign);
        TextView emptyView = new TextView(activity);
        emptyView.setText(activity.getText(R.string.opencrx_no_creator));
        assignedToSelector.setEmptyView(emptyView);

        this.creatorSelector = (Spinner) getView().findViewById(R.id.opencrx_TEA_dashboard_assign);

        this.assignedToTextInput = (AutoCompleteTextView) getView().findViewById(R.id.opencrx_TEA_contact_textinput);
        this.creatorTextInput = (AutoCompleteTextView) getView().findViewById(R.id.opencrx_TEA_creator_textinput);
    }

    @Override
    protected void readFromTaskOnInitialize() {


        Metadata metadata = getTaskMetadata(model.getId());
        if(metadata == null)
            metadata = OpencrxCoreUtils.INSTANCE.newMetadata(model.getId());

        // Fill the dashboard-spinner and set the current dashboard
        long dashboardId = OpencrxCoreUtils.INSTANCE.getDefaultCreator();
        if(metadata.containsNonNullValue(OpencrxCoreUtils.ACTIVITY_CREATOR_ID))
            dashboardId = metadata.getValue(OpencrxCoreUtils.ACTIVITY_CREATOR_ID);

        StoreObject[] dashboardsData = readStoreObjects(OpencrxActivityCreator.TYPE);
        dashboards = new ArrayList<OpencrxActivityCreator>(dashboardsData.length);
        int dashboardSpinnerIndex = -1;

        for (int i=0;i<dashboardsData.length;i++) {
            OpencrxActivityCreator dashboard = new OpencrxActivityCreator(dashboardsData[i]);
            dashboards.add(dashboard);
            if(dashboard.getId() == dashboardId) {
                dashboardSpinnerIndex = i;
            }
        }

        //dashboard to not sync as first spinner-entry
        dashboards.add(0, new OpencrxActivityCreator(OpencrxCoreUtils.CREATOR_NO_SYNC, activity.getString(R.string.opencrx_no_creator), "")); //$NON-NLS-1$

        ArrayAdapter<OpencrxActivityCreator> dashAdapter = new ArrayAdapter<OpencrxActivityCreator>(activity,
                android.R.layout.simple_spinner_item, dashboards);
        dashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        creatorSelector.setAdapter(dashAdapter);
        creatorSelector.setSelection(dashboardSpinnerIndex+1);

        ArrayAdapter<OpencrxActivityCreator> creatorAdapterTextInput = new ArrayAdapter<OpencrxActivityCreator>(activity,
                android.R.layout.simple_spinner_item, dashboards);
        creatorTextInput.setAdapter(creatorAdapterTextInput);
        creatorTextInput.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                    long id) {
                OpencrxActivityCreator creatorInput = (OpencrxActivityCreator) adapter.getItemAtPosition(position);

                if (creatorInput == null) return;

                int selectedIndex = creatorSelector.getSelectedItemPosition();

                for (int i = 0; i < creatorSelector.getAdapter().getCount(); ++i){
                    OpencrxActivityCreator current = (OpencrxActivityCreator) creatorSelector.getAdapter().getItem(i);
                    if (current != null && current.getId() == creatorInput.getId()){
                        selectedIndex = i;
                        break;
                    }
                }

                creatorSelector.setSelection(selectedIndex);
            }
        });

        // Assigned user
        long responsibleId = OpencrxCoreUtils.INSTANCE.getDefaultAssignedUser();
        if (metadata.containsNonNullValue(OpencrxCoreUtils.ACTIVITY_ASSIGNED_TO_ID)){
            responsibleId = metadata.getValue(OpencrxCoreUtils.ACTIVITY_ASSIGNED_TO_ID);
        }

        StoreObject[] usersData = readStoreObjects(OpencrxContact.TYPE);
        this.users = new ArrayList<OpencrxContact>();
        for (StoreObject user : usersData){
            this.users.add(new OpencrxContact(user));
        }

        ArrayAdapter<OpencrxContact> usersAdapter = new ArrayAdapter<OpencrxContact>(activity,
                android.R.layout.simple_spinner_item, this.users);
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assignedToSelector.setAdapter(usersAdapter);

        int responsibleSpinnerIndex = 0;

        for (int i = 0; i < this.users.size() ; i++) {
            if (this.users.get(i).getId() == responsibleId ) {
                responsibleSpinnerIndex = i;
                break;
            }
        }
        assignedToSelector.setSelection(responsibleSpinnerIndex);

        ArrayAdapter<OpencrxContact> contactAdapterTextInput = new ArrayAdapter<OpencrxContact>(activity,
                android.R.layout.simple_spinner_item, this.users);

        assignedToTextInput.setAdapter(contactAdapterTextInput);
        assignedToTextInput.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                    long id) {

                OpencrxContact userInput = (OpencrxContact) adapter.getItemAtPosition(position);

                if (userInput == null) return;

                int selectedIndex = assignedToSelector.getSelectedItemPosition();

                for (int i = 0; i < assignedToSelector.getAdapter().getCount(); ++i){
                    OpencrxContact current = (OpencrxContact) assignedToSelector.getAdapter().getItem(i);
                    if (current != null && current.getId() == userInput.getId()){
                        selectedIndex = i;
                        break;
                    }
                }

                assignedToSelector.setSelection(selectedIndex);

            }
        });
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        Metadata metadata = getTaskMetadata(task.getId());
        try {
            if (metadata == null) {
                metadata = OpencrxCoreUtils.INSTANCE.newMetadata(task.getId());
            }

            OpencrxActivityCreator dashboard = (OpencrxActivityCreator) creatorSelector.getSelectedItem();
            metadata.setValue(OpencrxCoreUtils.ACTIVITY_CREATOR_ID, dashboard.getId());

            OpencrxContact responsibleUser = (OpencrxContact) assignedToSelector.getSelectedItem();

            if(responsibleUser == null)
                metadata.setValue(OpencrxCoreUtils.ACTIVITY_ASSIGNED_TO_ID, 0L);
            else
                metadata.setValue(OpencrxCoreUtils.ACTIVITY_ASSIGNED_TO_ID, responsibleUser.getId());

            if(metadata.getSetValues().size() > 0) {
                metadataService.save(metadata);
                task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
            }
        } catch (Exception e) {
            Log.e("opencrx-error", "Error Saving Metadata", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    private Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(Metadata.PROPERTIES).where(
                                                                MetadataCriteria.byTaskAndwithKey(taskId, OpencrxCoreUtils.OPENCRX_ACTIVITY_METADATA_KEY))
                                                         );
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }

    private StoreObject[] readStoreObjects(String type) {
        StoreObject[] ret;
        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.PROPERTIES).
                where(StoreObjectCriteria.byType(type)));
        try {
            ret = new StoreObject[cursor.getCount()];
            for(int i = 0; i < ret.length; i++) {
                cursor.moveToNext();
                StoreObject dashboard = new StoreObject(cursor);
                ret[i] = dashboard;
            }
        } finally {
            cursor.close();
        }

        return ret;
    }

    @Override
    protected void refreshDisplayView() {
        // Nothing to do
    }

}
