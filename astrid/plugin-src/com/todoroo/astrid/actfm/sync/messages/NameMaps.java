package com.todoroo.astrid.actfm.sync.messages;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    // Universal table identifiers
    public static final String TABLE_ID_TASKS = "tasks";
    public static final String TABLE_ID_TAGS = "tags";
    public static final String TABLE_ID_USERS = "users";
    public static final String TABLE_ID_PUSHED_AT = "pushed_at";

    public static final String PUSHED_AT_TASKS = TABLE_ID_PUSHED_AT + "_" + TABLE_ID_TASKS;
    public static final String PUSHED_AT_TAGS = TABLE_ID_PUSHED_AT + "_" + TABLE_ID_TAGS;

    static {
        // Hardcoded local tables mapped to corresponding server names
        TABLE_LOCAL_TO_SERVER = new HashMap<Table, String>();
        TABLE_LOCAL_TO_SERVER.put(Task.TABLE, TABLE_ID_TASKS);
        TABLE_LOCAL_TO_SERVER.put(TagData.TABLE, TABLE_ID_TAGS);
        TABLE_LOCAL_TO_SERVER.put(User.TABLE, TABLE_ID_USERS);

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
    private static final Map<String, Property<?>> TASK_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TASK_PROPERTIES_EXCLUDED;

    private static void putPropertyToServerName(Property<?> property, String serverName,
            Map<Property<?>, String> propertyMap, Map<String, Property<?>> localNameMap, Map<String, String> serverNameMap,
            Set<String> excludedFromOutstandingSet, boolean excludedFromOustanding) {
        propertyMap.put(property, serverName);
        localNameMap.put(property.name, property);
        serverNameMap.put(property.name, serverName);
        if (excludedFromOustanding && excludedFromOutstandingSet != null)
            excludedFromOutstandingSet.add(property.name);
    }

    static {
        // Hardcoded local columns mapped to corresponding server names
        TASK_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TASK_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TASK_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TASK_PROPERTIES_EXCLUDED = new HashSet<String>();

        putPropertyToServerName(Task.TITLE,           "title",        TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.IMPORTANCE,      "importance",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.DUE_DATE,        "due",          TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.HIDE_UNTIL,      "hide_until",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, true);
        putPropertyToServerName(Task.CREATION_DATE,   "created_at",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.COMPLETION_DATE, "completed_at", TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.RECURRENCE,      "repeat",       TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.DELETION_DATE,   "deleted_at",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.NOTES,           "notes",        TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.RECURRENCE,      "repeat",       TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.USER_ID,         "user_id",      TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(Task.USER,            "user",         TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, true);
        putPropertyToServerName(Task.CREATOR_ID,      "creator_id",   TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, true);
        putPropertyToServerName(Task.UUID,            "uuid",         TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, true);
        putPropertyToServerName(Task.PUSHED_AT,       "pushed_at",    TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES, TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, true);

        TASK_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TASK_PROPERTIES_LOCAL_TO_SERVER);
    }


    private static final Map<Property<?>, String> TAG_DATA_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TAG_DATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TAG_PROPERTIES_EXCLUDED;

    static {
        // Hardcoded local columns mapped to corresponding server names
        TAG_DATA_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TAG_DATA_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TAG_PROPERTIES_EXCLUDED = new HashSet<String>();

        putPropertyToServerName(TagData.USER_ID,       "user_id",      TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(TagData.USER,          "user",         TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false); //TODO: NOT CORRECT
        putPropertyToServerName(TagData.NAME,          "name",         TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(TagData.PICTURE,       "picture_id",   TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false); //TODO: NOT CORRECT
        putPropertyToServerName(TagData.MEMBERS,       "members",      TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false); //TODO: NOT CORRECT
        putPropertyToServerName(TagData.CREATION_DATE, "created_at",   TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(TagData.DELETION_DATE, "deleted_at",   TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false);
        putPropertyToServerName(TagData.UUID,          "uuid",         TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, true);
        putPropertyToServerName(TagData.TAG_ORDERING,  "tag_ordering", TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, false); //TODO: NOT CORRECT
        putPropertyToServerName(TagData.PUSHED_AT,     "pushed_at",    TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, true);

        // Reverse the mapping to construct the server to local map
        TAG_DATA_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TAG_DATA_PROPERTIES_LOCAL_TO_SERVER);
    }

    private static final Map<Property<?>, String> USER_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> USER_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_PROPERTIES_SERVER_TO_LOCAL;

    static {
        USER_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        USER_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        USER_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();

        putPropertyToServerName(User.UUID,       "uuid",       USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES, USER_COLUMNS_LOCAL_TO_SERVER, null, true);
        putPropertyToServerName(User.EMAIL,      "email",      USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES, USER_COLUMNS_LOCAL_TO_SERVER, null, true);
        putPropertyToServerName(User.PICTURE,    "picture",    USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES, USER_COLUMNS_LOCAL_TO_SERVER, null, true);
        putPropertyToServerName(User.PUSHED_AT,  "pushed_at",  USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES, USER_COLUMNS_LOCAL_TO_SERVER, null, true);
        putPropertyToServerName(User.FIRST_NAME, "first_name", USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES, USER_COLUMNS_LOCAL_TO_SERVER, null, true);
        putPropertyToServerName(User.LAST_NAME,  "last_name",  USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES, USER_COLUMNS_LOCAL_TO_SERVER, null, true);


        // Reverse the mapping to construct the server to local map
        USER_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(USER_PROPERTIES_LOCAL_TO_SERVER);
    }

    private static <A, B> B mapColumnName(String table, String col, Map<A, B> taskMap, Map<A, B> tagMap, Map<A, B> userMap) {
        Map<A, B> map = null;
        if (TABLE_ID_TASKS.equals(table))
            map = taskMap;
        else if (TABLE_ID_TAGS.equals(table))
            map = tagMap;
        else if (TABLE_ID_USERS.equals(table))
            map = userMap;

        if (map == null)
            return null;

        return map.get(col);
    }

    public static boolean shouldRecordOutstandingColumnForTable(String table, String column) {
        if (TABLE_ID_TASKS.equals(table)) {
           if (TASK_COLUMN_NAMES_TO_PROPERTIES.containsKey(column))
               return !TASK_PROPERTIES_EXCLUDED.contains(column);
        } else if (TABLE_ID_TAGS.equals(table)) {
            if (TAG_DATA_COLUMN_NAMES_TO_PROPERTIES.containsKey(column))
                return !TAG_PROPERTIES_EXCLUDED.contains(column);
        }
        return false;
    }

    public static String localColumnNameToServerColumnName(String table, String localColumn) {
        return mapColumnName(table, localColumn, TASK_COLUMNS_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER, USER_COLUMNS_LOCAL_TO_SERVER);
    }

    public static Property<?> localColumnNameToProperty(String table, String localColumn) {
        return mapColumnName(table, localColumn, TASK_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES, USER_COLUMN_NAMES_TO_PROPERTIES);
    }

    public static Property<?> serverColumnNameToLocalProperty(String table, String serverColumn) {
        return mapColumnName(table, serverColumn, TASK_PROPERTIES_SERVER_TO_LOCAL, TAG_DATA_PROPERTIES_SERVER_TO_LOCAL, USER_PROPERTIES_SERVER_TO_LOCAL);
    }

}
