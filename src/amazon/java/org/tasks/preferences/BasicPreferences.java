package org.tasks.preferences;

import android.os.Bundle;

import org.tasks.R;
import org.tasks.injection.ActivityComponent;

public class BasicPreferences extends BaseBasicPreferences {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requires(false, R.string.synchronization, R.string.get_plugins);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void initiateThemePurchase() {

    }
}
