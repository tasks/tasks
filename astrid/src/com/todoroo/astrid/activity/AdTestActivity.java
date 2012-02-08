package com.todoroo.astrid.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.WebServicesView;

public class AdTestActivity extends Activity {

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        super.onCreate(savedInstanceState);

        WebServicesView webServicesView = new WebServicesView(this);
        webServicesView.setLayoutParams(new FrameLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        webServicesView.setPadding(10, 10, 10, 10);

        setContentView(webServicesView);

        Task task = new Task();
        task.setValue(Task.TITLE, "America (The Book)"); //$NON-NLS-1$
        webServicesView.setTask(task);
    }

}
