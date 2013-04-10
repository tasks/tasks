package com.todoroo.astrid.actfm.sync.messages;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.data.WaitingOnMe;

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
    public static final String TABLE_ID_USER_ACTIVITY = "user_activities";
    public static final String TABLE_ID_HISTORY = "history";
    public static final String TABLE_ID_ATTACHMENTS = "task_attachments";
    public static final String TABLE_ID_TASK_LIST_METADATA = "task_list_metadata";
    public static final String TABLE_ID_WAITING_ON_ME = "waiting_on_mes";

    private static final String PUSHED_AT_PREFIX = "pushed_at";
    public static final String PUSHED_AT_TASKS = PUSHED_AT_PREFIX + "_" + TABLE_ID_TASKS;
    public static final String PUSHED_AT_TAGS = PUSHED_AT_PREFIX + "_" + TABLE_ID_TAGS;
    public static final String PUSHED_AT_USERS = PUSHED_AT_PREFIX + "_" + TABLE_ID_USERS;
    public static final String PUSHED_AT_ACTIVITY = PUSHED_AT_PREFIX + "_" + TABLE_ID_USER_ACTIVITY;
    public static final String PUSHED_AT_TASK_LIST_METADATA = PUSHED_AT_PREFIX + "_" + TABLE_ID_TASK_LIST_METADATA;
    public static final String PUSHED_AT_WAITING_ON_ME = PUSHED_AT_PREFIX + "_" + TABLE_ID_WAITING_ON_ME;

    static {
        // Hardcoded local tables mapped to corresponding server names
        TABLE_LOCAL_TO_SERVER = new HashMap<Table, String>();
        TABLE_LOCAL_TO_SERVER.put(Task.TABLE, TABLE_ID_TASKS);
        TABLE_LOCAL_TO_SERVER.put(TagData.TABLE, TABLE_ID_TAGS);
        TABLE_LOCAL_TO_SERVER.put(User.TABLE, TABLE_ID_USERS);
        TABLE_LOCAL_TO_SERVER.put(History.TABLE, TABLE_ID_HISTORY);
        TABLE_LOCAL_TO_SERVER.put(UserActivity.TABLE, TABLE_ID_USER_ACTIVITY);
        TABLE_LOCAL_TO_SERVER.put(TaskAttachment.TABLE, TABLE_ID_ATTACHMENTS);
        TABLE_LOCAL_TO_SERVER.put(TaskListMetadata.TABLE, TABLE_ID_TASK_LIST_METADATA);
        TABLE_LOCAL_TO_SERVER.put(WaitingOnMe.TABLE, TABLE_ID_WAITING_ON_ME);

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
            Set<String> excludedFromOutstandingSet, boolean writeable) {
        propertyMap.put(property, serverName);
        localNameMap.put(property.name, property);
        serverNameMap.put(property.name, serverName);
        if (!writeable && excludedFromOutstandingSet != null)
            excludedFromOutstandingSet.add(property.name);
    }

    public static Property<?>[] syncableProperties(String table) {
        if (TABLE_ID_TASKS.equals(table))
            return computeSyncableProperties(TASK_PROPERTIES_LOCAL_TO_SERVER.keySet(), TASK_PROPERTIES_EXCLUDED);
        else if (TABLE_ID_TAGS.equals(table))
            return computeSyncableProperties(TAG_DATA_PROPERTIES_LOCAL_TO_SERVER.keySet(), TAG_PROPERTIES_EXCLUDED);
        else if (TABLE_ID_USER_ACTIVITY.equals(table))
            return computeSyncableProperties(USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER.keySet(), USER_ACTIVITY_PROPERTIES_EXCLUDED);
        else if (TABLE_ID_ATTACHMENTS.equals(table))
            return computeSyncableProperties(TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER.keySet(), TASK_ATTACHMENT_PROPERTIES_EXCLUDED);
        else if (TABLE_ID_TASK_LIST_METADATA.equals(table))
            return computeSyncableProperties(TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER.keySet(), TASK_LIST_METADATA_PROPERTIES_EXCLUDED);
        else if (TABLE_ID_WAITING_ON_ME.equals(table))
            return computeSyncableProperties(WAITING_ON_ME_PROPERTIES_LOCAL_TO_SERVER.keySet(), WAITING_ON_ME_PROPERTIES_EXCLUDED);
        return null;
    }

    private static Property<?>[] computeSyncableProperties(Set<Property<?>> baseSet, Set<String> excluded) {
        Set<Property<?>> result = new HashSet<Property<?>>();
        for (Property<?> elem : baseSet) {
            if (!excluded.contains(elem.name))
                result.add(elem);
        }
        return result.toArray(new Property<?>[result.size()]);
    }

    // ----------
    // Tasks
    // ----------

    private static final Map<Property<?>, String> TASK_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TASK_PROPERTIES_EXCLUDED;


    private static void putTaskPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES,
                TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        // Hardcoded local columns mapped to corresponding server names
        TASK_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TASK_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TASK_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TASK_PROPERTIES_EXCLUDED = new HashSet<String>();

        putTaskPropertyToServerName(Task.TITLE,           "title",          true);
        putTaskPropertyToServerName(Task.IMPORTANCE,      "importance",     true);
        putTaskPropertyToServerName(Task.DUE_DATE,        "due",            true);
        putTaskPropertyToServerName(Task.HIDE_UNTIL,      "hide_until",     true);
        putTaskPropertyToServerName(Task.CREATION_DATE,   "created_at",     true);
        putTaskPropertyToServerName(Task.COMPLETION_DATE, "completed_at",   true);
        putTaskPropertyToServerName(Task.RECURRENCE,      "repeat",         true);
        putTaskPropertyToServerName(Task.DELETION_DATE,   "deleted_at",     true);
        putTaskPropertyToServerName(Task.NOTES,           "notes",          true);
        putTaskPropertyToServerName(Task.RECURRENCE,      "repeat",         true);
        putTaskPropertyToServerName(Task.USER_ID,         "user_id",        true);
        putTaskPropertyToServerName(Task.CREATOR_ID,      "creator_id",     false);
        putTaskPropertyToServerName(Task.UUID,            "uuid",           false);
        putTaskPropertyToServerName(Task.IS_PUBLIC,       "public",         true);
        putTaskPropertyToServerName(Task.IS_READONLY,     "read_only",      false);
        putTaskPropertyToServerName(Task.CLASSIFICATION,  "classification", false);

        TASK_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TASK_PROPERTIES_LOCAL_TO_SERVER);
    }

    public static final String TAG_ADDED_COLUMN = "tag_added";
    public static final String TAG_REMOVED_COLUMN = "tag_removed";


    // ----------
    // TagData
    // ----------

    private static final Map<Property<?>, String> TAG_DATA_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TAG_DATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TAG_PROPERTIES_EXCLUDED;

    private static void putTagPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES,
                TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, writeable);
    }
    static {
        // Hardcoded local columns mapped to corresponding server names
        TAG_DATA_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TAG_DATA_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TAG_PROPERTIES_EXCLUDED = new HashSet<String>();

        putTagPropertyToServerName(TagData.USER_ID,         "user_id",      true);
        putTagPropertyToServerName(TagData.NAME,            "name",         true);
        putTagPropertyToServerName(TagData.CREATION_DATE,   "created_at",   true);
        putTagPropertyToServerName(TagData.DELETION_DATE,   "deleted_at",   true);
        putTagPropertyToServerName(TagData.UUID,            "uuid",         false);
        putTagPropertyToServerName(TagData.TASK_COUNT,      "task_count",   false);
        putTagPropertyToServerName(TagData.TAG_DESCRIPTION, "description",  true);
        putTagPropertyToServerName(TagData.PICTURE,         "picture",      true);
        putTagPropertyToServerName(TagData.IS_FOLDER,       "is_folder",    false);

        // Reverse the mapping to construct the server to local map
        TAG_DATA_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TAG_DATA_PROPERTIES_LOCAL_TO_SERVER);
    }

    public static final String MEMBER_ADDED_COLUMN = "member_added";
    public static final String MEMBER_REMOVED_COLUMN = "member_removed";



    // ----------
    // Users
    // ----------
    private static final Map<Property<?>, String> USER_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> USER_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> USER_PROPERTIES_EXCLUDED;

    private static void putUserPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, USER_PROPERTIES_LOCAL_TO_SERVER, USER_COLUMN_NAMES_TO_PROPERTIES,
                USER_COLUMNS_LOCAL_TO_SERVER, USER_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        USER_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        USER_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        USER_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        USER_PROPERTIES_EXCLUDED = new HashSet<String>();

        putUserPropertyToServerName(User.UUID,       "uuid",       false);
        putUserPropertyToServerName(User.PICTURE,    "picture",    false);
        putUserPropertyToServerName(User.FIRST_NAME, "first_name", false);
        putUserPropertyToServerName(User.LAST_NAME,  "last_name",  false);
        putUserPropertyToServerName(User.STATUS,     "connection", true);


        // Reverse the mapping to construct the server to local map
        USER_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(USER_PROPERTIES_LOCAL_TO_SERVER);
    }

    // ----------
    // User Activity
    // ----------
    private static final Map<Property<?>, String> USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> USER_ACTIVITY_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_ACTIVITY_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> USER_ACTIVITY_PROPERTIES_EXCLUDED;

    private static void putUserActivityPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER, USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES,
                USER_ACTIVITY_COLUMNS_LOCAL_TO_SERVER, USER_ACTIVITY_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        USER_ACTIVITY_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        USER_ACTIVITY_PROPERTIES_EXCLUDED = new HashSet<String>();

        putUserActivityPropertyToServerName(UserActivity.UUID,        "uuid",        false);
        putUserActivityPropertyToServerName(UserActivity.USER_UUID,   "user_id",     false);
        putUserActivityPropertyToServerName(UserActivity.ACTION,      "action",      true);
        putUserActivityPropertyToServerName(UserActivity.MESSAGE,     "message",     true);
        putUserActivityPropertyToServerName(UserActivity.PICTURE,     "picture",     true);
        putUserActivityPropertyToServerName(UserActivity.TARGET_ID,   "target_id",   true);
        putUserActivityPropertyToServerName(UserActivity.TARGET_NAME, "target_name", false);
        putUserActivityPropertyToServerName(UserActivity.CREATED_AT,  "created_at",  true);
        putUserActivityPropertyToServerName(UserActivity.DELETED_AT,  "deleted_at",  true);


        // Reverse the mapping to construct the server to local map
        USER_ACTIVITY_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER);
    }

    // ----------
    // TaskAttachment
    // ----------
    private static final Map<Property<?>, String> TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_ATTACHMENT_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_ATTACHMENT_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TASK_ATTACHMENT_PROPERTIES_EXCLUDED;

    public static final String ATTACHMENT_ADDED_COLUMN = "file";

    private static void putTaskAttachmentPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER, TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES,
                TASK_ATTACHMENT_COLUMNS_LOCAL_TO_SERVER, TASK_ATTACHMENT_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TASK_ATTACHMENT_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TASK_ATTACHMENT_PROPERTIES_EXCLUDED = new HashSet<String>();

        putTaskAttachmentPropertyToServerName(TaskAttachment.UUID,         "uuid",         false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.USER_UUID,    "user_id",      false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.TASK_UUID,    "task_id",      true);
        putTaskAttachmentPropertyToServerName(TaskAttachment.NAME,         "name",         false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.URL,          "url",          false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.SIZE,         "size",         false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.CONTENT_TYPE, "content_type", false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.CREATED_AT,   "created_at",   true);
        putTaskAttachmentPropertyToServerName(TaskAttachment.DELETED_AT,   "deleted_at",   true);


        // Reverse the mapping to construct the server to local map
        TASK_ATTACHMENT_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER);
    }

    // ----------
    // TaskListMetadata
    // ----------
    private static final Map<Property<?>, String> TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_LIST_METADATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_LIST_METADATA_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> TASK_LIST_METADATA_PROPERTIES_EXCLUDED;

    private static void putTaskListMetadataPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER, TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES,
                TASK_LIST_METADATA_COLUMNS_LOCAL_TO_SERVER, TASK_LIST_METADATA_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        TASK_LIST_METADATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        TASK_LIST_METADATA_PROPERTIES_EXCLUDED = new HashSet<String>();

        putTaskListMetadataPropertyToServerName(TaskListMetadata.UUID,          "uuid",          false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.TAG_UUID,      "tag_id",        true);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.FILTER,        "filter",        true);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.TASK_IDS,      "task_ids",      true);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.SORT,          "sort",          false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.SETTINGS,      "settings",      false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.CHILD_TAG_IDS, "child_tag_ids", false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.IS_COLLAPSED,  "is_collapsed",  false);

        // Reverse the mapping to construct the server to local map
        TASK_LIST_METADATA_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER);
    }

    // ----------
    // WaitingOnMe
    // ----------
    private static final Map<Property<?>, String> WAITING_ON_ME_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> WAITING_ON_ME_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> WAITING_ON_ME_COLUMNS_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> WAITING_ON_ME_PROPERTIES_SERVER_TO_LOCAL;
    private static final Set<String> WAITING_ON_ME_PROPERTIES_EXCLUDED;

    private static void putWaitingOnMePropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, WAITING_ON_ME_PROPERTIES_LOCAL_TO_SERVER, WAITING_ON_ME_COLUMN_NAMES_TO_PROPERTIES,
                WAITING_ON_ME_COLUMNS_LOCAL_TO_SERVER, WAITING_ON_ME_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        WAITING_ON_ME_PROPERTIES_LOCAL_TO_SERVER = new HashMap<Property<?>, String>();
        WAITING_ON_ME_COLUMN_NAMES_TO_PROPERTIES = new HashMap<String, Property<?>>();
        WAITING_ON_ME_COLUMNS_LOCAL_TO_SERVER = new HashMap<String, String>();
        WAITING_ON_ME_PROPERTIES_EXCLUDED = new HashSet<String>();

        putWaitingOnMePropertyToServerName(WaitingOnMe.UUID,            "uuid",            false);
        putWaitingOnMePropertyToServerName(WaitingOnMe.WAITING_USER_ID, "waiting_user_id", false);
        putWaitingOnMePropertyToServerName(WaitingOnMe.TASK_UUID,       "task_id",         false);
        putWaitingOnMePropertyToServerName(WaitingOnMe.WAIT_TYPE,       "wait_type",       false);
        putWaitingOnMePropertyToServerName(WaitingOnMe.CREATED_AT,      "created_at",      false);
        putWaitingOnMePropertyToServerName(WaitingOnMe.DELETED_AT,      "deleted_at",      true);
        putWaitingOnMePropertyToServerName(WaitingOnMe.READ_AT,         "read_at",         true);
        putWaitingOnMePropertyToServerName(WaitingOnMe.ACKNOWLEDGED,    "acknowledged",    true);

        // Reverse the mapping to construct the server to local map
        WAITING_ON_ME_PROPERTIES_SERVER_TO_LOCAL = AndroidUtilities.reverseMap(WAITING_ON_ME_PROPERTIES_LOCAL_TO_SERVER);
    }


    // ----------
    // Mapping helpers
    // ----------

    private static <A, B> B mapColumnName(String table, A col, Map<A, B> taskMap, Map<A, B> tagMap, Map<A, B> userMap,
            Map<A, B> userActivityMap, Map<A, B> taskAttachmentMap, Map<A, B> taskListMetadataMap, Map<A, B> waitingOnMeMap) {
        Map<A, B> map = null;
        if (TABLE_ID_TASKS.equals(table))
            map = taskMap;
        else if (TABLE_ID_TAGS.equals(table))
            map = tagMap;
        else if (TABLE_ID_USERS.equals(table))
            map = userMap;
        else if (TABLE_ID_USER_ACTIVITY.equals(table))
            map = userActivityMap;
        else if (TABLE_ID_ATTACHMENTS.equals(table))
            map = taskAttachmentMap;
        else if (TABLE_ID_TASK_LIST_METADATA.equals(table))
            map = taskListMetadataMap;
        else if (TABLE_ID_WAITING_ON_ME.equals(table))
            map = waitingOnMeMap;

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
        } else if (TABLE_ID_USER_ACTIVITY.equals(table)) {
            if (USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES.containsKey(column))
                return !USER_ACTIVITY_PROPERTIES_EXCLUDED.contains(column);
        } else if (TABLE_ID_USERS.equals(table)) {
            if (USER_COLUMN_NAMES_TO_PROPERTIES.containsKey(column))
                return !USER_PROPERTIES_EXCLUDED.contains(column);
        } else if (TABLE_ID_ATTACHMENTS.equals(table)) {
            if (TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES.containsKey(column))
                return !TASK_ATTACHMENT_PROPERTIES_EXCLUDED.contains(column);
        } else if (TABLE_ID_TASK_LIST_METADATA.equals(table)) {
            if (TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES.containsKey(column))
                return !TASK_LIST_METADATA_PROPERTIES_EXCLUDED.contains(column);
        } else if (TABLE_ID_WAITING_ON_ME.equals(table)) {
            if (WAITING_ON_ME_COLUMN_NAMES_TO_PROPERTIES.containsKey(column))
                return !WAITING_ON_ME_PROPERTIES_EXCLUDED.contains(column);
        }
        return false;
    }

    public static String localPropertyToServerColumnName(String table, Property<?> localProperty) {
        return mapColumnName(table, localProperty, TASK_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_PROPERTIES_LOCAL_TO_SERVER,
                USER_PROPERTIES_LOCAL_TO_SERVER, USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER,
                TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER, TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER,
                WAITING_ON_ME_PROPERTIES_LOCAL_TO_SERVER);
    }

    public static String localColumnNameToServerColumnName(String table, String localColumn) {
        return mapColumnName(table, localColumn, TASK_COLUMNS_LOCAL_TO_SERVER, TAG_DATA_COLUMNS_LOCAL_TO_SERVER,
                USER_COLUMNS_LOCAL_TO_SERVER, USER_ACTIVITY_COLUMNS_LOCAL_TO_SERVER,
                TASK_ATTACHMENT_COLUMNS_LOCAL_TO_SERVER, TASK_LIST_METADATA_COLUMNS_LOCAL_TO_SERVER,
                WAITING_ON_ME_COLUMNS_LOCAL_TO_SERVER);
    }

    public static Property<?> localColumnNameToProperty(String table, String localColumn) {
        return mapColumnName(table, localColumn, TASK_COLUMN_NAMES_TO_PROPERTIES, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES,
                USER_COLUMN_NAMES_TO_PROPERTIES, USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES,
                TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES, TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES,
                WAITING_ON_ME_COLUMN_NAMES_TO_PROPERTIES);
    }

    public static Property<?> serverColumnNameToLocalProperty(String table, String serverColumn) {
        return mapColumnName(table, serverColumn, TASK_PROPERTIES_SERVER_TO_LOCAL, TAG_DATA_PROPERTIES_SERVER_TO_LOCAL,
                USER_PROPERTIES_SERVER_TO_LOCAL, USER_ACTIVITY_PROPERTIES_SERVER_TO_LOCAL,
                TASK_ATTACHMENT_PROPERTIES_SERVER_TO_LOCAL, TASK_LIST_METADATA_PROPERTIES_SERVER_TO_LOCAL,
                WAITING_ON_ME_PROPERTIES_SERVER_TO_LOCAL);
    }

}
