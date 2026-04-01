/*
 * M3 Play Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.j.m3play.ui.utils

import androidx.compose.runtime.mutableStateOf

class ItemWrapper<T>(
    val item: T,
) {
    private val _isSelected = mutableStateOf(true)

    var isSelected: Boolean
        get() = _isSelected.value
        set(value) {
            _isSelected.value = value
        }
}
