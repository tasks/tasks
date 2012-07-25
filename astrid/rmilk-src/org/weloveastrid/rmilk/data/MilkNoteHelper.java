/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.data;

import org.weloveastrid.rmilk.api.data.RtmTaskNote;

import android.text.TextUtils;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.notes.NoteMetadata;

/**
 * Metadata entries for a Remember the Milk note. The first RMilk note becomes
 * Astrid's note field, subsequent notes are stored in metadata in this
 * format.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkNoteHelper {

    /** metadata key */
    public static final String PROVIDER = "rmilk"; //$NON-NLS-1$

    public static Metadata create(RtmTaskNote note) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, NoteMetadata.METADATA_KEY);
        metadata.setValue(NoteMetadata.EXT_ID, note.getId());
        metadata.setValue(NoteMetadata.EXT_PROVIDER, PROVIDER);
        metadata.setValue(NoteMetadata.TITLE, note.getTitle());
        metadata.setValue(NoteMetadata.BODY, note.getText());
        metadata.setValue(Metadata.CREATION_DATE, note.getCreated().getTime());
        return metadata;
    }

    /**
     * Turn a note's title and text into a string
     * @param title
     * @param text
     * @return
     */
    @SuppressWarnings("nls")
    public static String toNoteField(RtmTaskNote note) {
        String title = note.getTitle();
        String text = note.getText();
        if(TextUtils.isEmpty(text) && TextUtils.isEmpty(title))
            return "";
        StringBuilder result = new StringBuilder();
        if(!TextUtils.isEmpty(title)) {
            result.append(title);
            if(!TextUtils.isEmpty(text))
                result.append("\n");
        }
        if(!TextUtils.isEmpty(text)) {
            result.append(text);
        }
        return result.toString();
    }

    /**
     * Turn a string into a note's title and text
     * @param value
     * @return
     */
    @SuppressWarnings("nls")
    public static String[] fromNoteField(String value) {
        String[] result = new String[2];
        int firstLineBreak = value.indexOf('\n');
        if(firstLineBreak > -1 && firstLineBreak + 1 < value.length()) {
            result[0] = value.substring(0, firstLineBreak);
            result[1] = value.substring(firstLineBreak + 1, value.length());
        } else {
            result[0] = "";
            result[1] = value;
        }
        return result;
    }

}
