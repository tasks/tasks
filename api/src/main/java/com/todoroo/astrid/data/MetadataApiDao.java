/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import android.content.Context;

import com.todoroo.andlib.data.ContentResolverDao;
import com.todoroo.andlib.sql.Criterion;

/**
 * Data access object for accessing Astrid's {@link Metadata} table. A
 * piece of Metadata is information about a task, for example a tag or a
 * note. It operates in a one-to-many relation with tasks.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MetadataApiDao extends ContentResolverDao<Metadata> {

    public MetadataApiDao(Context context) {
        super(Metadata.class, context, Metadata.CONTENT_URI);
    }

    /**
     * Generates SQL clauses
     */
    public static class MetadataCriteria {

        /** Returns all metadata associated with a given task */
        public static Criterion byTask(long taskId) {
            return Metadata.TASK.eq(taskId);
        }

        /** Returns all metadata associated with a given key */
        public static Criterion withKey(String key) {
            return Metadata.KEY.eq(key);
        }

        /** Returns all metadata associated with a given key */
        public static Criterion byTaskAndwithKey(long taskId, String key) {
            return Criterion.and(withKey(key), byTask(taskId));
        }

    }

}
