/*
 * M3Play Utility Module
 *
 * Internal helper functions
 * Signature: M3PLAY::UTILITY::V1
 */

package com.j.m3play.ui.utils

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp

fun CornerBasedShape.top(): CornerBasedShape =
    copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
