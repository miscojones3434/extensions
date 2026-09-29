package com.patriarca.movistarplus

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LiveSearchResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newLiveSearchResponse
import com.lagradost.cloudstream3.newLiveStreamLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

class MovistarPlusProvider : MainAPI() {

    override var mainUrl = "http://192.168.1.149"
    override var name = "Movistar Plus+"
    override var lang = "es"

    override val supportedTypes = setOf(
        TvType.Live
    )

    override val hasMainPage = true

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val channels =
            VuPlusClient.getMovistarChannels()

        val items =
            channels.map { channel ->
                channel.toSearchResponse()
            }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name = "Movistar Plus+",
                    list = items,
                    isHorizontalImages = false
                )
            ),
            hasNext = false
        )
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        return VuPlusClient
            .getMovistarChannels()
            .filter { channel ->
                channel.name.contains(
                    query,
                    ignoreCase = true
                )
            }
            .map { channel ->
                channel.toSearchResponse()
            }
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val channel =
            tryParseJson<VuPlusClient.Channel>(
                url
            ) ?: return null

        return newLiveStreamLoadResponse(
            channel.name,
            url,
            url
        ) {
            tags = listOf(
                "Movistar Plus+",
                "Vu+",
                "En directo"
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val channel =
            tryParseJson<VuPlusClient.Channel>(
                data
            ) ?: return false

        if (channel.streamUrl.isBlank()) {
            return false
        }

        callback(
            newExtractorLink(
                source = "Vu+",
                name = channel.name,
                url = channel.streamUrl,
                type = ExtractorLinkType.VIDEO
            ) {
                quality = Qualities.Unknown.value
            }
        )

        return true
    }

    private fun VuPlusClient.Channel
        .toSearchResponse(): LiveSearchResponse {

        return newLiveSearchResponse(
            name,
            toJson(),
            TvType.Live,
            fix = false
        )
    }
}
