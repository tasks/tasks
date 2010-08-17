package com.todoroo.astrid.producteev;

import java.util.ArrayList;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.StoreObject;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.producteev.sync.ProducteevUser;
import com.todoroo.astrid.service.MetadataService;

/**
 * Control Set for managing task/dashboard assignments in Producteev
 *
 * @author Arne Jans <arne.jans@gmail.com>
 *
 */
public class ProducteevControlSet implements TaskEditControlSet {

    // --- instance variables

    private final Activity activity;

    private final View view;
    private Task myTask;
    private final Spinner responsibleSelector;
    private final Spinner dashboardSelector;

    private ArrayList<ProducteevUser> users = null;
    private ArrayList<ProducteevDashboard> dashboards = null;

    @Autowired
    MetadataService metadataService;

    public ProducteevControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        view = LayoutInflater.from(activity).inflate(R.layout.producteev_control, parent, true);

        this.responsibleSelector = (Spinner) activity.findViewById(R.id.producteev_TEA_task_assign);
        TextView emptyView = new TextView(activity);
        emptyView.setText(activity.getText(R.string.producteev_no_dashboard));
        responsibleSelector.setEmptyView(emptyView);

        this.dashboardSelector = (Spinner) activity.findViewById(R.id.producteev_TEA_dashboard_assign);
        this.dashboardSelector.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> spinnerParent, View spinnerView,
                    int position, long id) {
                Spinner dashSelector = (Spinner) spinnerParent;
                ProducteevDashboard dashboard = (ProducteevDashboard) dashSelector.getSelectedItem();
                refreshResponsibleSpinner(dashboard.getUsers());
            }

            @Override
            public void onNothingSelected(AdapterView<?> spinnerParent) {
                //
            }
        });
    }

    /**
     * Refresh the content of the responsibleSelector with the given userlist.
     *
     * @param newUsers the new userlist to show in the responsibleSelector
     */
    private void refreshResponsibleSpinner(ArrayList<ProducteevUser> newUsers) {
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(myTask.getId());
        long responsibleId = -1;
        if(metadata != null && metadata.containsNonNullValue(ProducteevTask.RESPONSIBLE_ID))
            responsibleId = metadata.getValue(ProducteevTask.RESPONSIBLE_ID);
        refreshResponsibleSpinner(newUsers, responsibleId);
    }

    /**
     * Refresh the content of the responsibleSelector with the given userlist.
     *
     * @param newUsers the new userlist to show in the responsibleSelector
     * @param responsibleId the id of the responsible user to set in the spinner
     */
    private void refreshResponsibleSpinner(ArrayList<ProducteevUser> newUsers, long responsibleId) {
        // Fill the responsible-spinner and set the current responsible
        this.users = (newUsers == null ? new ArrayList<ProducteevUser>() : newUsers);

        ArrayAdapter<ProducteevUser> usersAdapter = new ArrayAdapter<ProducteevUser>(activity,
                android.R.layout.simple_spinner_item, this.users);
        usersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        responsibleSelector.setAdapter(usersAdapter);

        int visibility = newUsers == null ? View.GONE : View.VISIBLE;

        view.findViewById(R.id.producteev_TEA_task_assign_label).setVisibility(visibility);
        responsibleSelector.setVisibility(visibility);

        int responsibleSpinnerIndex = 0;

        for (int i = 0; i < this.users.size() ; i++) {
            if (this.users.get(i).getId() == responsibleId) {
                responsibleSpinnerIndex = i;
                break;
            }
        }
        responsibleSelector.setSelection(responsibleSpinnerIndex);
    }

    @Override
    public void readFromTask(Task task) {
        this.myTask = task;
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(myTask.getId());
        if(metadata == null)
            metadata = ProducteevTask.newMetadata();

        // Fill the dashboard-spinner and set the current dashboard
        long dashboardId = -1;
        if(metadata.containsNonNullValue(ProducteevTask.DASHBOARD_ID))
            dashboardId = metadata.getValue(ProducteevTask.DASHBOARD_ID);

        StoreObject[] dashboardsData = ProducteevDataService.getInstance().getDashboards();
        dashboards = new ArrayList<ProducteevDashboard>(dashboardsData.length);
        ProducteevDashboard ownerDashboard = null;
        int dashboardSpinnerIndex = -1;

        //dashboard to not sync as first spinner-entry
        for (int i=0;i<dashboardsData.length;i++) {
            ProducteevDashboard dashboard = new ProducteevDashboard(dashboardsData[i]);
            dashboards.add(dashboard);
            if(dashboard.getId() == dashboardId) {
                ownerDashboard = dashboard;
                dashboardSpinnerIndex = i;
            }
        }
        dashboards.add(0, new ProducteevDashboard(ProducteevUtilities.DASHBOARD_NO_SYNC, activity.getString(R.string.producteev_no_dashboard),null));

        ArrayAdapter<ProducteevDashboard> dashAdapter = new ArrayAdapter<ProducteevDashboard>(activity,
                android.R.layout.simple_spinner_item, dashboards);
        dashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dashboardSelector.setAdapter(dashAdapter);
        dashboardSelector.setSelection(dashboardSpinnerIndex+1);

        if (ownerDashboard == null || ownerDashboard.getId() == ProducteevUtilities.DASHBOARD_NO_SYNC) {
            responsibleSelector.setEnabled(false);
            responsibleSelector.setAdapter(null);
            view.findViewById(R.id.producteev_TEA_task_assign_label).setVisibility(View.GONE);
            return;
        }

        refreshResponsibleSpinner(ownerDashboard.getUsers());
    }

    @Override
    public void writeToModel(Task task) {
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(task.getId());
        try {
            if (metadata == null) {
                metadata = new Metadata();
                metadata.setValue(Metadata.KEY, ProducteevTask.METADATA_KEY);
                metadata.setValue(Metadata.TASK, task.getId());
                metadata.setValue(ProducteevTask.ID, 0L);
            }

            ProducteevDashboard dashboard = (ProducteevDashboard) dashboardSelector.getSelectedItem();
            metadata.setValue(ProducteevTask.DASHBOARD_ID, dashboard.getId());

            ProducteevUser responsibleUser = (ProducteevUser) responsibleSelector.getSelectedItem();

            if(responsibleUser == null)
                metadata.setValue(ProducteevTask.RESPONSIBLE_ID, 0L);
            else
                metadata.setValue(ProducteevTask.RESPONSIBLE_ID, responsibleUser.getId());

            if(metadata.getSetValues().size() > 0 ) {
                metadataService.save(metadata);
                task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
            }
        } catch (Exception e) {
            Log.e("error-saving-pdv", "Error Saving Metadata", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}