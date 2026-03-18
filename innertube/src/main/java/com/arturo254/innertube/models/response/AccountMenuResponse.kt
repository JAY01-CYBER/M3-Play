package com.arturo254.innertube.models.response

import com.arturo254.innertube.models.AccountInfo
import com.arturo254.innertube.models.Runs
import kotlinx.serialization.Serializable
import java.text.Normalizer

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
                        ) {
                            fun toAccountInfo(): AccountInfo {
                                val sanitizedName = accountName.runs!!.first().text
                                    .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
                                    .replace(Regex("[\\p{M}]"), "")

                                return AccountInfo(
                                    name = sanitizedName,
                                    email = email?.runs?.first()?.text,
                                    channelHandle = channelHandle?.runs?.first()?.text,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}