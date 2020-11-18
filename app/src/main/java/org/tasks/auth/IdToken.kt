package org.tasks.auth

import android.util.Base64
import org.json.JSONObject

class IdToken(idToken: String) {
    private val parts: List<String> = idToken.split(".")
    val json = JSONObject(String(Base64.decode(parts[1], Base64.DEFAULT)))

    val email: String
        get() = json.getString("email")

    val sub: String
        get() = json.getString("sub")
}