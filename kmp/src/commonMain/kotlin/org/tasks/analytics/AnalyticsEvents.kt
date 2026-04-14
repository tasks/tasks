package org.tasks.analytics

object AnalyticsEvents {
    const val APP_OPENED = "Application Opened"
    const val APP_BACKGROUNDED = "Application Backgrounded"
    const val ADD_ACCOUNT = "add_account"
    const val ADD_TASK = "add_task"
    const val COMPLETE_TASK = "complete_task"
    const val INITIAL_SYNC_COMPLETE = "initial_sync_complete"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val SCREEN_ADD_ACCOUNT = "screen_add_account"
    const val SCREEN_WELCOME = "screen_welcome"
    const val SIGN_IN_ERROR = "sign_in_error"
    const val SIGN_IN_PROVIDER_SELECTED = "sign_in_provider_selected"
    const val SORT_CHANGE = "sort_change"
    const val SYNC_UNKNOWN_ACCESS = "sync_unknown_access"

    const val PARAM_FROM_BACKGROUND = "from_background"
    const val PARAM_MESSAGE = "message"
    const val PARAM_PROVIDER = "provider"
    const val PARAM_SELECTION = "selection"
    const val PARAM_SOURCE = "source"
    const val PARAM_TASK_COUNT = "task_count"
    const val PARAM_TYPE = "type"
}
