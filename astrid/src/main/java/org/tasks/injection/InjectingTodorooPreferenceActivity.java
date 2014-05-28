package org.tasks.injection;

import android.os.Bundle;

import com.todoroo.andlib.utility.TodorooPreferenceActivity;

public abstract class InjectingTodorooPreferenceActivity extends TodorooPreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((Injector) getApplication()).inject(this, new ActivityModule(this));

        super.onCreate(savedInstanceState);
    }
}
