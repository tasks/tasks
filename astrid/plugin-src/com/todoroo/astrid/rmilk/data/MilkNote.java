package com.todoroo.astrid.rmilk.data;

import android.text.TextUtils;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.rmilk.api.data.RtmTaskNote;

/**
 * Metadata entries for a Remember the Milk note. The first RMilk note becomes
 * Astrid's note field, subsequent notes are stored in metadata in this
 * format.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkNote {

    /** metadata key */
    public static final String METADATA_KEY = "rmilk-note"; //$NON-NLS-1$

    /** note id */
    public static final StringProperty ID = Metadata.VALUE1;

    /** note title */
    public static final StringProperty TITLE = Metadata.VALUE2;

    /** note text */
    public static final StringProperty TEXT = Metadata.VALUE3;

    /** note creation date */
    public static final LongProperty CREATED = new LongProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    public static Metadata create(RtmTaskNote note) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(ID, note.getId());
        metadata.setValue(TITLE, note.getTitle());
        metadata.setValue(TEXT, note.getText());
        metadata.setValue(CREATED, note.getCreated().getTime());
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

    /**
     * Turn a note's title and text into an HTML string for notes
     * @param metadata
     * @return
     */
    @SuppressWarnings("nls")
    public static String toTaskDetail(Metadata metadata) {
        String title = metadata.getValue(TITLE);
        String text = metadata.getValue(TEXT);

        String result;
        if(!TextUtils.isEmpty(title))
            result = "<b>" + title + "</b> " + text;
        else
            result = text;

        return result;
    }

}
