package com.patriarcatv

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newMovieSearchResponse
import java.net.URLEncoder

object PelisForteAdapter {

    private const val BASE_URL = "https://www2.pelisforte.se"

    data class MovieItem(
        val title: String,
        val url: String,
        val posterUrl: String?,
        val year: Int?
    )

    data class MovieDetails(
        val title: String,
        val url: String,
        val posterUrl: String?,
        val year: Int?,
        val players: List<String>
    )

    suspend fun getLatest(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> =
        getMovies(pageUrl("$BASE_URL/pelicula", page))
            .map { it.toSearchResponse(api) }

    suspend fun getCastellano(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> =
        getMovies(
            pageUrl(
                "$BASE_URL/pelis/idiomas/castellano",
                page
            )
        ).map { it.toSearchResponse(api) }

    suspend fun getLatino(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> =
        getMovies(
            pageUrl(
                "$BASE_URL/pelis/idiomas/espanol-latino",
                page
            )
        ).map { it.toSearchResponse(api) }

    suspend fun getVose(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> =
        getMovies(
            pageUrl(
                "$BASE_URL/pelis/idiomas/subtituladas-p02",
                page
            )
        ).map { it.toSearchResponse(api) }

    suspend fun search(
        api: MainAPI,
        query: String
    ): List<SearchResponse> {

        if (query.isBlank()) {
            return emptyList()
        }

        val encoded = URLEncoder.encode(
            query.trim(),
            Charsets.UTF_8.name()
        )

        return getMovies(
            "$BASE_URL/page/1?s=$encoded"
        ).map {
            it.toSearchResponse(api)
        }
    }

    suspend fun loadMovie(
        url: String
    ): MovieDetails? {

        if (!url.startsWith(BASE_URL)) {
            return null
        }

        val document = try {
            app.get(url).document
        } catch (_: Throwable) {
            return null
        }

        val title =
            document.selectFirst("h1")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: document
                    .selectFirst("meta[property=og:title]")
                    ?.attr("content")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: return null

        val poster =
            document
                .selectFirst("meta[property=og:image]")
                ?.attr("content")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val year =
            document
                .selectFirst(".year")
                ?.text()
                ?.trim()
                ?.toIntOrNull()
                ?: Regex("""\b(?:19|20)\d{2}\b""")
                    .find(document.text())
                    ?.value
                    ?.toIntOrNull()

        /*
         * Igual que findvideos() del canal oficial de Alfa:
         *
         * soup = create_soup(item.url).find('section', class_='player')
         * matches = soup.find_all("iframe")
         * url = elem['data-src']
         * url = url.replace("?h=", "r.php?h=")
         */

        val players =
            document
                .selectFirst("section.player")
                ?.select("iframe")
                ?.mapNotNull { iframe ->

                    val value =
                        iframe.attr("data-src")
                            .ifBlank {
                                iframe.attr("src")
                            }
                            .trim()

                    if (value.isBlank()) {
                        null
                    } else {
                        normalizeUrl(
                            value.replace(
                                "?h=",
                                "r.php?h="
                            )
                        )
                    }
                }
                ?.distinct()
                ?: emptyList()

        return MovieDetails(
            title = title,
            url = url,
            posterUrl = poster?.let(::normalizeUrl),
            year = year,
            players = players
        )
    }

    suspend fun resolvePlayer(
        playerUrl: String
    ): String? {

        /*
         * Igual que play() de Alfa:
         *
         * url = httptools.downloadpage(item.url).url
         */

        return try {
            app.get(
                playerUrl,
                allowRedirects = true
            ).url
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun getMovies(
        url: String
    ): List<MovieItem> {

        val document = try {
            app.get(url).document
        } catch (_: Throwable) {
            return emptyList()
        }

        val container =
            document.selectFirst("ul.post-lst")
                ?: return emptyList()

        return container
            .select("li")
            .filter { element ->
                element.classNames().any {
                    it.matches(
                        Regex("""post-\d+""")
                    )
                }
            }
            .mapNotNull { element ->

                val linkElement =
                    element.selectFirst("a[href]")
                        ?: return@mapNotNull null

                val title =
                    element.selectFirst("h2")
                        ?.text()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val link =
                    linkElement
                        .attr("href")
                        .trim()
                        .takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val poster =
                    element.selectFirst("img")
                        ?.attr("src")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }

                val year =
                    element
                        .selectFirst("span.year")
                        ?.text()
                        ?.trim()
                        ?.toIntOrNull()

                MovieItem(
                    title = title,
                    url = normalizeUrl(link),
                    posterUrl =
                        poster?.let(::normalizeUrl),
                    year = year
                )
            }
            .distinctBy {
                it.url
            }
    }

    private fun MovieItem.toSearchResponse(
        api: MainAPI
    ): SearchResponse {

        return api.newMovieSearchResponse(
            name = title,
            url = url,
            type = TvType.Movie
        ) {
            posterUrl =
                this@toSearchResponse.posterUrl

            year =
                this@toSearchResponse.year
        }
    }

    private fun pageUrl(
        base: String,
        page: Int
    ): String {

        if (page <= 1) {
            return base
        }

        return "${base.trimEnd('/')}/page/$page"
    }

    private fun normalizeUrl(
        value: String
    ): String {

        return when {

            value.startsWith("//") ->
                "https:$value"

            value.startsWith("/") ->
                "$BASE_URL$value"

            else ->
                value
        }
    }
}
