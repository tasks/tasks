package com.todoroo.astrid.service;

import com.todoroo.astrid.data.Metadata;

public interface SynchronizeMetadataCallback {
    void beforeDeleteMetadata(Metadata m);
}
