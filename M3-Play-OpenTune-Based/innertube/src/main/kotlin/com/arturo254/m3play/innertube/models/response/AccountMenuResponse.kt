/*
 * M3 Play Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.j.m3play.innertube.models.response

import com.j.m3play.innertube.models.AccountInfo
import com.j.m3play.innertube.models.Runs
import com.j.m3play.innertube.models.Thumbnails
import com.j.m3play.innertube.models.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class AccountMenuResponse(
    val actions: List<Action>,
) {
    @Serializable
    data class Action(
        val openPopupAction: OpenPopupAction,
    ) {
        @Serializable
        data class OpenPopupAction(
            val popup: Popup,
        ) {
            @Serializable
            data class Popup(
                val multiPageMenuRenderer: MultiPageMenuRenderer,
            ) {
                @Serializable
                data class MultiPageMenuRenderer(
                    val header: Header?,
                ) {
                    @Serializable
                    data class Header(
                        val activeAccountHeaderRenderer: ActiveAccountHeaderRenderer,
                    ) {
                        @Serializable
                        data class ActiveAccountHeaderRenderer(
                            val accountName: Runs,
                            val email: Runs?,
                            val channelHandle: Runs?,
                            val accountPhoto: Thumbnails,
                        ) {
                            fun toAccountInfo(): AccountInfo? {
                                val name = accountName.runs?.firstOrNull()?.text ?: return null
                                return AccountInfo(
                                    name = name,
                                    email = email?.runs?.firstOrNull()?.text,
                                    channelHandle = channelHandle?.runs?.firstOrNull()?.text,
                                    thumbnailUrl = accountPhoto.thumbnails.lastOrNull()?.url,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
