package com.todoroo.astrid.service;

import com.todoroo.astrid.data.Metadata;

public interface SynchronizeMetadataCallback {
    public void beforeDeleteMetadata(Metadata m);
}
