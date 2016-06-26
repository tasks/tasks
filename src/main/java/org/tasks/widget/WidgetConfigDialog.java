package org.tasks.widget;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
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
import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.Theme;
import org.tasks.preferences.ThemeManager;

import java.text.NumberFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.tasks.dialogs.ThemePickerDialog.newThemePickerDialog;

public class WidgetConfigDialog extends InjectingDialogFragment implements SeekBar.OnSeekBarChangeListener {

    private static final String FRAG_TAG_THEME_SELECTION = "frag_tag_theme_selection";
    private static final String FRAG_TAG_COLOR_SELECTION = "frag_tag_color_selection";
    private static final String EXTRA_FILTER = "extra_filter";
    private static final String EXTRA_THEME = "extra_theme";
    private static final String EXTRA_APP_WIDGET_ID = "extra_app_widget_id";

    public static WidgetConfigDialog newWidgetConfigDialog(int appWidgetId) {
        WidgetConfigDialog dialog = new WidgetConfigDialog();
        dialog.appWidgetId = appWidgetId;
        return dialog;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateOpacity();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void setTheme(Theme theme) {
        this.theme = theme.getThemeIndex();
        updateTheme();
    }

    public void setColor(Theme theme) {
        this.color = theme.getThemeIndex();
        updateColor();
    }

    public interface WidgetConfigCallback {
        void ok();

        void cancel();
    }

    private static final int REQUEST_FILTER = 1005;

    @BindView(R.id.opacity_value) TextView opacityValue;
    @BindView(R.id.selected_filter) TextView selectedFilter;
    @BindView(R.id.selected_theme) TextView selectedTheme;
    @BindView(R.id.selected_color) TextView selectedColor;
    @BindView(R.id.hideDueDate) CheckBox hideDueDate;
    @BindView(R.id.hideCheckboxes) CheckBox hideCheckBoxes;
    @BindView(R.id.hideHeader) CheckBox hideHeader;
    @BindView(R.id.opacity_seekbar) SeekBar opacitySeekbar;

    @Inject DialogBuilder dialogBuilder;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject Preferences preferences;
    @Inject @ForApplication Context context;
    @Inject ThemeManager themeManager;

    private Filter filter;
    private int theme = 0;
    private int color = 0;
    private int appWidgetId;
    private WidgetConfigCallback callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = themeManager.getThemedLayoutInflater().inflate(R.layout.widget_config_activity, null);

        ButterKnife.bind(this, view);

        opacitySeekbar.setOnSeekBarChangeListener(this);

        if (savedInstanceState != null) {
            theme = savedInstanceState.getInt(EXTRA_THEME);
            filter = savedInstanceState.getParcelable(EXTRA_FILTER);
            appWidgetId = savedInstanceState.getInt(EXTRA_APP_WIDGET_ID);
        } else {
            filter = defaultFilterProvider.getDefaultFilter();
            opacitySeekbar.setProgress(WidgetConfigActivity.DEFAULT_OPACITY);
        }

        updateFilter();
        updateTheme();
        updateColor();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_APP_WIDGET_ID, appWidgetId);
        outState.putInt(EXTRA_THEME, theme);
        outState.putParcelable(EXTRA_FILTER, filter);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        callback.cancel();
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
        opacityValue.setText(NumberFormat.getPercentInstance().format(opacitySeekbar.getProgress() / 255.0));
    }

    private void updateTheme() {
        selectedTheme.setText(themeManager.getBaseThemeName(theme));
    }

    private void updateColor() {
        selectedColor.setText(themeManager.getColorName(color));
    }

    @OnClick(R.id.filter_selection)
    void changeFilter() {
        startActivityForResult(new Intent(getActivity(), FilterSelectionActivity.class) {{
            putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
        }}, REQUEST_FILTER);
    }

    @OnClick(R.id.theme_selection)
    public void showThemeSelection() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentByTag(FRAG_TAG_THEME_SELECTION) == null) {
            newThemePickerDialog(ThemePickerDialog.ColorPalette.THEMES).show(fragmentManager, FRAG_TAG_THEME_SELECTION);
        }
    }

    @OnClick(R.id.theme_color)
    public void showColorSelection() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentByTag(FRAG_TAG_COLOR_SELECTION) == null) {
            newThemePickerDialog(ThemePickerDialog.ColorPalette.COLORS).show(fragmentManager, FRAG_TAG_COLOR_SELECTION);
        }
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
        preferences.setBoolean(WidgetConfigActivity.PREF_HIDE_CHECKBOXES + appWidgetId, hideCheckBoxes.isChecked());
        preferences.setBoolean(WidgetConfigActivity.PREF_HIDE_HEADER + appWidgetId, hideHeader.isChecked());
        preferences.setInt(WidgetConfigActivity.PREF_THEME + appWidgetId, theme);
        preferences.setInt(WidgetConfigActivity.PREF_COLOR + appWidgetId, color);
        preferences.setInt(WidgetConfigActivity.PREF_WIDGET_OPACITY + appWidgetId, opacitySeekbar.getProgress());

        // force update after setting preferences
        context.sendBroadcast(new Intent(context, TasksWidget.class) {{
            setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        }});
    }
}
