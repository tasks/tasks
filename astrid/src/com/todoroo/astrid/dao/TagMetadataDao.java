/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import org.json.JSONArray;

import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.tags.TagMemberMetadata;

/**
 * Data Access layer for {@link Metadata}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagMetadataDao extends DatabaseDao<TagMetadata> {

    @Autowired
    private Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public TagMetadataDao() {
        super(TagMetadata.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    public static class TagMetadataCriteria {
        /** Returns all metadata associated with a given task */
        public static Criterion byTag(String tagUuid) {
            return TagMetadata.TAG_UUID.eq(tagUuid);
        }

        /** Returns all metadata associated with a given key */
        public static Criterion withKey(String key) {
            return TagMetadata.KEY.eq(key);
        }

        /** Returns all metadata associated with a given key */
        public static Criterion byTagAndWithKey(String tagUuid, String key) {
            return Criterion.and(withKey(key), byTag(tagUuid));
        }
    }

    @Override
    protected boolean shouldRecordOutstanding(TagMetadata item) {
        ContentValues cv = item.getSetValues();
        return super.shouldRecordOutstanding(item) && cv != null &&
                ((cv.containsKey(TagMetadata.KEY.name) &&
                        TagMemberMetadata.KEY.equals(item.getValue(TagMetadata.KEY))) ||
                (cv.containsKey(TagMetadata.DELETION_DATE.name) &&
                        item.getValue(TagMetadata.DELETION_DATE) > 0));
    }

    @Override
    protected boolean createOutstandingEntries(long modelId, ContentValues modelSetValues) {
        Long tagDataId = modelSetValues.getAsLong(TagMetadata.TAG_ID.name);
        String memberId = modelSetValues.getAsString(TagMemberMetadata.USER_UUID.name);
        Long deletionDate = modelSetValues.getAsLong(TagMetadata.DELETION_DATE.name);
        if (tagDataId == null || tagDataId == AbstractModel.NO_ID || RemoteModel.isUuidEmpty(memberId))
            return false;

        TagOutstanding to = new TagOutstanding();
        to.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, tagDataId);
        to.setValue(OutstandingEntry.CREATED_AT_PROPERTY, DateUtilities.now());

        String addedOrRemoved = NameMaps.MEMBER_ADDED_COLUMN;
        if (deletionDate != null && deletionDate > 0)
            addedOrRemoved = NameMaps.MEMBER_REMOVED_COLUMN;

        to.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, addedOrRemoved);
        to.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, memberId);
        database.insert(outstandingTable.name, null, to.getSetValues());
        return true;
    }

    public void createMemberLink(long tagId, String tagUuid, String memberId, boolean suppressOutstanding) {
        TagMetadata newMetadata = TagMemberMetadata.newMemberMetadata(tagId, tagUuid, memberId);
        if (suppressOutstanding)
            newMetadata.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        if (update(Criterion.and(TagMetadataCriteria.byTagAndWithKey(tagUuid, TagMemberMetadata.KEY),
                TagMemberMetadata.USER_UUID.eq(memberId)), newMetadata) <= 0) {
            if (suppressOutstanding)
                newMetadata.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            createNew(newMetadata);
        }
    }

    public void removeMemberLinks(long tagId, String tagUuid, String[] memberIds, boolean suppressOutstanding) {
        TagMetadata deleteTemplate = new TagMetadata();
        deleteTemplate.setValue(TagMetadata.TAG_ID, tagId); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        if (memberIds != null) {
            for (String uuid : memberIds) {
                // TODO: Right now this is in a loop because each deleteTemplate needs the individual tagUuid in order to record
                // the outstanding entry correctly. If possible, this should be improved to a single query
                deleteTemplate.setValue(TagMemberMetadata.USER_UUID, uuid); // Need this for recording changes in outstanding table
                if (suppressOutstanding)
                    deleteTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                update(Criterion.and(TagMetadataCriteria.withKey(TagMemberMetadata.KEY), Metadata.DELETION_DATE.eq(0),
                        TagMetadata.TAG_UUID.eq(tagUuid), TagMemberMetadata.USER_UUID.eq(uuid)), deleteTemplate);
            }
        }
    }

    public void synchronizeMembers(long tagId, String tagUuid, JSONArray members) {
        throw new RuntimeException("IMPLEMENT ME!");
    }
}

