package com.todoroo.astrid.provider;

import com.todoroo.andlib.data.AbstractDatabase;
import com.todoroo.astrid.test.DatabaseTestCase;

public class ProviderTestUtilities extends DatabaseTestCase {

    public static final void setDatabaseOverride(AbstractDatabase database) {
        Astrid3ContentProvider.setDatabaseOverride(database);

    }
}
