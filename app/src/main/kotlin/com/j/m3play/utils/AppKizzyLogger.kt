/*
 * M3Play — Music, thoughtfully crafted.
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This file is part of M3Play. See the repository LICENSE for terms.
 * Existing copyright and attribution notices are preserved below.
 */

/*
 * M3Play Utility Module
 *
 * Internal helper functions
 * Signature: M3PLAY::UTILITY::V1
 */

package com.j.m3play.utils

import com.my.kizzy.KizzyLogger
import timber.log.Timber

/**
 * Timber-backed implementation of KizzyLogger for the Android app.
 */
class AppKizzyLogger(private val tag: String = "Kizzy") : KizzyLogger {
    override fun info(message: String) {
        Timber.tag(tag).i(message)
    }

    override fun fine(message: String) {
        Timber.tag(tag).v(message)
    }

    override fun warning(message: String) {
        Timber.tag(tag).w(message)
    }

    override fun severe(message: String) {
        Timber.tag(tag).e(message)
    }
}
