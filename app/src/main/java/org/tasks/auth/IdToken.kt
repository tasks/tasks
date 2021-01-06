package org.tasks.auth

import android.util.Base64
import org.json.JSONObject

class IdToken(idToken: String) {
    private val parts: List<String> = idToken.split(".")
    private val json = JSONObject(String(Base64.decode(parts[1], Base64.URL_SAFE)))

    val email: String?
        get() = json.optString("email").takeIf { it.isNotBlank() }

    val sub: String
        get() = json.getString("sub")

    val login: String?
        get() = json.optString("login").takeIf { it.isNotBlank() }
}