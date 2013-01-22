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
    private static void putPropertyToServerName(Property<?> property, String serverName,
            Map<Property<?>, String> propertyMap, Map<String, Property<?>> localNameMap, Map<String, String> serverNameMap,
            Set<String> excludedFromOutstandingSet, boolean excludedFromOustanding) {
        propertyMap.put(property, serverName);
        localNameMap.put(property.name, property);
        serverNameMap.put(property.name, serverName);
        if (excludedFromOustanding && excludedFromOutstandingSet != null)
            excludedFromOutstandingSet.add(property.name);
    }

    private static final Map<Property<?>, String> TASK_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TASK_PROPERTIES_EXCLUDED;


    private static void putTaskPropertyToServerName(Property<?> property, String serverName, boolean excludedFromOutstanding) {
        putPropertyToServerName(property, serverName, TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES,
                TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, excludedFromOutstanding);
    }

    static {
        // Hardcoded local columns mapped to corresponding server names
        TASK_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TASK_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TASK_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TASK_PROPERTIES_EXCLUDED = new HashSet<String>();

        putTaskPropertyToServerName(Task.TITLE,           "title",        false);
        putTaskPropertyToServerName(Task.IMPORTANCE,      "importance",   false);
        putTaskPropertyToServerName(Task.DUE_DATE,        "due",          false);
        putTaskPropertyToServerName(Task.HIDE_UNTIL,      "hide_until",   true);
        putTaskPropertyToServerName(Task.CREATION_DATE,   "created_at",   false);
        putTaskPropertyToServerName(Task.COMPLETION_DATE, "completed_at", false);
        putTaskPropertyToServerName(Task.RECURRENCE,      "repeat",       false);
        putTaskPropertyToServerName(Task.DELETION_DATE,   "deleted_at",   false);
        putTaskPropertyToServerName(Task.NOTES,           "notes",        false);
        putTaskPropertyToServerName(Task.RECURRENCE,      "repeat",       false);
        putTaskPropertyToServerName(Task.USER_ID,         "user_id",      false);
        putTaskPropertyToServerName(Task.USER,            "user",         true);
        putTaskPropertyToServerName(Task.CREATOR_ID,      "creator_id",   true);
        putTaskPropertyToServerName(Task.UUID,            "uuid",         true);
        putTaskPropertyToServerName(Task.PUSHED_AT,       "pushed_at",    true);
        putTaskPropertyToServerName(Task.IS_PUBLIC,       "public",       false);
        putTaskPropertyToServerName(Task.IS_READONLY,     "read_only",    true);

        TASK_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TASK_PROPERTIES_LOCAL_TO_SERVER);
    }

    public static final String TAG_ADDED_COLUMN = "tag_added";
    public static final String TAG_REMOVED_COLUMN = "tag_removed";


    private static final Map<Property<?>, String> TAG_DATA_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TAG_DATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TAG_PROPERTIES_EXCLUDED;

    private static void putTagPropertyToServerName(Property<?> property, String serverName, boolean excludedFromOutstanding) {
        putPropertyToServerName(property, serverName, TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES,
                TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, excludedFromOutstanding);
    }
    static {
        // Hardcoded local columns mapped to corresponding server names
        TAG_DATA_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TAG_DATA_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TAG_PROPERTIES_EXCLUDED = new HashSet<String>();

        putTagPropertyToServerName(TagData.USER_ID,         "user_id",      false);
        putTagPropertyToServerName(TagData.NAME,            "name",         false);
        putTagPropertyToServerName(TagData.PICTURE,         "picture_id",   false); //TODO: NOT CORRECT
        putTagPropertyToServerName(TagData.MEMBERS,         "members",      false); //TODO: NOT CORRECT
        putTagPropertyToServerName(TagData.CREATION_DATE,   "created_at",   false);
        putTagPropertyToServerName(TagData.DELETION_DATE,   "deleted_at",   false);
        putTagPropertyToServerName(TagData.UUID,            "uuid",         true);
        putTagPropertyToServerName(TagData.TAG_ORDERING,    "tag_ordering", false); //TODO: NOT CORRECT
        putTagPropertyToServerName(TagData.PUSHED_AT,       "pushed_at",    true);
        putTagPropertyToServerName(TagData.TASK_COUNT,      "task_count",   true);
        putTagPropertyToServerName(TagData.TAG_DESCRIPTION, "description",  false);

        // Reverse the mapping to construct the server to local map
        TAG_DATA_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TAG_DATA_PROPERTIES_LOCAL_TO_SERVER);
    }

    private static final Map<Property<?>, String> USER_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> USER_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_PROPERTIES_SERVER_TO_LOCAL;

    private static void putUserPropertyToServerName(Property<?> property, String serverName, boolean excludedFromOutstanding) {
        putPropertyToServerName(property, serverName, USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES,
                USER_COLUMNS_LOCAL_TO_SERVER, null, excludedFromOutstanding);
    }

    static {
        USER_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        USER_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        USER_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();

        putUserPropertyToServerName(User.UUID,       "uuid",       true);
        putUserPropertyToServerName(User.EMAIL,      "email",      true);
        putUserPropertyToServerName(User.PICTURE,    "picture",    true);
        putUserPropertyToServerName(User.PUSHED_AT,  "pushed_at",  true);
        putUserPropertyToServerName(User.FIRST_NAME, "first_name", true);
        putUserPropertyToServerName(User.LAST_NAME,  "last_name",  true);


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
