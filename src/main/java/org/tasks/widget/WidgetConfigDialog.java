package org.tasks.widget;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class WidgetConfigDialog extends InjectingDialogFragment implements SeekBar.OnSeekBarChangeListener {

    public static WidgetConfigDialog newWidgetConfigDialog(int appWidgetId) {
        WidgetConfigDialog dialog = new WidgetConfigDialog();
        dialog.setAppWidgetId(appWidgetId);
        return dialog;
    }

    public void setAppWidgetId(int appWidgetId) {
        this.appWidgetId = appWidgetId;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        opacity = progress;
        updateOpacity();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public interface WidgetConfigCallback {
        void ok();

        void done();
    }

    private static final int REQUEST_FILTER = 1005;

    @Bind(R.id.opacity_value) TextView opacityValue;
    @Bind(R.id.selected_filter) TextView selectedFilter;
    @Bind(R.id.hideDueDate) CheckBox hideDueDate;
    @Bind(R.id.darkTheme) CheckBox darkTheme;
    @Bind(R.id.hideCheckboxes) CheckBox hideCheckBoxes;
    @Bind(R.id.hideHeader) CheckBox hideHeader;
    @Bind(R.id.opacity_seekbar) SeekBar opacitySeekbar;

    @Inject DialogBuilder dialogBuilder;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject Preferences preferences;
    @Inject @ForApplication Context context;

    private int opacity = WidgetConfigActivity.DEFAULT_OPACITY;
    private Filter filter;
    private int appWidgetId;
    private WidgetConfigCallback callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.widget_config_activity, null);

        ButterKnife.bind(this, view);

        opacitySeekbar.setProgress(opacity);
        opacitySeekbar.setOnSeekBarChangeListener(this);

        filter = defaultFilterProvider.getDefaultFilter();

        updateFilter();
        updateOpacity();

        return dialogBuilder.newDialog()
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveConfiguration();
                        callback.ok();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        callback.done();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (WidgetConfigCallback) getActivity();
    }

    private void updateFilter() {
        selectedFilter.setText(filter.listingTitle);
    }

    private void updateOpacity() {
        opacityValue.setText(Integer.toString(opacity));
    }

    @OnClick(R.id.filter_selection)
    void changeFilter() {
        startActivityForResult(new Intent(getActivity(), FilterSelectionActivity.class) {{
            putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
        }}, REQUEST_FILTER);
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
                updateFilter();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void saveConfiguration(){
        preferences.setString(WidgetConfigActivity.PREF_WIDGET_ID + appWidgetId, defaultFilterProvider.getFilterPreferenceValue(filter));
        preferences.setBoolean(WidgetConfigActivity.PREF_SHOW_DUE_DATE + appWidgetId, !hideDueDate.isChecked());
        preferences.setBoolean(WidgetConfigActivity.PREF_DARK_THEME + appWidgetId, darkTheme.isChecked());
        preferences.setBoolean(WidgetConfigActivity.PREF_HIDE_CHECKBOXES + appWidgetId, hideCheckBoxes.isChecked());
        preferences.setBoolean(WidgetConfigActivity.PREF_HIDE_HEADER + appWidgetId, hideHeader.isChecked());
        preferences.setInt(WidgetConfigActivity.PREF_WIDGET_OPACITY + appWidgetId, opacity);

        // force update after setting preferences
        context.sendBroadcast(new Intent(context, TasksWidget.class) {{
            setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        }});
    }
}
