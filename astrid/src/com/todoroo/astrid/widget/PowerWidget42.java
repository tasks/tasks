package com.todoroo.astrid.widget;

import com.timsu.astrid.R;

public class PowerWidget42 extends PowerWidget {

    @Override
    public Class<? extends UpdateService> getUpdateService() {
        return UpdateService42.class;
    }

    public static class UpdateService42 extends PowerWidget.UpdateService {

        @Override
        public Class<? extends PowerWidget> getWidgetClass() {
            return PowerWidget42.class;
        }

        @Override
        public int getWidgetLayout() {
            return R.layout.widget_power_42;
        }

        @Override
        public int getRowLimit() {
            return 5;
        }
    }

    public static class ConfigActivity extends WidgetConfigActivity {
        @Override
        public boolean showColorSelectionSetting() {
            return true;
        }

        @Override
        public void updateWidget() {
            new PowerWidget42().updateAppWidget(this, mAppWidgetId);
        }
    }
}
