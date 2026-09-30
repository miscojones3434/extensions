package com.patriarcatv

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class PATRIARCATVProvider : MainAPI() {

    override var name = "PATRIARCATV"

    override var mainUrl =
        "https://www2.pelisforte.se"

    override var lang = "es"

    override val supportedTypes =
        setOf(
            TvType.Movie
        )

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/pelicula" to "PelisForte · Novedades",
        "$mainUrl/pelis/idiomas/castellano" to "PelisForte · Castellano",
        "$mainUrl/pelis/idiomas/espanol-latino" to "PelisForte · Latino",
        "$mainUrl/pelis/idiomas/subtituladas-p02" to "PelisForte · VOSE"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val realPage =
            if (page < 1) 1 else page

        val items =
            when (request.name) {

                "PelisForte · Castellano" ->
                    PelisForteAdapter.getCastellano(
                        this,
                        realPage
                    )

                "PelisForte · Latino" ->
                    PelisForteAdapter.getLatino(
                        this,
                        realPage
                    )

                "PelisForte · VOSE" ->
                    PelisForteAdapter.getVose(
                        this,
                        realPage
                    )

                else ->
                    PelisForteAdapter.getLatest(
                        this,
                        realPage
                    )
            }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name = request.name,
                    list = items,
                    isHorizontalImages = true
                )
            ),
            hasNext = items.isNotEmpty()
        )
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        if (query.isBlank()) {
            return emptyList()
        }

        return PelisForteAdapter.search(
            this,
            query
        )
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        val movie =
            PelisForteAdapter.loadMovie(url)
                ?: return null

        return newMovieLoadResponse(
            movie.title,
            movie.url,
            TvType.Movie,
            movie.url
        ) {
            posterUrl = movie.posterUrl
            year = movie.year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val movie =
            PelisForteAdapter.loadMovie(data)
                ?: return false

        if (movie.players.isEmpty()) {
            return false
        }

        var found = false

        for (player in movie.players) {

            val resolved =
                PelisForteAdapter.resolvePlayer(player)
                    ?: continue

            try {

                loadExtractor(
                    resolved,
                    player,
                    subtitleCallback,
                    callback
                )

                found = true

            } catch (_: Throwable) {
            }
        }

        return found
    }
}
