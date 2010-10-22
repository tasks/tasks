package com.todoroo.astrid.widget;

import com.timsu.astrid.R;

public class PowerWidget44 extends PowerWidget {

    @Override
    public Class<? extends UpdateService> getUpdateService() {
        return UpdateService44.class;
    }

    public static class UpdateService44 extends PowerWidget.UpdateService {

        @Override
        public Class<? extends PowerWidget> getWidgetClass() {
            return PowerWidget44.class;
        }

        @Override
        public int getWidgetLayout() {
            return R.layout.widget_power_44;
        }

        @Override
        public int getRowLimit() {
            return 10;
        }
    }

    public static class ConfigActivity extends WidgetConfigActivity {
        @Override
        public boolean showColorSelectionSetting() {
            return true;
        }

        @Override
        public boolean showEncouragementSetting() {
            return true;
        }

        @Override
        public void updateWidget() {
            new PowerWidget44().updateAppWidget(this, mAppWidgetId);
        }
    }
}
