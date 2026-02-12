package org.tasks.caldav

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TasksAccountResponse(
    val createdAt: Long? = null,
    val subscription: Subscription? = null,
    @SerialName("inbound_email") val inboundEmail: InboundEmail? = null,
    @SerialName("app_passwords") val appPasswords: List<AppPassword> = emptyList(),
) {
    @Serializable
    data class Subscription(
        val free: Boolean = true,
        val provider: String? = null,
        val expiration: Long? = null,
    )

    @Serializable
    data class InboundEmail(
        val email: String? = null,
        val calendar: String? = null,
    )

    @Serializable
    data class AppPassword(
        val description: String? = null,
        @SerialName("session_id") val sessionId: Int = -1,
        @SerialName("created_at") val createdAt: Long? = null,
        @SerialName("last_access") val lastAccess: Long? = null,
    )
}
