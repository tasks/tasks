/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
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
                        item.getValue(TagMetadata.DELETION_DATE) > 0)) &&
                RemoteModelDao.getOutstandingEntryFlag(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING);
    }

    @Override
    protected int createOutstandingEntries(long modelId, ContentValues modelSetValues) {
        Long tagDataId = modelSetValues.getAsLong(TagMetadata.TAG_ID.name);
        String memberId = modelSetValues.getAsString(TagMemberMetadata.USER_UUID.name);
        Long deletionDate = modelSetValues.getAsLong(TagMetadata.DELETION_DATE.name);
        if (tagDataId == null || tagDataId == AbstractModel.NO_ID || RemoteModel.isUuidEmpty(memberId))
            return -1;

        TagOutstanding to = new TagOutstanding();
        to.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, tagDataId);
        to.setValue(OutstandingEntry.CREATED_AT_PROPERTY, DateUtilities.now());

        String addedOrRemoved = NameMaps.MEMBER_ADDED_COLUMN;
        if (deletionDate != null && deletionDate > 0)
            addedOrRemoved = NameMaps.MEMBER_REMOVED_COLUMN;

        to.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, addedOrRemoved);
        to.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, memberId);
        database.insert(outstandingTable.name, null, to.getSetValues());
        ActFmSyncThread.getInstance().enqueueMessage(new ChangesHappened<TagData, TagOutstanding>(tagDataId, TagData.class,
                PluginServices.getTagDataDao(), PluginServices.getTagOutstandingDao()), null);
        return 1;
    }

    public void createMemberLink(long tagId, String tagUuid, String memberId, boolean suppressOutstanding) {
        createMemberLink(tagId, tagUuid, memberId, false, suppressOutstanding);
    }

    public void createMemberLink(long tagId, String tagUuid, String memberId, boolean removedMember, boolean suppressOutstanding) {
        TagMetadata newMetadata = TagMemberMetadata.newMemberMetadata(tagId, tagUuid, memberId);
        if (removedMember)
            newMetadata.setValue(TagMetadata.DELETION_DATE, DateUtilities.now());
        if (suppressOutstanding)
            newMetadata.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        if (update(Criterion.and(TagMetadataCriteria.byTagAndWithKey(tagUuid, TagMemberMetadata.KEY),
                TagMemberMetadata.USER_UUID.eq(memberId)), newMetadata) <= 0) {
            if (suppressOutstanding)
                newMetadata.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            createNew(newMetadata);
        }
    }

    public void removeMemberLink(long tagId, String tagUuid, String memberId, boolean suppressOutstanding) {
        TagMetadata deleteTemplate = new TagMetadata();
        deleteTemplate.setValue(TagMetadata.TAG_ID, tagId); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(TagMetadata.DELETION_DATE, DateUtilities.now());
        deleteTemplate.setValue(TagMemberMetadata.USER_UUID, memberId); // Need this for recording changes in outstanding table

        if (suppressOutstanding)
            deleteTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        update(Criterion.and(TagMetadataCriteria.withKey(TagMemberMetadata.KEY), TagMetadata.DELETION_DATE.eq(0),
                TagMetadata.TAG_UUID.eq(tagUuid), TagMemberMetadata.USER_UUID.eq(memberId)), deleteTemplate);
    }

    public void removeMemberLinks(long tagId, String tagUuid, String[] memberIds, boolean suppressOutstanding) {
        TagMetadata deleteTemplate = new TagMetadata();
        deleteTemplate.setValue(TagMetadata.TAG_ID, tagId); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(TagMetadata.DELETION_DATE, DateUtilities.now());
        if (memberIds != null) {
            for (String uuid : memberIds) {
                // TODO: Right now this is in a loop because each deleteTemplate needs the individual tagUuid in order to record
                // the outstanding entry correctly. If possible, this should be improved to a single query
                deleteTemplate.setValue(TagMemberMetadata.USER_UUID, uuid); // Need this for recording changes in outstanding table
                if (suppressOutstanding)
                    deleteTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                update(Criterion.and(TagMetadataCriteria.withKey(TagMemberMetadata.KEY), TagMetadata.DELETION_DATE.eq(0),
                        TagMetadata.TAG_UUID.eq(tagUuid), TagMemberMetadata.USER_UUID.eq(uuid)), deleteTemplate);
            }
        }
    }

    public void synchronizeMembers(TagData tagData, String legacyMembersString, String tagUuid, JSONArray members) {
        long tagId = tagData.getId();
        Set<String> emails = new HashSet<String>();
        Set<String> ids = new HashSet<String>();

        HashMap<String, String> idToEmail = new HashMap<String, String>();

        for (int i = 0; i < members.length(); i++) {
            JSONObject person = members.optJSONObject(i);
            if (person != null) {
                String id = person.optString("id"); //$NON-NLS-1$
                if (!TextUtils.isEmpty(id))
                    ids.add(id);

                String email = person.optString("email"); //$NON-NLS-1$
                if (!TextUtils.isEmpty(email))
                    emails.add(email);

                if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(email)) {
                    idToEmail.put(id, email);
                }
            }
        }

        if (!TextUtils.isEmpty(legacyMembersString)) {
            try {
                JSONArray legacyMembers = new JSONArray(legacyMembersString);
                for (int i = 0; i < legacyMembers.length(); i++) {
                    JSONObject user = legacyMembers.optJSONObject(i);
                    if (user != null) {
                        String id = user.optString("id"); //$NON-NLS-1$
                        String email = user.optString("email"); //$NON-NLS-1$

                        if (!TextUtils.isEmpty(id)) {
                            createMemberLink(tagId, tagUuid, id, !ids.contains(id), false);
                        } else if (!TextUtils.isEmpty(email)) {
                            createMemberLink(tagId, tagUuid, email, !emails.contains(email), false);
                        }
                    }

                }
            } catch (JSONException e) {
                //
            }
            tagData.setValue(TagData.MEMBERS, ""); //$NON-NLS-1$
            PluginServices.getTagDataDao().saveExisting(tagData);
        }

        TodorooCursor<TagMetadata> currentMembers = query(Query.select(TagMemberMetadata.USER_UUID).where(TagMetadataCriteria.byTagAndWithKey(tagUuid, TagMemberMetadata.KEY)));
        try {
            TagMetadata m = new TagMetadata();
            for (currentMembers.moveToNext(); !currentMembers.isAfterLast(); currentMembers.moveToNext()) {
                m.clear();
                m.readFromCursor(currentMembers);

                String userId = m.getValue(TagMemberMetadata.USER_UUID);
                boolean exists = ids.remove(userId) || emails.remove(userId);
                if (exists && idToEmail.containsKey(userId)) {
                    String email = idToEmail.get(userId);
                    emails.remove(email);
                }

                if (!exists) { // Was in database, but not in new members list
                    removeMemberLink(tagId, tagUuid, userId, false);
                }
            }
        } finally {
            currentMembers.close();
        }

        for (String email : emails) {
            createMemberLink(tagId, tagUuid, email, false);
        }

        for (String id : ids) {
            createMemberLink(tagId, tagUuid, id, false);
        }
    }

    public boolean tagHasMembers(String uuid) {
        TodorooCursor<TagMetadata> metadata = query(Query.select(TagMetadata.ID).where(Criterion.and(TagMetadataCriteria.byTagAndWithKey(uuid, TagMemberMetadata.KEY), TagMetadata.DELETION_DATE.eq(0))));
        try {
            return metadata.getCount() > 0;
        } finally {
            metadata.close();
        }
    }

    public boolean memberOfTagData(String email, String tagId, String memberId) {
        Criterion criterion;
        if (!RemoteModel.isUuidEmpty(memberId) && !TextUtils.isEmpty(email))
            criterion = Criterion.or(TagMemberMetadata.USER_UUID.eq(email), TagMemberMetadata.USER_UUID.eq(memberId));
        else if (!RemoteModel.isUuidEmpty(memberId))
            criterion = TagMemberMetadata.USER_UUID.eq(memberId);
        else if (!TextUtils.isEmpty(email))
            criterion = TagMemberMetadata.USER_UUID.eq(email);
        else
            return false;

        TodorooCursor<TagMetadata> count = query(Query.select(TagMetadata.ID).where(
                Criterion.and(TagMetadataCriteria.withKey(TagMemberMetadata.KEY), TagMetadata.TAG_UUID.eq(tagId), criterion)));
        try {
            return count.getCount() > 0;
        } finally {
            //
        }
    }
}

