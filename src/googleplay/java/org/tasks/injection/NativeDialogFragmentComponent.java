package org.tasks.injection;

import org.tasks.activities.NativeGoogleTaskListPicker;

import dagger.Subcomponent;

@Subcomponent(modules = NativeDialogFragmentModule.class)
public interface NativeDialogFragmentComponent extends BaseNativeDialogFragmentComponent {
    void inject(NativeGoogleTaskListPicker nativeGoogleTaskListPicker);
}
