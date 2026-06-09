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

/**
 * Provides settings for cert4android. Implementations can override the getters.
 *
 * Usually implemented by the app which uses cert4android, and then passed to cert4android classes
 * which need it.
 */
interface SettingsProvider {

    /**
     * The app foreground status:
     *
     * - `true`: foreground – directly launch UI ([TrustCertificateActivity]) and show notification (if possible)
     * - `false`: background – only show notification (if possible)
     * - `null`: non-interactive mode – don't show notification or launch activity
     */
    val appInForeground: Boolean?

    /**
     * Whether system certificates shall be trusted.
     *
     * @return `true` if system certificates are considered trustworthy, `false` otherwise
     */
    val trustSystemCerts: Boolean

}