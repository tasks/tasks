package org.tasks.http

import at.bitfire.dav4jvm.ktor.DomainAuthProvider
import at.bitfire.dav4jvm.ktor.PreemptiveBasicDigestAuthProvider
import at.bitfire.dav4jvm.ktor.UrlUtils
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import org.tasks.caldav.canonicalUrlOrNull

fun HttpClientConfig<*>.installBasicDigestAuth(username: String, password: String, url: String?) {
    install(Auth) {
        providers.add(
            DomainAuthProvider(
                UrlUtils.hostToDomain(url.canonicalUrlOrNull()?.host),
                PreemptiveBasicDigestAuthProvider(username, password),
            )
        )
    }
}
