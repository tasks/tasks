package org.tasks.widget;

import org.tasks.injection.ActivityComponent;

public class WidgetConfigActivity extends BaseWidgetConfigActivity {
    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void initiateThemePurchase() {

    }
}
