/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.helper.UUIDHelper;

import java.util.List;

@Dao
public abstract class TagDataDao {

    @Query("SELECT * FROM tagdata WHERE name = :name COLLATE NOCASE LIMIT 1")
    public abstract TagData getTagByName(String name);

    // TODO: does this need to be ordered?
    @Query("SELECT * FROM tagdata WHERE deleted = 0 ORDER BY _id ASC")
    public abstract List<TagData> allTags();

    @Query("SELECT * FROM tagdata WHERE remoteId = :uuid LIMIT 1")
    public abstract TagData getByUuid(String uuid);

    @Query("SELECT * FROM tagdata WHERE deleted = 0 AND name IS NOT NULL ORDER BY UPPER(name) ASC")
    public abstract List<TagData> tagDataOrderedByName();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void persist(TagData tagData);

    @Query("UPDATE tagdata SET name = :name WHERE remoteId = :remoteId")
    public abstract void rename(String remoteId, String name);

    @Query("DELETE FROM tagdata WHERE _id = :id")
    public abstract void delete(Long id);

    @Insert
    public abstract void insert(TagData tag);

    public void createNew(TagData tag) {
        if (RemoteModel.isUuidEmpty(tag.getRemoteId())) {
            tag.setRemoteId(UUIDHelper.newUUID());
        }
        insert(tag);
    }
}

