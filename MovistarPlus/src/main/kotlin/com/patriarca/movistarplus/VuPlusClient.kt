package com.patriarca.movistarplus

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import java.net.URLEncoder

object VuPlusClient {

    private const val OPENWEBIF_BASE_URL =
        "http://192.168.1.149"

    private const val STREAM_BASE_URL =
        "http://192.168.1.149:8001"

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ServicesResponse(
        @JsonProperty("result")
        val result: Boolean? = null,

        @JsonProperty("services")
        val services: List<ServiceItem> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ServiceItem(
        @JsonProperty("servicename")
        val serviceName: String? = null,

        @JsonProperty("servicereference")
        val serviceReference: String? = null
    )

    data class Channel(
        val name: String,
        val serviceReference: String,
        val streamUrl: String
    )

    suspend fun getMovistarChannels(): List<Channel> {
        val bouquets = getServices(null)

        val movistarBouquet = bouquets.firstOrNull { bouquet ->
            bouquet.serviceName
                .orEmpty()
                .contains(
                    "movistar",
                    ignoreCase = true
                )
        } ?: return emptyList()

        val bouquetReference =
            movistarBouquet.serviceReference
                ?.takeIf { it.isNotBlank() }
                ?: return emptyList()

        return getServices(bouquetReference)
            .mapNotNull { service ->

                val name =
                    service.serviceName
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val reference =
                    service.serviceReference
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                if (reference.contains("FROM BOUQUET", ignoreCase = true)) {
                    return@mapNotNull null
                }

                Channel(
                    name = name,
                    serviceReference = reference,
                    streamUrl = buildStreamUrl(reference)
                )
            }
    }

    private suspend fun getServices(
        serviceReference: String?
    ): List<ServiceItem> {

        val url =
            if (serviceReference.isNullOrBlank()) {
                "$OPENWEBIF_BASE_URL/api/getservices"
            } else {
                "$OPENWEBIF_BASE_URL/api/getservices?sRef=${
                    encode(serviceReference)
                }"
            }

        val response = app.get(url)

        if (response.code !in 200..299) {
            return emptyList()
        }

        val parsed =
            tryParseJson<ServicesResponse>(
                response.text
            ) ?: return emptyList()

        if (parsed.result == false) {
            return emptyList()
        }

        return parsed.services
    }

    private fun buildStreamUrl(
        serviceReference: String
    ): String {
        return "$STREAM_BASE_URL/${
            encodeStreamReference(serviceReference)
        }"
    }

    private fun encode(
        value: String
    ): String {
        return URLEncoder
            .encode(
                value,
                Charsets.UTF_8.name()
            )
            .replace("+", "%20")
    }

    private fun encodeStreamReference(
        value: String
    ): String {
        return value
            .trim()
            .replace("%", "%25")
            .replace(" ", "%20")
    }
}
