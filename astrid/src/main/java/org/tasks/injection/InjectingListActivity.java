package org.tasks.injection;

import android.app.ListActivity;
import android.os.Bundle;

public class InjectingListActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Injector) getApplication()).inject(this, new ActivityModule());

        super.onCreate(savedInstanceState);
    }
}
