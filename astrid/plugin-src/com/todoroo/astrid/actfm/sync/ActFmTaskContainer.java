package com.todoroo.astrid.actfm.sync;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONObject;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ActFmTaskContainer extends SyncContainer {

    public ActFmTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this.task = task;
        this.metadata = metadata;
    }

    @SuppressWarnings("nls")
    public ActFmTaskContainer(Task task, ArrayList<Metadata> metadata, JSONObject remoteTask) {
        this(task, metadata);
        task.setValue(Task.REMOTE_ID, remoteTask.optLong("id"));
    }

    /** create note metadata from comment json object */
    @SuppressWarnings("nls")
    public static Metadata newNoteMetadata(JSONObject comment) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, NoteMetadata.METADATA_KEY);
        metadata.setValue(NoteMetadata.EXT_ID, comment.optString("id"));
        metadata.setValue(NoteMetadata.EXT_PROVIDER,
                ActFmDataService.NOTE_PROVIDER);

        Date creationDate = new Date(comment.optInt("date") * 1000L);
        metadata.setValue(Metadata.CREATION_DATE, creationDate.getTime());
        metadata.setValue(NoteMetadata.BODY, comment.optString("message"));

        JSONObject owner = comment.optJSONObject("owner");
        metadata.setValue(NoteMetadata.THUMBNAIL, owner.optString("picture"));
        String title = String.format("%s on %s",
                owner.optString("name"),
                DateUtilities.getDateString(ContextManager.getContext(), creationDate));
        metadata.setValue(NoteMetadata.TITLE, title);

        return metadata;
    }

}
