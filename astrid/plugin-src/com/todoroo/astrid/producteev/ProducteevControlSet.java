package com.todoroo.astrid.producteev;

import java.util.ArrayList;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.producteev.sync.ProducteevUser;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.ui.PopupControlSet;

/**
 * Control Set for managing task/dashboard assignments in Producteev
 *
 * @author Arne Jans <arne.jans@gmail.com>
 *
 */
public class ProducteevControlSet extends PopupControlSet {

    private Spinner responsibleSelector;
    private Spinner dashboardSelector;

    private ArrayList<ProducteevUser> users = null;
    private ArrayList<ProducteevDashboard> dashboards = null;

    @Autowired MetadataService metadataService;
    @Autowired ExceptionService exceptionService;

    private int lastDashboardSelection = 0;

    public ProducteevControlSet(final Activity activity, int layout, int displayViewLayout, int title) {
        super(activity, layout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Refresh the content of the responsibleSelector with the given userlist.
     *
     * @param newUsers the new userlist to show in the responsibleSelector
     */
    private void refreshResponsibleSpinner(ArrayList<ProducteevUser> newUsers) {
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(model.getId());
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

        getView().findViewById(R.id.producteev_TEA_task_assign_label).setVisibility(visibility);
        responsibleSelector.setVisibility(visibility);

        int responsibleSpinnerIndex = 0;

        for (int i = 0; i < this.users.size() ; i++) {
            if (this.users.get(i).getId() == responsibleId ||
                    (responsibleId == -1 && this.users.get(i).getId() == Preferences.getLong(ProducteevUtilities.PREF_USER_ID, -1))) {
                responsibleSpinnerIndex = i;
                break;
            }
        }
        responsibleSelector.setSelection(responsibleSpinnerIndex);
    }

    @Override
    protected void readFromTaskPrivate() {
        Metadata metadata = ProducteevDataService.getInstance().getTaskMetadata(model.getId());
        if(metadata == null)
            metadata = ProducteevTask.newMetadata();

        // Fill the dashboard-spinner and set the current dashboard
        long dashboardId = ProducteevUtilities.INSTANCE.getDefaultDashboard();
        if(metadata.containsNonNullValue(ProducteevTask.DASHBOARD_ID))
            dashboardId = metadata.getValue(ProducteevTask.DASHBOARD_ID);

        StoreObject[] dashboardsData = ProducteevDataService.getInstance().getDashboards();
        dashboards = new ArrayList<ProducteevDashboard>(dashboardsData.length);
        ProducteevDashboard ownerDashboard = null;
        int dashboardSpinnerIndex = -1;

        int i = 0;
        for (i=0;i<dashboardsData.length;i++) {
            ProducteevDashboard dashboard = new ProducteevDashboard(dashboardsData[i]);
            dashboards.add(dashboard);
            if(dashboard.getId() == dashboardId) {
                ownerDashboard = dashboard;
                dashboardSpinnerIndex = i;
            }
        }

        //dashboard to not sync as first spinner-entry
        dashboards.add(0, new ProducteevDashboard(ProducteevUtilities.DASHBOARD_NO_SYNC, activity.getString(R.string.producteev_no_dashboard),null));
        // dummy entry for adding a new dashboard
        dashboards.add(new ProducteevDashboard(ProducteevUtilities.DASHBOARD_CREATE, activity.getString(R.string.producteev_create_dashboard),null));

        ArrayAdapter<ProducteevDashboard> dashAdapter = new ArrayAdapter<ProducteevDashboard>(activity,
                android.R.layout.simple_spinner_item, dashboards);
        dashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dashboardSelector.setAdapter(dashAdapter);
        dashboardSelector.setSelection(dashboardSpinnerIndex+1);

        if (ownerDashboard == null || ownerDashboard.getId() == ProducteevUtilities.DASHBOARD_NO_SYNC
                || ownerDashboard.getId() == ProducteevUtilities.DASHBOARD_CREATE) {
            responsibleSelector.setEnabled(false);
            responsibleSelector.setAdapter(null);
            getView().findViewById(R.id.producteev_TEA_task_assign_label).setVisibility(View.GONE);
            return;
        }

        refreshResponsibleSpinner(ownerDashboard.getUsers());
    }

    @Override
    protected void afterInflate() {

        this.displayText.setText(activity.getString(R.string.producteev_TEA_control_set_display));

        //view = LayoutInflater.from(activity).inflate(R.layout.producteev_control, parent, true);

        this.responsibleSelector = (Spinner) getView().findViewById(R.id.producteev_TEA_task_assign);
        TextView emptyView = new TextView(activity);
        emptyView.setText(activity.getText(R.string.producteev_no_dashboard));
        responsibleSelector.setEmptyView(emptyView);

        this.dashboardSelector = (Spinner) getView().findViewById(R.id.producteev_TEA_dashboard_assign);
        this.dashboardSelector.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> spinnerParent, View spinnerView,
                    int position, long id) {
                final Spinner dashSelector = (Spinner) spinnerParent;
                ProducteevDashboard dashboard = (ProducteevDashboard) dashSelector.getSelectedItem();
                if (dashboard.getId() == ProducteevUtilities.DASHBOARD_CREATE) {
                    // let the user create a new dashboard
                    final EditText editor = new EditText(ProducteevControlSet.this.activity);
                    OnClickListener okListener = new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity context = ProducteevControlSet.this.activity;
                            String newDashboardName = editor.getText().toString();
                            if (newDashboardName == null || newDashboardName.length() == 0) {
                                dialog.cancel();
                            } else {
                                // create the real dashboard, select it in the spinner and refresh responsiblespinner
                                ProgressDialog progressDialog = com.todoroo.andlib.utility.DialogUtilities.progressDialog(context,
                                        context.getString(R.string.DLG_wait));
                                try {
                                    progressDialog.show();
                                    JSONObject newDashJSON = ProducteevSyncProvider.getInvoker().dashboardsCreate(
                                            newDashboardName).getJSONObject("dashboard"); //$NON-NLS-1$
                                    StoreObject local = ProducteevDataService.getInstance().updateDashboards(newDashJSON, true);
                                    if (local != null) {
                                        ProducteevDashboard newDashboard = new ProducteevDashboard(local);
                                        ArrayAdapter<ProducteevDashboard> adapter = (ArrayAdapter<ProducteevDashboard>) dashSelector.getAdapter();
                                        adapter.insert(newDashboard, adapter.getCount()-1);
                                        dashSelector.setSelection(adapter.getCount()-2);
                                        refreshResponsibleSpinner(newDashboard.getUsers());
                                        DialogUtilities.dismissDialog(context, progressDialog);
                                    }
                                } catch (Exception e) {
                                    DialogUtilities.dismissDialog(context, progressDialog);
                                    DialogUtilities.okDialog(context,
                                            context.getString(R.string.DLG_error, e.getMessage()),
                                            null);
                                    exceptionService.reportError("pdv-create-dashboard", e); //$NON-NLS-1$
                                    dashSelector.setSelection(0);
                                }
                            }

                        }
                    };
                    OnClickListener cancelListener = new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            dashboardSelector.setSelection(lastDashboardSelection);
                        }
                    };
                    DialogUtilities.viewDialog(ProducteevControlSet.this.activity,
                            ProducteevControlSet.this.activity.getString(R.string.producteev_create_dashboard_name),
                            editor,
                            okListener,
                            cancelListener);
                } else {
                    refreshResponsibleSpinner(dashboard.getUsers());
                    lastDashboardSelection = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> spinnerParent) {
                //
            }
        });
    }

    @Override
    protected String writeToModelPrivate(Task task) {
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

            // Erase PDTV-repeating-info if task itself is repeating with Astrid-repeat
            if (task.containsNonNullValue(Task.RECURRENCE) && task.getValue(Task.RECURRENCE).length()>0) {
                metadata.setValue(ProducteevTask.REPEATING_SETTING, ""); //$NON-NLS-1$
            }

            if(metadata.getSetValues().size() > 0) {
                metadataService.save(metadata);
                task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
            }
        } catch (Exception e) {
            Log.e("error-saving-pdv", "Error Saving Metadata", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }

    @Override
    protected void refreshDisplayView() {
        // TODO Auto-generated method stub

    }
}