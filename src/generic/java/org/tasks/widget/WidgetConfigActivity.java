package org.tasks.widget;

import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class WidgetConfigActivity extends BaseWidgetConfigActivity {

    @Inject Preferences preferences;

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void initiateThemePurchase() {
        preferences.setBoolean(R.string.p_purchased_themes, true);
        showThemeSelection();
    }
}
