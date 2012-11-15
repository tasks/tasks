package com.todoroo.astrid.actfm.sync.messages;

import java.util.HashMap;
import java.util.Map;

import com.todoroo.andlib.data.Property;
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

    public static final String SERVER_TABLE_TASKS = "tasks";
    public static final String SERVER_TABLE_TAGS = "tags";
    public static final String SERVER_TABLE_USERS = "users";

    static {
        // Hardcoded local tables mapped to corresponding server names
        TABLE_LOCAL_TO_SERVER = new HashMap<Table, String>();
        TABLE_LOCAL_TO_SERVER.put(Task.TABLE, SERVER_TABLE_TASKS);
        TABLE_LOCAL_TO_SERVER.put(TagData.TABLE, SERVER_TABLE_TAGS);
        TABLE_LOCAL_TO_SERVER.put(User.TABLE, SERVER_TABLE_USERS);

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
    private static final Map<Property<?>, String> TASK_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, String> TASK_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_PROPERTIES_SERVER_TO_LOCAL;

    private static void putPropertyToServerName(Property<?> property, String serverName,
            Map<Property<?>, String> propertyMap, Map<String, String> nameMap) {
        propertyMap.put(property, serverName);
        nameMap.put(property.name, serverName);
    }

    static {
        // Hardcoded local columns mapped to corresponding server names
        TASK_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TASK_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        putPropertyToServerName(Task.TITLE,           "title",        TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.IMPORTANCE,      "importance",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.DUE_DATE,        "due",          TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.HIDE_UNTIL,      "hide_until",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.COMPLETION_DATE, "completed_at", TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.DELETION_DATE,   "deleted_at",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.NOTES,           "notes",        TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.RECURRENCE,      "repeat",       TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.USER_ID,         "user_id",      TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.USER,            "user",         TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.CREATOR_ID,      "creator_id",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.UUID,            "uuid",         TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(Task.PROOF_TEXT,      "proof_text",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMNS_LOCAL_TO_SERVER);

        TASK_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TASK_PROPERTIES_LOCAL_TO_SERVER);
    }


    private static final Map<Property<?>, String> TAG_DATA_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, String> TAG_DATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_PROPERTIES_SERVER_TO_LOCAL;

    static {
        // Hardcoded local columns mapped to corresponding server names
        TAG_DATA_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();

        putPropertyToServerName(TagData.USER_ID,       "user_id",      TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(TagData.USER,          "user",         TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER); //TODO: NOT CORRECT
        putPropertyToServerName(TagData.NAME,          "name",         TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(TagData.PICTURE,       "picture_id",   TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER); //TODO: NOT CORRECT
        putPropertyToServerName(TagData.MEMBERS,       "members",      TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER); //TODO: NOT CORRECT
        putPropertyToServerName(TagData.CREATION_DATE, "created_at",   TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(TagData.DELETION_DATE, "deleted_at",   TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(TagData.UUID,          "uuid",         TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(TagData.PROOF_TEXT,    "proof_text",   TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER);
        putPropertyToServerName(TagData.TAG_ORDERING,  "tag_ordering", TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER); //TODO: NOT CORRECT

        // Reverse the mapping to construct the server to local map
        TAG_DATA_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TAG_DATA_PROPERTIES_LOCAL_TO_SERVER);
    }

    public static String localColumnNameToServerColumnName(String table, String localColumn) {
        Map<String, String> map = null;
        if (SERVER_TABLE_TASKS.equals(table))
            map = TASK_COLUMNS_LOCAL_TO_SERVER;
        else if (SERVER_TABLE_TAGS.equals(table))
            map = TAG_DATA_COLUMNS_LOCAL_TO_SERVER;

        if (map == null)
            return null;

        return map.get(localColumn);
    }

    public static Property<?> serverColumnNameToLocalProperty(String table, String serverColumn) {
        Map<String, Property<?>> map = null;
        if (SERVER_TABLE_TASKS.equals(table))
            map = TASK_PROPERTIES_SERVER_TO_LOCAL;
        else if (SERVER_TABLE_TAGS.equals(table))
            map = TAG_DATA_PROPERTIES_SERVER_TO_LOCAL;

        if (map == null)
            return null;

        return map.get(serverColumn);
    }

}
