package com.todoroo.astrid.producteev.sync;

import org.json.JSONObject;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.producteev.api.ApiUtilities;

/**
 * Metadata entries for a Producteev note. The first Producteev note becomes
 * Astrid's note field, subsequent notes are stored in metadata in this
 * format.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevNote {

    /** metadata key */
    public static final String METADATA_KEY = "producteev-note"; //$NON-NLS-1$

    /** note id */
    public static final LongProperty ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** note message */
    public static final StringProperty MESSAGE = Metadata.VALUE2;

    /** note creation date */
    public static final LongProperty CREATED = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /**
     * Create metadata from json object
     * @param note JSON object with params id_note and message
     * @return
     */
    @SuppressWarnings("nls")
    public static Metadata create(JSONObject note) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(ID, note.optLong("id_note"));
        metadata.setValue(MESSAGE, ApiUtilities.decode(note.optString("message")));
        metadata.setValue(CREATED, ApiUtilities.producteevToUnixTime(
                note.optString("time_create"), 0));
        return metadata;
    }

}
