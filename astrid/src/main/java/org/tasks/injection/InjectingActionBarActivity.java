package org.tasks.injection;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class InjectingActionBarActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Injector) getApplication()).inject(this, new ActivityModule(this));

        super.onCreate(savedInstanceState);
    }
}
