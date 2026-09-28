package com.patriarca.movistarplus

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

object MovistarChannelsClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class RawChannel(
        @JsonProperty("CodCadenaTv")
        val id: String? = null,

        @JsonProperty("Nombre")
        val name: String? = null,

        @JsonProperty("Dial")
        val dial: String? = null,

        @JsonProperty("PuntoReproduccion")
        val playbackUrl: String? = null,

        @JsonProperty("CasId")
        val casId: String? = null,

        @JsonProperty("Uid")
        val uid: String? = null,

        @JsonProperty("Logo")
        val logo: String? = null,

        @JsonProperty("Logos")
        val logos: List<MovistarImage> = emptyList(),

        @JsonProperty("Imagenes")
        val images: List<MovistarImage> = emptyList(),

        @JsonProperty("tvProducts")
        val products: List<String> = emptyList()
    )

    data class Channel(
        val id: String,
        val name: String,
        val dial: String,
        val playbackUrl: String,
        val logoUrl: String?,
        val casId: String?,
        val uid: String?,
        val products: List<String>,
        val subscribed: Boolean,
        val sessionRequest: String
    )

    suspend fun getChannels(): List<Channel> {
        if (!MovistarSessionManager.isInitialized) {
            return emptyList()
        }

        if (MovistarSessionManager.platform.isBlank()) {
            return emptyList()
        }

        val url = MovistarApi.channels(
            profile = MovistarSessionManager.platform,
            demarcation = MovistarSessionManager.demarcation
        )

        val response = app.get(
            url,
            headers = MovistarSessionManager.playbackHeaders()
        )

        val channels =
            tryParseJson<List<RawChannel>>(response.text)
                ?: return emptyList()

        return channels.mapNotNull { raw ->
            val id = raw.id
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val name = raw.name
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val playbackUrl = raw.playbackUrl
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val logoUrl =
                raw.logo
                    ?.takeIf { it.isNotBlank() }
                    ?: raw.logos
                        .firstOrNull()
                        ?.uri
                        ?.takeIf { it.isNotBlank() }
                    ?: raw.images
                        .firstOrNull()
                        ?.uri
                        ?.takeIf { it.isNotBlank() }

            Channel(
                id = id,
                name = name,
                dial = raw.dial.orEmpty(),
                playbackUrl = playbackUrl,
                logoUrl = logoUrl,
                casId = raw.casId,
                uid = raw.uid,
                products = raw.products,
                subscribed = isSubscribed(raw.products),
                sessionRequest =
                    """{"contentID":"$id","streamType":"CHN"}"""
            )
        }.sortedWith(
            compareBy<Channel> {
                it.dial.toIntOrNull() ?: Int.MAX_VALUE
            }.thenBy {
                it.name
            }
        )
    }

    private fun isSubscribed(
        products: List<String>
    ): Boolean {
        if (products.isEmpty()) {
            return true
        }

        val subscriptions =
            MovistarSessionManager.linearSubscription

        if (subscriptions.isEmpty()) {
            return false
        }

        return products.any { product ->
            subscriptions.any { subscription ->
                subscription.equals(
                    product,
                    ignoreCase = true
                )
            }
        }
    }
}
