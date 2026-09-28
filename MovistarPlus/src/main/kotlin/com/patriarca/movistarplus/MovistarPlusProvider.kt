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

class MovistarPlusProvider : MainAPI() {

    override var mainUrl = MovistarApi.WEB_URL
    override var name = "Movistar Plus+"
    override var lang = "es"

    override val supportedTypes = setOf(
        TvType.Live,
        TvType.Movie,
        TvType.TvSeries,
        TvType.Documentary
    )

    override val hasMainPage = true

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        if (!MovistarSessionManager.isInitialized) {
            return newHomePageResponse(
                emptyList(),
                hasNext = false
            )
        }

        val channels = MovistarChannelsClient.getChannels()

        val liveItems = channels.map { channel ->
            channel.toSearchResponse()
        }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name = "En directo",
                    list = liveItems,
                    isHorizontalImages = false
                )
            ),
            hasNext = false
        )
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        if (!MovistarSessionManager.isInitialized) {
            return emptyList()
        }

        return MovistarChannelsClient
            .getChannels()
            .filter {
                it.name.contains(
                    query,
                    ignoreCase = true
                )
            }
            .map {
                it.toSearchResponse()
            }
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val data =
            tryParseJson<MovistarPlaybackData>(url)
                ?: return null

        val channel =
            MovistarChannelsClient
                .getChannels()
                .firstOrNull {
                    it.id == data.contentId
                }

        val title = channel?.let {
            if (it.dial.isNotBlank()) {
                "${it.dial}. ${it.name}"
            } else {
                it.name
            }
        } ?: data.contentId

        return newLiveStreamLoadResponse(
            title,
            url,
            url
        ) {
            posterUrl = channel?.logoUrl

            tags = listOfNotNull(
                "Movistar Plus+",
                channel?.dial
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "Dial $it" }
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return false
    }

    private fun MovistarChannelsClient.Channel
        .toSearchResponse(): LiveSearchResponse {

        val playbackData = MovistarPlaybackData(
            contentId = id,
            streamType = "CHN",
            url = playbackUrl,
            drmMediaId = casId
        )

        val title =
            if (dial.isNotBlank()) {
                "$dial. $name"
            } else {
                name
            }

        return newLiveSearchResponse(
            title,
            playbackData.toJson(),
            TvType.Live,
            fix = false
        ) {
            posterUrl = logoUrl
        }
    }
}
