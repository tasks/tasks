package org.tasks.analytics;

import org.tasks.R;

public class Tracking {

  public enum Events {
    SET_DEFAULT_LIST(R.string.tracking_category_preferences, R.string.p_default_list),
    SET_BADGE_LIST(R.string.tracking_category_preferences, R.string.p_badge_list),
    DEFAULT_REMOTE_LIST(R.string.tracking_category_preferences, R.string.p_default_remote_list),
    SET_THEME(R.string.tracking_category_preferences, R.string.p_theme),
    SET_COLOR(R.string.tracking_category_preferences, R.string.p_theme_color),
    SET_ACCENT(R.string.tracking_category_preferences, R.string.p_theme_accent),
    SET_LAUNCHER(R.string.tracking_category_preferences, R.string.p_theme_launcher),
    SET_TAG_COLOR(R.string.tracking_category_tags, R.string.p_theme_color),
    WIDGET_ADD(R.string.tracking_category_widget, R.string.tracking_action_add),
    TIMER_START(R.string.tracking_category_timer, R.string.tracking_action_start),
    GTASK_ENABLED(R.string.tracking_category_google_tasks, R.string.tracking_action_on),
    GTASK_DISABLED(R.string.tracking_category_google_tasks, R.string.tracking_action_off),
    GTASK_NEW_LIST(R.string.tracking_category_google_tasks, R.string.tracking_action_new_list),
    GTASK_RENAME_LIST(
        R.string.tracking_category_google_tasks, R.string.tracking_action_rename_list),
    GTASK_DELETE_LIST(
        R.string.tracking_category_google_tasks, R.string.tracking_action_delete_list),
    GTASK_SET_COLOR(R.string.tracking_category_google_tasks, R.string.p_theme_color),
    GTASK_CLEAR_COMPLETED(
        R.string.tracking_category_google_tasks, R.string.tracking_action_clear_completed),
    MULTISELECT_DELETE(
        R.string.tracking_category_event, R.string.tracking_event_multiselect_delete),
    MULTISELECT_CLONE(R.string.tracking_category_event, R.string.tracking_event_multiselect_clone),
    MULTISELECT_MOVE(R.string.tracking_category_event, R.string.tracking_event_multiselect_move),
    CLEAR_COMPLETED(R.string.tracking_category_event, R.string.tracking_action_clear_completed),
    UPGRADE(R.string.tracking_category_event, R.string.tracking_event_upgrade),
    TASK_CREATION_FAILED(R.string.tracking_category_error, R.string.tracking_event_task_creation),
    NIGHT_MODE_MISMATCH(
        R.string.tracking_category_event, R.string.tracking_event_night_mode_mismatch),
    SET_PREFERENCE(R.string.tracking_category_preferences, 0),
    PLAY_SERVICES_WARNING(
        R.string.tracking_category_event, R.string.tracking_event_play_services_error),
    RECURRENCE_CUSTOM(R.string.tracking_category_recurrence, R.string.tracking_action_custom),
    RECURRENCE_PRESET(R.string.tracking_category_recurrence, R.string.tracking_action_preset),
    TASKER_CREATE(R.string.tracking_category_tasker, R.string.tracking_action_task_created),
    TASKER_LIST_NOTIFICATION(
        R.string.tracking_category_tasker, R.string.tracking_action_list_notification),
    IMPORT_XML(R.string.tracking_category_backup, R.string.tracking_action_import_xml),
    IMPORT_JSON(R.string.tracking_category_backup, R.string.tracking_action_import_json),
    EXPORT(R.string.tracking_category_backup, R.string.tracking_action_export),
    CALDAV_ACCOUNT_ADDED(R.string.tracking_category_caldav, R.string.tracking_action_add_account),
    CALDAV_ACCOUNT_REMOVED(
        R.string.tracking_category_caldav, R.string.tracking_action_remove_account),
    CALDAV_LIST_ADDED(R.string.tracking_category_caldav, R.string.tracking_action_new_list),
    CALDAV_LIST_DELETED(R.string.tracking_category_caldav, R.string.tracking_action_delete_list);

    public final int category;
    public final int action;

    Events(int category, int action) {
      this.category = category;
      this.action = action;
    }
  }
}
