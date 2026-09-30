package com.patriarcatv

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newMovieSearchResponse

object AlfaCatalogAdapter {

    fun getByCategory(
        api: MainAPI,
        category: AlfaChannelCatalog.Category
    ): List<SearchResponse> {

        return AlfaChannelCatalog.channels
            .filter { channel ->
                channel.category == category
            }
            .map { channel ->
                channel.toSearchResponse(api)
            }
    }

    fun search(
        api: MainAPI,
        query: String
    ): List<SearchResponse> {

        if (query.isBlank()) {
            return emptyList()
        }

        return AlfaChannelCatalog.channels
            .filter { channel ->
                channel.title.contains(
                    query,
                    ignoreCase = true
                ) || channel.id.contains(
                    query,
                    ignoreCase = true
                )
            }
            .map { channel ->
                channel.toSearchResponse(api)
            }
    }

    private fun AlfaChannelCatalog.ChannelDefinition
        .toSearchResponse(api: MainAPI): SearchResponse {

        return api.newMovieSearchResponse(
            name = title,
            url = buildChannelUrl(id),
            type = TvType.Others
        )
    }

    fun buildChannelUrl(
        channelId: String
    ): String {
        return "patriarcatv://alfa/$channelId"
    }

    fun extractChannelId(
        url: String
    ): String? {

        val prefix = "patriarcatv://alfa/"

        if (!url.startsWith(prefix)) {
            return null
        }

        return url
            .removePrefix(prefix)
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
