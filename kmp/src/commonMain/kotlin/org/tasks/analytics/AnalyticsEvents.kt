package org.tasks.analytics

object AnalyticsEvents {
    const val APP_OPENED = "Application Opened"
    const val APP_BACKGROUNDED = "Application Backgrounded"
    const val ADD_ACCOUNT = "add_account"
    const val ADD_TASK = "add_task"
    const val COMPLETE_TASK = "complete_task"
    const val INITIAL_SYNC_COMPLETE = "initial_sync_complete"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val PRICING_BILLING_TOGGLE = "pricing_billing_toggle"
    const val PRICING_SIGN_IN_CLICK = "pricing_sign_in_click"
    const val PRICING_SPONSOR_CLICK = "pricing_sponsor_click"
    const val PRICING_SUBSCRIBE_CLICK = "pricing_subscribe_click"
    const val RESTORE_ERROR = "restore_error"
    const val RESTORE_NOT_SPONSOR = "restore_not_sponsor"
    const val RESTORE_SELECTION = "restore_selection"
    const val RESTORE_SPONSOR_CLICK = "restore_sponsor_click"
    const val RESTORE_SUCCESS = "restore_success"
    const val SCREEN_ADD_ACCOUNT = "screen_add_account"
    const val SCREEN_PRICING = "screen_pricing"
    const val SCREEN_RESTORE_PURCHASES = "screen_restore_purchases"
    const val SCREEN_WELCOME = "screen_welcome"
    const val SIGN_IN_ERROR = "sign_in_error"
    const val SIGN_IN_PROVIDER_SELECTED = "sign_in_provider_selected"
    const val SETTINGS_CLICK = "settings_click"
    const val SORT_CHANGE = "sort_change"
    const val SYNC_UNKNOWN_ACCESS = "sync_unknown_access"

    const val PARAM_FROM_BACKGROUND = "from_background"
    const val PARAM_MESSAGE = "message"
    const val PARAM_PERIOD = "period"
    const val PARAM_PROVIDER = "provider"
    const val PARAM_SELECTION = "selection"
    const val PARAM_SOURCE = "source"
    const val PARAM_TASK_COUNT = "task_count"
    const val PARAM_TIER = "tier"
    const val PARAM_TYPE = "type"

    const val PERIOD_ANNUAL = "annual"
    const val PERIOD_MONTHLY = "monthly"
    const val SELECTION_GITHUB = "github"
    const val SELECTION_GOOGLE_PLAY = "google_play"
    const val SOURCE_SETTINGS = "settings"
    const val TIER_CLOUD = "cloud"
    const val TIER_NYP = "nyp"

    const val CREATE_LIST = "create_list"

    const val CLOUD_ONBOARDING = "cloud_onboarding"
    const val PARAM_STEP = "step"

    object CloudOnboarding {
        const val TRIGGERED = "triggered"
        const val WELCOME = "welcome"
        const val SIGN_IN = "sign_in"
        const val SIGNED_IN = "signed_in"
        const val CREATE_LIST = "create_list"
        const val DONE = "done"
    }

    object SettingsClick {
        const val DELETE_LIST = "delete_list"
        const val WHATS_NEW = "whats_new"
        const val RATE_TASKS = "rate_tasks"
        const val DOCUMENTATION = "documentation"
        const val ISSUE_TRACKER = "issue_tracker"
        const val CONTACT_DEVELOPER = "contact_developer"
        const val SEND_LOGS = "send_logs"
        const val REDDIT = "reddit"
        const val TWITTER = "twitter"
        const val SOURCE_CODE = "source_code"
        const val THIRD_PARTY_LICENSES = "third_party_licenses"
        const val TOS = "tos"
        const val PRIVACY_POLICY = "privacy_policy"
    }
}

fun Analytics.logCloudOnboarding(step: String) =
    logEvent(AnalyticsEvents.CLOUD_ONBOARDING, AnalyticsEvents.PARAM_STEP to step)
