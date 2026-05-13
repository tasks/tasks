package org.tasks.presentation

import android.content.Context
import com.google.android.horologist.data.TargetNodeId

private const val PREFS_NAME = "phone_connection"
private const val KEY_NODE_ID = "phone_node_id"
private const val KEY_VERSION_PREFIX = "phone_version_"

fun Context.savePhoneNodeId(nodeId: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_NODE_ID, nodeId)
        .commit()
}

fun Context.clearPhoneNodeId() {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_NODE_ID)
        .commit()
}

fun Context.phoneTargetNodeId(): TargetNodeId {
    val nodeId = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_NODE_ID, null)
    return if (nodeId != null) {
        TargetNodeId.SpecificNodeId(nodeId)
    } else {
        TargetNodeId.PairedPhone
    }
}

fun Context.getCachedPhoneVersion(nodeId: String): Int =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt("$KEY_VERSION_PREFIX$nodeId", 0)

fun Context.setCachedPhoneVersion(nodeId: String, versionCode: Int) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt("$KEY_VERSION_PREFIX$nodeId", versionCode)
        .commit()
}
