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

    override var name = "PATRIARCATV"

    override var mainUrl =
        "https://www2.pelisforte.se"

    override var lang = "es"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.Cartoon,
        TvType.Documentary,
        TvType.Others
    )

    override val hasMainPage = true

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val sections = mutableListOf<HomePageList>()

        val pelisForteLatest =
            PelisForteAdapter.getLatest(this)

        if (pelisForteLatest.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · Novedades",
                    list = pelisForteLatest,
                    isHorizontalImages = true
                )
            )
        }

        val pelisForteCastellano =
            PelisForteAdapter.getCastellano(this)

        if (pelisForteCastellano.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · Castellano",
                    list = pelisForteCastellano,
                    isHorizontalImages = true
                )
            )
        }

        val pelisForteLatino =
            PelisForteAdapter.getLatino(this)

        if (pelisForteLatino.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · Latino",
                    list = pelisForteLatino,
                    isHorizontalImages = true
                )
            )
        }

        val pelisForteVose =
            PelisForteAdapter.getVose(this)

        if (pelisForteVose.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = "PelisForte · VOSE",
                    list = pelisForteVose,
                    isHorizontalImages = true
                )
            )
        }

        addCatalogSection(
            sections = sections,
            title = "Fuentes de Películas",
            category = AlfaChannelCatalog.Category.MOVIES
        )

        addCatalogSection(
            sections = sections,
            title = "Fuentes de Series",
            category = AlfaChannelCatalog.Category.SERIES
        )

        addCatalogSection(
            sections = sections,
            title = "Fuentes de Anime",
            category = AlfaChannelCatalog.Category.ANIME
        )

        addCatalogSection(
            sections = sections,
            title = "Fuentes de Documentales",
            category = AlfaChannelCatalog.Category.DOCUMENTARIES
        )

        addCatalogSection(
            sections = sections,
            title = "Fuentes Asiáticas",
            category = AlfaChannelCatalog.Category.ASIAN
        )

        addCatalogSection(
            sections = sections,
            title = "Fuentes de Dibujos",
            category = AlfaChannelCatalog.Category.CARTOONS
        )

        return newHomePageResponse(
            sections,
            hasNext = false
        )
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        val results =
            mutableListOf<SearchResponse>()

        results.addAll(
            PelisForteAdapter.search(this, query)
        )

        results.addAll(
            AlfaCatalogAdapter.search(this, query)
        )

        return results
            .distinctBy {
                it.url
            }
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        if (url.startsWith("patriarcatv://alfa/")) {
            return null
        }

        val movie =
            PelisForteAdapter.loadMovie(url)
                ?: return null

        return newMovieLoadResponse(
            name = movie.title,
            url = movie.url,
            type = TvType.Movie,
            dataUrl = movie.url
        ) {
            posterUrl = movie.posterUrl
            year = movie.year
            plot = movie.plot
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

        var loaded = false

        movie.playerUrls.forEach { playerUrl ->

            try {
                loadExtractor(
                    playerUrl,
                    subtitleCallback,
                    callback
                )

                loaded = true
            } catch (_: Throwable) {
            }
        }

        return loaded
    }

    private fun addCatalogSection(
        sections: MutableList<HomePageList>,
        title: String,
        category: AlfaChannelCatalog.Category
    ) {

        val items =
            AlfaCatalogAdapter.getByCategory(
                this,
                category
            )

        if (items.isNotEmpty()) {
            sections.add(
                HomePageList(
                    name = title,
                    list = items,
                    isHorizontalImages = false
                )
            )
        }
    }
}
