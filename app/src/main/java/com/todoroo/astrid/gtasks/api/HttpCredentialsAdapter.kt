/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.todoroo.astrid.gtasks.api

import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import org.tasks.gtasks.GoogleAccountManager
import java.io.IOException
import java.net.URI
import java.util.*
import javax.inject.Inject

class HttpCredentialsAdapter @Inject constructor(private val googleAccountManager: GoogleAccountManager) : HttpRequestInitializer {

    private var credentials: GoogleCredentials? = null

    @Throws(IOException::class)
    override fun initialize(request: HttpRequest) {
        if (credentials == null || !credentials!!.hasRequestMetadata()) {
            return
        }
        val requestHeaders = request.headers
        var uri: URI? = null
        if (request.url != null) {
            uri = request.url.toURI()
        }
        val credentialHeaders = credentials!!.getRequestMetadata(uri) ?: return
        for ((headerName, value) in credentialHeaders) {
            val requestValues: MutableList<String> = ArrayList()
            requestValues.addAll(value!!)
            requestHeaders[headerName] = requestValues
        }
    }

    suspend fun checkToken(account: String?, scope: String) {
        if (credentials == null) {
            val token = googleAccountManager.getAccessToken(account, scope)
            credentials = GoogleCredentials(AccessToken(token, null))
        }
    }

    fun invalidateToken() {
        googleAccountManager.invalidateToken(credentials?.accessToken?.tokenValue)
        credentials = null
    }
}