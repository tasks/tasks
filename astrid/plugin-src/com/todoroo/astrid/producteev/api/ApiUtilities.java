package com.todoroo.astrid.producteev.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONObject;

import android.text.Html;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;

/**
 * Utilities for working with API responses and JSON objects
 *
 * @author timsu
 *
 */
public final class ApiUtilities {

    private static final SimpleDateFormat timeParser = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss Z", Locale.US); //$NON-NLS-1$

    private static final SimpleDateFormat timeWriter = new SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss Z", Locale.US); //$NON-NLS-1$

    private static final SimpleDateFormat dateWriter = new SimpleDateFormat(
            "yyyy/MM/dd", Locale.US); //$NON-NLS-1$

    /**
     * Utility method to convert PDV time to unix time
     *
     * @param date
     * @param defaultValue
     * @return
     */
    public static long producteevToUnixTime(String value, long defaultValue) {
        synchronized(timeParser) {
            try {
                return DateUtilities.dateToUnixtime(timeParser.parse(value));
            } catch (ParseException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Utility method to convert unix time to PDV time
     * @param time
     * @return
     */
    public static String unixTimeToProducteev(long time) {
        synchronized(timeWriter) {
            return timeWriter.format(DateUtilities.unixtimeToDate(time));
        }
    }

    /**
     * Utility method to convert unix date to PDV date
     * @param time
     * @return
     */
    public static String unixDateToProducteev(long date) {
        synchronized(dateWriter) {
            return dateWriter.format(DateUtilities.unixtimeToDate(date));
        }
    }

    /**
     * Un-escape a Producteev string
     * @param string
     * @return
     */
    public static String decode(String string) {
        string = string.replace("\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
        return Html.fromHtml(string).toString();
    }

    /**
     * Create metadata from json object
     * @param note JSON object with params id_note and message
     * @return
     */
    @SuppressWarnings("nls")
    public static Metadata createNoteMetadata(JSONObject note) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, NoteMetadata.METADATA_KEY);
        metadata.setValue(NoteMetadata.EXT_ID, note.optString("id_note"));
        metadata.setValue(NoteMetadata.EXT_PROVIDER, ProducteevDataService.NOTE_PROVIDER);
        metadata.setValue(NoteMetadata.BODY, ApiUtilities.decode(note.optString("message")));

        long created = ApiUtilities.producteevToUnixTime(note.optString("time_create"), 0);
        metadata.setValue(Metadata.CREATION_DATE, created);

        // TODO if id_creator != yourself, update the title
        metadata.setValue(NoteMetadata.TITLE, DateUtilities.getDateStringWithWeekday(ContextManager.getContext(),
                new Date(created)));
        return metadata;
    }
}
