package org.tasks.injection;

import android.app.Activity;
import android.os.Bundle;

public class InjectingActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Injector) getApplication()).inject(this, new ActivityModule(this));

        super.onCreate(savedInstanceState);
    }
}
