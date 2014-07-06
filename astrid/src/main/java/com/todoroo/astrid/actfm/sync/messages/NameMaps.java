package com.todoroo.astrid.actfm.sync.messages;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.UserActivity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NameMaps {

    // --------------------------------
    // ---- Table name mappings -------
    // --------------------------------

    // Universal table identifiers
    public static final String TABLE_ID_USER_ACTIVITY = "user_activities";

    // --------------------------------
    // ---- Column name mappings -------
    // --------------------------------
    private static void putPropertyToServerName(Property<?> property, String serverName,
            Map<Property<?>, String> propertyMap, Map<String, Property<?>> localNameMap, Map<String, String> serverNameMap,
            Set<String> excludedFromOutstandingSet, boolean writeable) {
        propertyMap.put(property, serverName);
        localNameMap.put(property.name, property);
        serverNameMap.put(property.name, serverName);
        if (!writeable && excludedFromOutstandingSet != null) {
            excludedFromOutstandingSet.add(property.name);
        }
    }

    // ----------
    // Tasks
    // ----------

    private static final Map<Property<?>, String> TASK_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_COLUMNS_LOCAL_TO_SERVER;
    private static final Set<String> TASK_PROPERTIES_EXCLUDED;


    private static void putTaskPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TASK_PROPERTIES_LOCAL_TO_SERVER, TASK_COLUMN_NAMES_TO_PROPERTIES,
                TASK_COLUMNS_LOCAL_TO_SERVER, TASK_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        // Hardcoded local columns mapped to corresponding server names
        TASK_PROPERTIES_LOCAL_TO_SERVER = new HashMap<>();
        TASK_COLUMNS_LOCAL_TO_SERVER = new HashMap<>();
        TASK_COLUMN_NAMES_TO_PROPERTIES = new HashMap<>();
        TASK_PROPERTIES_EXCLUDED = new HashSet<>();

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
        putTaskPropertyToServerName(Task.UUID,            "uuid",           false);
        putTaskPropertyToServerName(Task.CLASSIFICATION,  "classification", false);
    }

    // ----------
    // TagData
    // ----------

    private static final Map<Property<?>, String> TAG_DATA_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TAG_DATA_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TAG_DATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Set<String> TAG_PROPERTIES_EXCLUDED;

    private static void putTagPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TAG_DATA_PROPERTIES_LOCAL_TO_SERVER, TAG_DATA_COLUMN_NAMES_TO_PROPERTIES,
                TAG_DATA_COLUMNS_LOCAL_TO_SERVER, TAG_PROPERTIES_EXCLUDED, writeable);
    }
    static {
        // Hardcoded local columns mapped to corresponding server names
        TAG_DATA_PROPERTIES_LOCAL_TO_SERVER = new HashMap<>();
        TAG_DATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<>();
        TAG_DATA_COLUMN_NAMES_TO_PROPERTIES = new HashMap<>();
        TAG_PROPERTIES_EXCLUDED = new HashSet<>();

        putTagPropertyToServerName(TagData.NAME,            "name",         true);
        putTagPropertyToServerName(TagData.DELETION_DATE,   "deleted_at",   true);
        putTagPropertyToServerName(TagData.UUID,            "uuid",         false);
        putTagPropertyToServerName(TagData.TASK_COUNT,      "task_count",   false);
    }

    // ----------
    // User Activity
    // ----------
    private static final Map<Property<?>, String> USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> USER_ACTIVITY_COLUMNS_LOCAL_TO_SERVER;
    private static final Set<String> USER_ACTIVITY_PROPERTIES_EXCLUDED;

    private static void putUserActivityPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER, USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES,
                USER_ACTIVITY_COLUMNS_LOCAL_TO_SERVER, USER_ACTIVITY_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        USER_ACTIVITY_PROPERTIES_LOCAL_TO_SERVER = new HashMap<>();
        USER_ACTIVITY_COLUMN_NAMES_TO_PROPERTIES = new HashMap<>();
        USER_ACTIVITY_COLUMNS_LOCAL_TO_SERVER = new HashMap<>();
        USER_ACTIVITY_PROPERTIES_EXCLUDED = new HashSet<>();

        putUserActivityPropertyToServerName(UserActivity.UUID,        "uuid",        false);
        putUserActivityPropertyToServerName(UserActivity.ACTION,      "action",      true);
        putUserActivityPropertyToServerName(UserActivity.MESSAGE,     "message",     true);
        putUserActivityPropertyToServerName(UserActivity.PICTURE,     "picture",     true);
        putUserActivityPropertyToServerName(UserActivity.TARGET_ID,   "target_id",   true);
        putUserActivityPropertyToServerName(UserActivity.TARGET_NAME, "target_name", false);
        putUserActivityPropertyToServerName(UserActivity.CREATED_AT,  "created_at",  true);
        putUserActivityPropertyToServerName(UserActivity.DELETED_AT,  "deleted_at",  true);
    }

    // ----------
    // TaskAttachment
    // ----------
    private static final Map<Property<?>, String> TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_ATTACHMENT_COLUMNS_LOCAL_TO_SERVER;
    private static final Set<String> TASK_ATTACHMENT_PROPERTIES_EXCLUDED;

    private static void putTaskAttachmentPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER, TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES,
                TASK_ATTACHMENT_COLUMNS_LOCAL_TO_SERVER, TASK_ATTACHMENT_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        TASK_ATTACHMENT_PROPERTIES_LOCAL_TO_SERVER = new HashMap<>();
        TASK_ATTACHMENT_COLUMN_NAMES_TO_PROPERTIES = new HashMap<>();
        TASK_ATTACHMENT_COLUMNS_LOCAL_TO_SERVER = new HashMap<>();
        TASK_ATTACHMENT_PROPERTIES_EXCLUDED = new HashSet<>();

        putTaskAttachmentPropertyToServerName(TaskAttachment.UUID,         "uuid",         false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.TASK_UUID,    "task_id",      true);
        putTaskAttachmentPropertyToServerName(TaskAttachment.NAME,         "name",         false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.URL,          "url",          false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.SIZE,         "size",         false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.CONTENT_TYPE, "content_type", false);
        putTaskAttachmentPropertyToServerName(TaskAttachment.CREATED_AT,   "created_at",   true);
        putTaskAttachmentPropertyToServerName(TaskAttachment.DELETED_AT,   "deleted_at",   true);
    }

    // ----------
    // TaskListMetadata
    // ----------
    private static final Map<Property<?>, String> TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER;
    private static final Map<String, Property<?>> TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES;
    private static final Map<String, String> TASK_LIST_METADATA_COLUMNS_LOCAL_TO_SERVER;
    private static final Set<String> TASK_LIST_METADATA_PROPERTIES_EXCLUDED;

    private static void putTaskListMetadataPropertyToServerName(Property<?> property, String serverName, boolean writeable) {
        putPropertyToServerName(property, serverName, TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER, TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES,
                TASK_LIST_METADATA_COLUMNS_LOCAL_TO_SERVER, TASK_LIST_METADATA_PROPERTIES_EXCLUDED, writeable);
    }

    static {
        TASK_LIST_METADATA_PROPERTIES_LOCAL_TO_SERVER = new HashMap<>();
        TASK_LIST_METADATA_COLUMN_NAMES_TO_PROPERTIES = new HashMap<>();
        TASK_LIST_METADATA_COLUMNS_LOCAL_TO_SERVER = new HashMap<>();
        TASK_LIST_METADATA_PROPERTIES_EXCLUDED = new HashSet<>();

        putTaskListMetadataPropertyToServerName(TaskListMetadata.UUID,          "uuid",          false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.TAG_UUID,      "tag_id",        true);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.FILTER,        "filter",        true);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.TASK_IDS,      "task_ids",      true);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.SORT,          "sort",          false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.SETTINGS,      "settings",      false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.CHILD_TAG_IDS, "child_tag_ids", false);
        putTaskListMetadataPropertyToServerName(TaskListMetadata.IS_COLLAPSED,  "is_collapsed",  false);
    }
}
