package org.tasks.injection;

import org.tasks.activities.GoogleTaskListSelectionDialog;

import dagger.Subcomponent;

@Subcomponent(modules = DialogFragmentModule.class)
public interface DialogFragmentComponent extends BaseDialogFragmentComponent {

    void inject(GoogleTaskListSelectionDialog googleTaskListSelectionDialog);

}
