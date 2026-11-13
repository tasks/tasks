package org.tasks.analytics

import org.tasks.billing.BillingProvider
import org.tasks.billing.SubscriptionSkus

object AnalyticsEvents {
    const val APP_OPENED = "Application Opened"
    const val APP_BACKGROUNDED = "Application Backgrounded"
    const val CONTENT_PROVIDER_API = "cp_api"
    const val CONTENT_PROVIDER_TASKS = "cp_tasks"
    const val CONTENT_PROVIDER_ASTRID2 = "cp_astrid2"

    const val MCP_TOOL_USED = "mcp_tool_used"
    const val APP_FUNCTION_CALL = "app_function_call"
    const val APP_INTENT_CALL = "app_intent_call"

    const val PARAM_PACKAGE = "package"
    const val PARAM_COLLECTION = "collection"
    const val PARAM_ACCESS = "access"

    const val ADD_ACCOUNT = "add_account"
    const val ADD_TASK = "add_task"
    const val COMPLETE_TASK = "complete_task"
    const val BILLING_FLOW_RESULT = "billing_flow_result"
    const val INITIAL_SYNC_COMPLETE = "initial_sync_complete"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val PRICING_BILLING_TOGGLE = "pricing_billing_toggle"
    const val PRICING_CLOSE = "pricing_close"
    const val PRICING_NYP_SELECT = "pricing_nyp_select"
    const val PRICING_OPEN_PORTAL = "pricing_open_portal"
    const val PRICING_REOPEN_CHECKOUT = "pricing_reopen_checkout"
    const val PRICING_SIGN_IN_CLICK = "pricing_sign_in_click"
    const val PRICING_SUBSCRIBE_CLICK = "pricing_subscribe_click"
    const val RESTORE_ERROR = "restore_error"
    const val RESTORE_NOT_SPONSOR = "restore_not_sponsor"
    const val RESTORE_SELECTION = "restore_selection"
    const val RESTORE_SUCCESS = "restore_success"
    const val SCREEN_ADD_ACCOUNT = "screen_add_account"
    const val SCREEN_RESTORE_PURCHASES = "screen_restore_purchases"
    const val SCREEN_WELCOME = "screen_welcome"
    const val SHOWED_PURCHASE_DIALOG = "showed_purchase_dialog"
    const val SIGN_IN_ERROR = "sign_in_error"
    const val SIGN_IN_PROVIDER_SELECTED = "sign_in_provider_selected"
    const val SETTINGS_CLICK = "settings_click"
    const val SORT_CHANGE = "sort_change"
    const val SYNC_ADD_ACCOUNT = "sync_add_account"
    const val SYNC_NOT_FOUND = "sync_not_found"
    const val SYNC_UNKNOWN_ACCESS = "sync_unknown_access"

    const val PARAM_FROM_BACKGROUND = "from_background"
    const val PARAM_LATE = "late"
    const val PARAM_MESSAGE = "message"
    const val PARAM_ORDER_ID = "order_id"
    const val PARAM_PERIOD = "period"
    const val PARAM_PRICE = "price"
    const val PARAM_PROVIDER = "provider"
    const val PARAM_RESULT = "result"
    const val PARAM_SELECTION = "selection"
    const val PARAM_SKU = "sku"
    const val PARAM_SOURCE = "source"
    const val PARAM_STATE = "state"
    const val PARAM_TASK_COUNT = "task_count"
    const val PARAM_TIER = "tier"
    const val PARAM_TYPE = "type"

    const val PERIOD_ANNUAL = "annual"
    const val PERIOD_MONTHLY = "monthly"
    const val SELECTION_CUSTOM = "custom"
    const val SELECTION_GITHUB = "github"
    const val SELECTION_GOOGLE_PLAY = "google_play"
    const val SELECTION_PRESET = "preset"
    const val SELECTION_SLIDER = "slider"
    const val SELECTION_STRIPE = "stripe"
    const val SOURCE_COLORS = "colors"
    const val SOURCE_DRAWER = "drawer"
    const val SOURCE_ONBOARDING = "onboarding"
    const val SOURCE_PAYWALL = "paywall"
    const val SOURCE_SETTINGS = "settings"
    const val SOURCE_THEMES = "themes"
    const val TIER_CLOUD = "cloud"
    const val TIER_NYP = "nyp"

    const val CREATE_LIST = "create_list"
    const val CREATE_TAG = "create_tag"

    const val CLOUD_ONBOARDING = "cloud_onboarding"
    const val PARAM_STEP = "step"

    object BillingFlow {
        const val ACKNOWLEDGE_FAILED = "ACKNOWLEDGE_FAILED"
        const val LAUNCHED = "LAUNCHED"
        const val LAUNCH_FAILED = "LAUNCH_FAILED"
        const val PURCHASED = "PURCHASED"
        const val PURCHASE_FAILED = "PURCHASE_FAILED"
        const val PURCHASE_UPDATED_FAILED = "PURCHASE_UPDATED_FAILED"
        const val QUERY_PURCHASES_FAILED = "QUERY_PURCHASES_FAILED"
        const val QUERY_SKUS_FAILED = "QUERY_SKUS_FAILED"
        const val SUBSCRIPTION_CENTER = "SUBSCRIPTION_CENTER"
    }

    object Checkout {
        const val ACCOUNT_IN_USE = "account_in_use"
        const val ACCOUNT_LINK_CONFLICT = "account_link_conflict"
        const val BROWSER_CLOSED = "browser_closed"
        const val CANCELLED = "cancelled"
        const val CARD_DECLINED = "card_declined"
        const val CHANGED = "changed"
        const val EMPTY_CATALOG = "empty_catalog"
        const val ENCRYPTION_FAILED = "encryption_failed"
        const val ENDED = "ended"
        const val EXPIRED = "expired"
        const val FAILED = "failed"
        const val GONE = "gone"
        const val INVALID_CODE = "invalid_code"
        const val INVALID_EMAIL = "invalid_email"
        const val NEEDS_UPGRADE = "needs_upgrade"
        const val NOT_LINKED = "not_linked"
        const val NOTHING_TO_RESTORE = "nothing_to_restore"
        const val NO_PROOF_OF_PURCHASE = "no_proof_of_purchase"
        const val PURCHASE_GONE = "purchase_gone"
        const val RATE_LIMITED = "rate_limited"
        const val RESTORED = "restored"
        const val RESTORE_REQUIRED = "restore_required"
        const val RESUMED = "resumed"
        const val STORAGE_FAILED = "storage_failed"
        const val SUBSCRIPTION_INVALID = "subscription_invalid"
        const val SUPERSEDED = "superseded"
        const val TOO_MANY_ATTEMPTS = "too_many_attempts"
        const val UNAVAILABLE = "unavailable"
        const val UNKNOWN_PRICE = "unknown_price"
        const val UNREACHABLE = "unreachable"
        const val USER_CANCELED = "USER_CANCELED"
    }

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
        const val DELETE_TAG = "delete_tag"
        const val WHATS_NEW = "whats_new"
        const val RATE_TASKS = "rate_tasks"
        const val DOCUMENTATION = "documentation"
        const val ISSUE_TRACKER = "issue_tracker"
        const val CONTACT_DEVELOPER = "contact_developer"
        const val SEND_LOGS = "send_logs"
        const val REDDIT = "reddit"
        const val SOURCE_CODE = "source_code"
        const val THIRD_PARTY_LICENSES = "third_party_licenses"
        const val TOS = "tos"
        const val PRIVACY_POLICY = "privacy_policy"
    }
}

val BillingProvider.analyticsName: String
    get() = name.lowercase()

fun Analytics.logBillingFlow(
    provider: BillingProvider,
    state: String,
    sku: String? = null,
    result: String? = null,
    vararg params: Pair<String, Any>,
) = logEvent(
    AnalyticsEvents.BILLING_FLOW_RESULT,
    *listOfNotNull(
        AnalyticsEvents.PARAM_PROVIDER to provider.analyticsName,
        AnalyticsEvents.PARAM_STATE to state,
        sku?.let { AnalyticsEvents.PARAM_SKU to it },
        sku?.takeIf { SubscriptionSkus.isProSubscription(it) }?.let {
            AnalyticsEvents.PARAM_PERIOD to if (SubscriptionSkus.isMonthly(it)) {
                AnalyticsEvents.PERIOD_MONTHLY
            } else {
                AnalyticsEvents.PERIOD_ANNUAL
            }
        },
        result?.let { AnalyticsEvents.PARAM_RESULT to it.take(100) },
    ).toTypedArray(),
    *params,
)

fun Analytics.logCloudOnboarding(step: String) =
    logEvent(AnalyticsEvents.CLOUD_ONBOARDING, AnalyticsEvents.PARAM_STEP to step)
