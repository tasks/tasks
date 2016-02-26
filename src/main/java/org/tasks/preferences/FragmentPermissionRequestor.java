package org.tasks.preferences;

import android.app.Fragment;
import android.support.v13.app.FragmentCompat;

import javax.inject.Inject;

public class FragmentPermissionRequestor extends PermissionRequestor {
    private Fragment fragment;

    @Inject
    public FragmentPermissionRequestor(Fragment fragment, PermissionChecker permissionChecker) {
        super(permissionChecker);

        this.fragment = fragment;
    }

    @Override
    protected void requestPermissions(String[] permissions, int rc) {
        FragmentCompat.requestPermissions(fragment, permissions, rc);
    }
}
