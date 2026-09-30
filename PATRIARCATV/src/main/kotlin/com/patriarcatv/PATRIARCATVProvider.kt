package com.patriarcatv

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class PATRIARCATVProvider : MainAPI() {

    override var name =
        "PATRIARCATV"

    override var mainUrl =
        "https://www2.pelisforte.se"

    override var lang =
        "es"

    override val supportedTypes =
        setOf(
            TvType.Movie
        )

    override val hasMainPage =
        true

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val realPage =
            if (page < 1) 1 else page

        val sections =
            mutableListOf<HomePageList>()

        val latest =
            PelisForteAdapter.getLatest(
                this,
                realPage
            )

        if (latest.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · Novedades",
                    list = latest,
                    isHorizontalImages = true
                )
            )
        }

        val castellano =
            PelisForteAdapter.getCastellano(
                this,
                realPage
            )

        if (castellano.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · Castellano",
                    list = castellano,
                    isHorizontalImages = true
                )
            )
        }

        val latino =
            PelisForteAdapter.getLatino(
                this,
                realPage
            )

        if (latino.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · Latino",
                    list = latino,
                    isHorizontalImages = true
                )
            )
        }

        val vose =
            PelisForteAdapter.getVose(
                this,
                realPage
            )

        if (vose.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · VOSE",
                    list = vose,
                    isHorizontalImages = true
                )
            )
        }

        return newHomePageResponse(
            sections,
            hasNext = sections.isNotEmpty()
        )
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

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

            posterUrl =
                movie.posterUrl

            year =
                movie.year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback:
            (SubtitleFile) -> Unit,
        callback:
            (ExtractorLink) -> Unit
    ): Boolean {

        val movie =
            PelisForteAdapter.loadMovie(data)
                ?: return false

        if (movie.players.isEmpty()) {
            return false
        }

        var found =
            false

        for (player in movie.players) {

            val resolved =
                PelisForteAdapter
                    .resolvePlayer(player)
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
