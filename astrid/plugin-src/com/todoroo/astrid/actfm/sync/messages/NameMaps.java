package com.todoroo.astrid.actfm.sync.messages;

import java.util.HashMap;
import java.util.Map;

import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;

@SuppressWarnings("nls")
public class NameMaps {

    // --------------------------------
    // ---- Table name mappings -------
    // --------------------------------
    private static final Map<Table, String> TABLE_LOCAL_TO_SERVER;
    private static final Map<String, Table> TABLE_SERVER_TO_LOCAL;

    static {
        // Hardcoded local tables mapped to corresponding server names
        TABLE_LOCAL_TO_SERVER = new HashMap<Table, String>();
        TABLE_LOCAL_TO_SERVER.put(Task.TABLE, "tasks");
        TABLE_LOCAL_TO_SERVER.put(TagData.TABLE, "tags");
        TABLE_LOCAL_TO_SERVER.put(User.TABLE, "users");

        // Reverse the mapping to construct the server to local map
        TABLE_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TABLE_LOCAL_TO_SERVER);
    }

    public static String getServerNameForTable(Table table) {
        return TABLE_LOCAL_TO_SERVER.get(table);
    }

    public static Table getLocalTableForServerName(String serverName) {
        return TABLE_SERVER_TO_LOCAL.get(serverName);
    }


    // --------------------------------
    // ---- Column name mappings -------
    // --------------------------------
    private static final Map<String, String> TASK_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, String> TASK_COLUMNS_SERVER_TO_LOCAL;

    static {
        // Hardcoded local columns mapped to corresponding server names
        TASK_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.TITLE.name, "title");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.IMPORTANCE.name, "importance");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.DUE_DATE.name, "due");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.HIDE_UNTIL.name, "hide_until");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.COMPLETION_DATE.name, "completed_at");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.DELETION_DATE.name, "deleted_at");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.NOTES.name, "notes");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.RECURRENCE.name, "repeat");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.USER_ID.name, "user_id");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.USER.name, "user"); // TODO: NOT CORRECT
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.CREATOR_ID.name, "creator_id");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.SHARED_WITH.name, "shared_with"); //TODO: NOT CORRECT
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.UUID.name, "uuid");
        TASK_COLUMNS_LOCAL_TO_SERVER.put(Task.PROOF_TEXT.name, "proof_text");

        // Reverse the mapping to construct the server to local map
        TASK_COLUMNS_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TASK_COLUMNS_LOCAL_TO_SERVER);
    }

    private static final Map<String, String> TAG_DATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, String> TAG_DATA_COLUMNS_SERVER_TO_LOCAL;

    static {
        // Hardcoded local columns mapped to corresponding server names
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.USER_ID.name, "user_id");
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.USER.name, "user"); //TODO: NOT CORRECT
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.NAME.name, "name");
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.PICTURE.name, "picture_id"); //TODO: NOT CORRECT
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.MEMBERS.name, "members"); //TODO: NOT CORRECT
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.CREATION_DATE.name, "created_at");
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.DELETION_DATE.name, "deleted_at");
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.UUID.name, "uuid");
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.PROOF_TEXT.name, "proof_text");
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER.put(TagData.TAG_ORDERING.name, "tag_ordering"); //TODO: NOT CORRECT

        // Reverse the mapping to construct the server to local map
        TAG_DATA_COLUMNS_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TAG_DATA_COLUMNS_LOCAL_TO_SERVER);
    }

    public static String localColumnNameToServerColumnName(Table table, String localColumn) {
        Map<String, String> map = null;
        if (table == Task.TABLE)
            map = TASK_COLUMNS_LOCAL_TO_SERVER;
        else if (table == TagData.TABLE)
            map = TAG_DATA_COLUMNS_LOCAL_TO_SERVER;

        if (map == null)
            return null;

        return map.get(localColumn);
    }

    public static String serverColumnNameToLocalColumnName(Table table, String serverColumn) {
        Map<String, String> map = null;
        if (table == Task.TABLE)
            map = TASK_COLUMNS_SERVER_TO_LOCAL;
        else if (table == TagData.TABLE)
            map = TAG_DATA_COLUMNS_SERVER_TO_LOCAL;

        if (map == null)
            return null;

        return map.get(serverColumn);
    }

}
