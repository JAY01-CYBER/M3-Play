/*
 * M3Play Project (2026)
 * Kòi Natsuko (github.com/JAY01-CYBER)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.my.kizzy.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    @SerialName("id")
    val id: String,
)