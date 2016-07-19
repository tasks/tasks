package org.tasks.injection;

import org.tasks.activities.SupportGoogleTaskListPicker;

import dagger.Subcomponent;

@Subcomponent(modules = DialogFragmentModule.class)
public interface DialogFragmentComponent extends BaseDialogFragmentComponent {

    void inject(SupportGoogleTaskListPicker supportGoogleTaskListPicker);

}
