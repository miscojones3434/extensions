package com.patriarcatv

import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URLDecoder
import java.net.URLEncoder

class PATRIARCATVProvider : MainAPI() {

    override var name = "PATRIARCATV"
    override var mainUrl = PelisForteAdapter.BASE_URL
    override var lang = "es"

    override val supportedTypes =
        setOf(
            TvType.Movie,
            TvType.Others
        )

    override val hasMainPage = true

    override val mainPage =
        mainPageOf(
            "list|$mainUrl/pelicula|" to "Novedades",
            "list|$mainUrl/pelis/idiomas/castellano|CAST" to "Castellano",
            "list|$mainUrl/pelis/idiomas/espanol-latino|LAT" to "Latino",
            "list|$mainUrl/pelis/idiomas/subtituladas-p02|VOSE" to "VOSE",
            "section|$mainUrl/pelicula|Generos" to "Generos",
            "alphabet|$mainUrl/pelicula|letters" to "Alfabetico",
            "alphabet|$mainUrl/pelicula|years" to "Años"
        )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val parts =
            request.data.split(
                "|",
                limit = 3
            )

        val action =
            parts.getOrNull(0)
                ?: return newHomePageResponse(
                    emptyList()
                )

        val url =
            parts.getOrNull(1)
                ?: return newHomePageResponse(
                    emptyList()
                )

        val extra =
            parts.getOrNull(2)
                .orEmpty()

        return when (action) {

            "list" -> {

                val pageUrl =
                    if (page <= 1) {
                        url
                    } else {
                        "${url.trimEnd('/')}/page/$page"
                    }

                val catalog =
                    PelisForteAdapter.listAll(
                        api = this,
                        url = pageUrl,
                        extra = extra
                    )

                val results =
                    catalog.items.map { item ->
                        PelisForteAdapter.run {
                            item.toSearchResponse(
                                this@PATRIARCATVProvider
                            )
                        }
                    }

                newHomePageResponse(
                    listOf(
                        HomePageList(
                            name = request.name,
                            list = results,
                            isHorizontalImages = true
                        )
                    ),
                    hasNext =
                        catalog.nextUrl != null
                )
            }

            "section" -> {

                val entries =
                    PelisForteAdapter.section(
                        url = url,
                        title = extra
                    )

                val results =
                    entries.map { entry ->
                        navigationResponse(
                            title = entry.title,
                            targetUrl = entry.url,
                            extra = entry.extra
                        )
                    }

                newHomePageResponse(
                    listOf(
                        HomePageList(
                            name = request.name,
                            list = results,
                            isHorizontalImages = false
                        )
                    ),
                    hasNext = false
                )
            }

            "alphabet" -> {

                val entries =
                    PelisForteAdapter.alphabet(
                        url = url,
                        years =
                            extra == "years"
                    )

                val results =
                    entries.map { entry ->
                        navigationResponse(
                            title = entry.title,
                            targetUrl = entry.url,
                            extra = entry.extra
                        )
                    }

                newHomePageResponse(
                    listOf(
                        HomePageList(
                            name = request.name,
                            list = results,
                            isHorizontalImages = false
                        )
                    ),
                    hasNext = false
                )
            }

            else ->
                newHomePageResponse(
                    emptyList()
                )
        }
    }

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        if (query.isBlank()) {
            return emptyList()
        }

        return PelisForteAdapter.search(
            api = this,
            query = query
        )
    }

    override suspend fun load(
        url: String
    ): LoadResponse? {

        if (
            url.startsWith(
                "$mainUrl/__patriarcatv/list?"
            )
        ) {

            val nav =
                decodeNavigation(url)
                    ?: return null

            val catalog =
                PelisForteAdapter.listAll(
                    api = this,
                    url = nav.url,
                    extra = nav.extra
                )

            val results =
                catalog.items.map { item ->
                    PelisForteAdapter.run {
                        item.toSearchResponse(
                            this@PATRIARCATVProvider
                        )
                    }
                }

            return newMovieLoadResponse(
                name = nav.title,
                url = url,
                type = TvType.Others,
                data = url
            ) {
                comingSoon = true
                recommendations = results
            }
        }

        val movie =
            PelisForteAdapter.loadMovie(url)
                ?: return null

        return newMovieLoadResponse(
            name = movie.title,
            url = movie.url,
            type = TvType.Movie,
            data = movie.url
        ) {

            posterUrl =
                movie.posterUrl

            backgroundPosterUrl =
                movie.backdropUrl

            year =
                movie.year

            plot =
                movie.plot

            duration =
                movie.durationMinutes

            tags =
                movie.genres
                    .takeIf {
                        it.isNotEmpty()
                    }

            score =
                Score.from10(
                    movie.rating
                )

            actors =
                movie.cast
                    .mapNotNull { person ->

                        val actorName =
                            person.name
                                .trim()
                                .takeIf {
                                    it.isNotBlank()
                                }
                                ?: return@mapNotNull null

                        val actorImage =
                            person.profilePath
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let { path ->
                                    if (
                                        path.startsWith("http://") ||
                                        path.startsWith("https://")
                                    ) {
                                        path
                                    } else {
                                        "https://image.tmdb.org/t/p/original$path"
                                    }
                                }

                        ActorData(
                            actor = Actor(
                                name = actorName,
                                image = actorImage
                            ),
                            roleString =
                                person.character
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                        )
                    }
                    .takeIf {
                        it.isNotEmpty()
                    }
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

        if (
            data.startsWith(
                "$mainUrl/__patriarcatv/list?"
            )
        ) {
            return false
        }

        val players =
            PelisForteAdapter.findVideos(
                data
            )

        if (players.isEmpty()) {
            return false
        }

        var found = false

        for (player in players) {

            val resolved =
                PelisForteAdapter.play(
                    player
                )
                    ?: continue

            try {
                loadExtractor(
                    resolved.url,
                    resolved.referer,
                    subtitleCallback,
                    callback
                )

                found = true
            } catch (_: Throwable) {
            }
        }

        return found
    }

    private fun navigationResponse(
        title: String,
        targetUrl: String,
        extra: String
    ): SearchResponse {

        return newMovieSearchResponse(
            name = title,
            url =
                navigationUrl(
                    title = title,
                    url = targetUrl,
                    extra = extra
                ),
            type = TvType.Others
        )
    }

    private fun navigationUrl(
        title: String,
        url: String,
        extra: String
    ): String {

        fun encode(
            value: String
        ): String =
            URLEncoder.encode(
                value,
                Charsets.UTF_8.name()
            )

        return "$mainUrl/__patriarcatv/list" +
            "?title=${encode(title)}" +
            "&url=${encode(url)}" +
            "&extra=${encode(extra)}"
    }

    private data class NavigationData(
        val title: String,
        val url: String,
        val extra: String
    )

    private fun decodeNavigation(
        value: String
    ): NavigationData? {

        val query =
            value.substringAfter(
                "?",
                ""
            )

        if (query.isBlank()) {
            return null
        }

        val values =
            query
                .split("&")
                .mapNotNull { part ->

                    val key =
                        part.substringBefore(
                            "=",
                            ""
                        )

                    val rawValue =
                        part.substringAfter(
                            "=",
                            ""
                        )

                    if (key.isBlank()) {
                        null
                    } else {
                        key to URLDecoder.decode(
                            rawValue,
                            Charsets.UTF_8.name()
                        )
                    }
                }
                .toMap()

        val title =
            values["title"]
                ?: return null

        val targetUrl =
            values["url"]
                ?: return null

        return NavigationData(
            title = title,
            url = targetUrl,
            extra =
                values["extra"]
                    .orEmpty()
        )
    }
}
