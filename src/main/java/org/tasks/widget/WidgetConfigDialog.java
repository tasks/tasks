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
import android.widget.TextView;

import com.google.common.base.Strings;
import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.SeekBarDialog;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;

import java.text.NumberFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.tasks.dialogs.SeekBarDialog.newSeekBarDialog;

public class WidgetConfigDialog extends InjectingDialogFragment {

    private static final String EXTRA_FILTER = "extra_filter";
    private static final String EXTRA_THEME = "extra_theme";
    private static final String EXTRA_APP_WIDGET_ID = "extra_app_widget_id";
    private static final String EXTRA_OPACITY = "extra_opacity";
    private static final String EXTRA_FONT_SIZE = "extra_font_size";
    private static final String FRAG_TAG_SEEKBAR = "frag_tag_seekbar";

    public static WidgetConfigDialog newWidgetConfigDialog(int appWidgetId) {
        WidgetConfigDialog dialog = new WidgetConfigDialog();
        dialog.appWidgetId = appWidgetId;
        return dialog;
    }

    public interface WidgetConfigCallback {
        void ok();

        void cancel();
    }

    private static final int REQUEST_FILTER = 1005;
    private static final int REQUEST_THEME_SELECTION = 1006;
    private static final int REQUEST_COLOR_SELECTION = 1007;
    private static final int REQUEST_OPACITY = 1008;
    private static final int REQUEST_FONT_SIZE = 1009;

    @BindView(R.id.opacity_value) TextView opacityValue;
    @BindView(R.id.selected_filter) TextView selectedFilter;
    @BindView(R.id.selected_theme) TextView selectedTheme;
    @BindView(R.id.selected_color) TextView selectedColor;
    @BindView(R.id.showDueDate) CheckBox showDueDate;
    @BindView(R.id.showCheckBoxes) CheckBox showCheckBoxes;
    @BindView(R.id.showHeader) CheckBox showHeader;
    @BindView(R.id.font_size_value) TextView selectedFontSize;
    @BindView(R.id.showSettings) CheckBox showSettings;

    @Inject DialogBuilder dialogBuilder;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject Preferences preferences;
    @Inject @ForApplication Context context;
    @Inject Theme theme;
    @Inject ThemeCache themeCache;

    private Filter filter;
    private int themeIndex = 0;
    private int colorIndex = 0;
    private int appWidgetId;
    private WidgetConfigCallback callback;
    private int opacityPercentage;
    private int fontSize;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = theme.getLayoutInflater(context).inflate(R.layout.widget_config_activity, null);

        ButterKnife.bind(this, view);

        if (savedInstanceState != null) {
            themeIndex = savedInstanceState.getInt(EXTRA_THEME);
            filter = savedInstanceState.getParcelable(EXTRA_FILTER);
            appWidgetId = savedInstanceState.getInt(EXTRA_APP_WIDGET_ID);
            opacityPercentage = savedInstanceState.getInt(EXTRA_OPACITY);
            fontSize = savedInstanceState.getInt(EXTRA_FONT_SIZE);
        } else {
            String filterId = preferences.getStringValue(WidgetConfigActivity.PREF_WIDGET_ID + appWidgetId);
            if (Strings.isNullOrEmpty(filterId)) {
                filter = defaultFilterProvider.getDefaultFilter();
            } else {
                filter = defaultFilterProvider.getFilterFromPreference(filterId);
            }
            opacityPercentage = preferences.getInt(WidgetConfigActivity.PREF_WIDGET_OPACITY + appWidgetId, 100);
            fontSize = preferences.getInt(WidgetConfigActivity.PREF_FONT_SIZE + appWidgetId, 16);
            themeIndex = preferences.getInt(WidgetConfigActivity.PREF_THEME + appWidgetId, 0);
            colorIndex = preferences.getInt(WidgetConfigActivity.PREF_COLOR + appWidgetId, 0);
            showDueDate.setChecked(preferences.getBoolean(WidgetConfigActivity.PREF_SHOW_DUE_DATE + appWidgetId, true));
            showCheckBoxes.setChecked(preferences.getBoolean(WidgetConfigActivity.PREF_SHOW_CHECKBOXES + appWidgetId, true));
            showHeader.setChecked(preferences.getBoolean(WidgetConfigActivity.PREF_SHOW_HEADER + appWidgetId, true));
            showSettings.setChecked(preferences.getBoolean(WidgetConfigActivity.PREF_SHOW_SETTINGS + appWidgetId, true));
        }

        updateFilter();
        updateTheme();
        updateColor();
        updateOpacity();
        updateFontSize();

        return dialogBuilder.newDialog()
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveConfiguration();
                        callback.ok();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callback.cancel();
                    }
                })
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_APP_WIDGET_ID, appWidgetId);
        outState.putInt(EXTRA_THEME, themeIndex);
        outState.putInt(EXTRA_OPACITY, opacityPercentage);
        outState.putInt(EXTRA_FONT_SIZE, fontSize);
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
        opacityValue.setText(NumberFormat.getPercentInstance().format(opacityPercentage / 100.0));
    }

    private void updateFontSize() {
        selectedFontSize.setText(NumberFormat.getIntegerInstance().format(fontSize));
    }

    private void updateTheme() {
        selectedTheme.setText(themeCache.getWidgetTheme(themeIndex).getName());
    }

    private void updateColor() {
        selectedColor.setText(themeCache.getThemeColor(colorIndex).getName());
    }

    @OnClick(R.id.filter_selection)
    void changeFilter() {
        startActivityForResult(new Intent(getActivity(), FilterSelectionActivity.class) {{
            putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
        }}, REQUEST_FILTER);
    }

    @OnClick(R.id.theme_selection)
    public void showThemeSelection() {
        startActivityForResult(new Intent(context, ColorPickerActivity.class) {{
            putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerDialog.ColorPalette.WIDGET_BACKGROUND);
        }}, REQUEST_THEME_SELECTION);
    }

    @OnClick(R.id.theme_color)
    public void showColorSelection() {
        startActivityForResult(new Intent(context, ColorPickerActivity.class) {{
            putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerDialog.ColorPalette.COLORS);
        }}, REQUEST_COLOR_SELECTION);
    }

    @OnClick(R.id.widget_opacity)
    public void showOpacitySlider() {
        SeekBarDialog seekBarDialog = newSeekBarDialog(R.layout.dialog_opacity_seekbar, opacityPercentage);
        seekBarDialog.setTargetFragment(this, REQUEST_OPACITY);
        seekBarDialog.show(getChildFragmentManager(), FRAG_TAG_SEEKBAR);
    }

    @OnClick(R.id.font_size)
    public void showFontSizeSlider() {
        SeekBarDialog seekBarDialog = newSeekBarDialog(R.layout.dialog_font_size_seekbar, fontSize);
        seekBarDialog.setTargetFragment(this, REQUEST_FONT_SIZE);
        seekBarDialog.show(getChildFragmentManager(), FRAG_TAG_SEEKBAR);
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_THEME_SELECTION) {
            if (resultCode == Activity.RESULT_OK) {
                themeIndex = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
                updateTheme();
            }
        } else if (requestCode == REQUEST_COLOR_SELECTION) {
            if (resultCode == Activity.RESULT_OK) {
                colorIndex = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
                updateColor();
            }
        } else if (requestCode == REQUEST_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
                updateFilter();
            }
        } else if (requestCode == REQUEST_OPACITY) {
            if (resultCode == Activity.RESULT_OK) {
                opacityPercentage = data.getIntExtra(SeekBarDialog.EXTRA_VALUE, 100);
                updateOpacity();
            }
        } else if (requestCode == REQUEST_FONT_SIZE) {
            if (resultCode == Activity.RESULT_OK) {
                fontSize = data.getIntExtra(SeekBarDialog.EXTRA_VALUE, 16);
                updateFontSize();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void saveConfiguration(){
        preferences.setString(WidgetConfigActivity.PREF_WIDGET_ID + appWidgetId, defaultFilterProvider.getFilterPreferenceValue(filter));
        preferences.setBoolean(WidgetConfigActivity.PREF_SHOW_DUE_DATE + appWidgetId, showDueDate.isChecked());
        preferences.setBoolean(WidgetConfigActivity.PREF_SHOW_CHECKBOXES+ appWidgetId, showCheckBoxes.isChecked());
        preferences.setBoolean(WidgetConfigActivity.PREF_SHOW_HEADER + appWidgetId, showHeader.isChecked());
        preferences.setBoolean(WidgetConfigActivity.PREF_SHOW_SETTINGS + appWidgetId, showSettings.isChecked());
        preferences.setInt(WidgetConfigActivity.PREF_THEME + appWidgetId, themeIndex);
        preferences.setInt(WidgetConfigActivity.PREF_COLOR + appWidgetId, colorIndex);
        preferences.setInt(WidgetConfigActivity.PREF_WIDGET_OPACITY + appWidgetId, opacityPercentage);
        preferences.setInt(WidgetConfigActivity.PREF_FONT_SIZE + appWidgetId, fontSize);

        // force update after setting preferences
        context.sendBroadcast(new Intent(context, TasksWidget.class) {{
            setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        }});
    }
}
