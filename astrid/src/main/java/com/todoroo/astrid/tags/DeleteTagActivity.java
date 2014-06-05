package com.todoroo.astrid.tags;

import android.content.Intent;
import android.widget.Toast;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.TagDataService;

import org.tasks.R;

import javax.inject.Inject;

public class DeleteTagActivity extends TagActivity {

    @Inject TagDataDao tagDataDao;
    @Inject TagDataService tagDataService;
    @Inject MetadataDao metadataDao;

    @Override
    protected void showDialog() {
        DialogUtilities.okCancelDialog(this, getString(R.string.DLG_delete_this_tag_question, tag), getOkListener(), getCancelListener());
    }

    @Override
    protected Intent ok() {
        int deleted = deleteTagMetadata(uuid);
        TagData tagData = tagDataDao.fetch(uuid, TagData.ID, TagData.UUID, TagData.DELETION_DATE, TagData.MEMBER_COUNT, TagData.USER_ID);
        Intent tagDeleted = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED);
        if (tagData != null) {
            tagData.setDeletionDate(DateUtilities.now());
            tagDataService.save(tagData);
            tagDeleted.putExtra(TagViewFragment.EXTRA_TAG_UUID, tagData.getUuid());
        }
        Toast.makeText(this, getString(R.string.TEA_tags_deleted, tag, deleted), Toast.LENGTH_SHORT).show();

        sendBroadcast(tagDeleted);
        return tagDeleted;
    }

    private int deleteTagMetadata(String uuid) {
        Metadata deleted = new Metadata();
        deleted.setDeletionDate(DateUtilities.now());

        return metadataDao.update(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)), deleted);
    }
}

