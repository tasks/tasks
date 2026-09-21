/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.cert4android

import java.security.cert.X509Certificate

interface CertStore {

    /**
     * Removes user (dis-)trust decisions for all certificates.
     */
    fun clearUserDecisions()

    /**
     * Determines whether a certificate chain is trusted.
     */
    fun isTrusted(chain: Array<X509Certificate>, authType: String, trustSystemCerts: Boolean, appInForeground: Boolean?): Boolean

    /**
     * Determines whether a certificate has been explicitly accepted by the user. In this case,
     * we can ignore an invalid host name for that certificate.
     */
    fun isTrustedByUser(cert: X509Certificate): Boolean

    /**
     * Sets this certificate as trusted.
     */
    fun setTrustedByUser(cert: X509Certificate)

    /**
     * Sets this certificate as untrusted.
     */
    fun setUntrustedByUser(cert: X509Certificate)

}