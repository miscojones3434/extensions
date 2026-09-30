package com.patriarcatv

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newMovieSearchResponse
import java.net.URLEncoder

object PelisForteAdapter {

    private const val BASE_URL = "https://www2.pelisforte.se"

    private val headers = mapOf(
        "User-Agent" to
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36",
        "Accept" to
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "es-ES,es;q=0.9,en;q=0.8"
    )

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
        val plot: String?,
        val players: List<String>
    )

    suspend fun getLatest(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> {
        return getMovies(
            pageUrl("$BASE_URL/pelicula", page)
        ).map {
            it.toSearchResponse(api)
        }
    }

    suspend fun getCastellano(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> {
        return getMovies(
            pageUrl(
                "$BASE_URL/pelis/idiomas/castellano",
                page
            )
        ).map {
            it.toSearchResponse(api)
        }
    }

    suspend fun getLatino(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> {
        return getMovies(
            pageUrl(
                "$BASE_URL/pelis/idiomas/espanol-latino",
                page
            )
        ).map {
            it.toSearchResponse(api)
        }
    }

    suspend fun getVose(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> {
        return getMovies(
            pageUrl(
                "$BASE_URL/pelis/idiomas/subtituladas-p02",
                page
            )
        ).map {
            it.toSearchResponse(api)
        }
    }

    suspend fun search(
        api: MainAPI,
        query: String
    ): List<SearchResponse> {

        if (query.isBlank()) {
            return emptyList()
        }

        val encoded =
            URLEncoder.encode(
                query.trim(),
                Charsets.UTF_8.name()
            )

        return getMovies(
            "$BASE_URL/page/1?s=$encoded"
        ).map {
            it.toSearchResponse(api)
        }
    }

    private suspend fun getMovies(
        url: String
    ): List<MovieItem> {

        val response = try {
            app.get(
                url,
                headers = headers
            )
        } catch (_: Throwable) {
            return emptyList()
        }

        if (response.code !in 200..299) {
            return emptyList()
        }

        val document = response.document

        /*
         * Código Alfa original:
         *
         * soup.find('ul', class_='post-lst')
         *     .find_all("li", class_=re.compile(r"^post-\d+"))
         *
         * Aquí hacemos exactamente esa estructura,
         * tolerando clases adicionales.
         */
        val container =
            document.selectFirst("ul.post-lst")
                ?: return emptyList()

        val elements =
            container.children().filter { element ->

                element.tagName() == "li" &&
                    element.classNames().any { className ->
                        className.matches(
                            Regex("""post-\d+""")
                        )
                    }
            }

        return elements
            .mapNotNull { element ->

                val anchor =
                    element.selectFirst("a[href]")
                        ?: return@mapNotNull null

                val pageUrl =
                    anchor.attr("href")
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                val title =
                    element.selectFirst("h2")
                        ?.text()
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                val image =
                    element.selectFirst("img")

                val poster =
                    image
                        ?.attr("data-src")
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: image
                            ?.attr("data-lazy-src")
                            ?.takeIf {
                                it.isNotBlank()
                            }
                        ?: image
                            ?.attr("src")
                            ?.takeIf {
                                it.isNotBlank()
                            }

                val year =
                    element.selectFirst("span.year")
                        ?.text()
                        ?.trim()
                        ?.filter {
                            it.isDigit()
                        }
                        ?.takeIf {
                            it.length == 4
                        }
                        ?.toIntOrNull()

                MovieItem(
                    title = title,
                    url = normalizeUrl(pageUrl),
                    posterUrl =
                        poster?.let(::normalizeUrl),
                    year = year
                )
            }
            .distinctBy {
                it.url
            }
    }

    suspend fun loadMovie(
        url: String
    ): MovieDetails? {

        val response = try {
            app.get(
                url,
                headers = headers
            )
        } catch (_: Throwable) {
            return null
        }

        if (response.code !in 200..299) {
            return null
        }

        val document =
            response.document

        val title =
            document.selectFirst("h1")
                ?.text()
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: document
                    .selectFirst(
                        "meta[property=og:title]"
                    )
                    ?.attr("content")
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                ?: return null

        val poster =
            document
                .selectFirst(
                    "meta[property=og:image]"
                )
                ?.attr("content")
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val plot =
            document
                .selectFirst(
                    "meta[name=description]"
                )
                ?.attr("content")
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val year =
            document.selectFirst(".year")
                ?.text()
                ?.filter {
                    it.isDigit()
                }
                ?.takeIf {
                    it.length == 4
                }
                ?.toIntOrNull()
                ?: Regex(
                    """\b(?:19|20)\d{2}\b"""
                )
                    .find(document.text())
                    ?.value
                    ?.toIntOrNull()

        /*
         * Alfa:
         *
         * soup.find('section', class_='player')
         * iframe['data-src']
         * replace("?h=", "r.php?h=")
         */
        val players =
            document
                .selectFirst("section.player")
                ?.select("iframe")
                ?.mapNotNull { iframe ->

                    val raw =
                        iframe.attr("data-src")
                            .ifBlank {
                                iframe.attr("src")
                            }
                            .trim()

                    raw
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?.replace(
                            "?h=",
                            "r.php?h="
                        )
                        ?.let(::normalizeUrl)
                }
                ?.distinct()
                ?: emptyList()

        return MovieDetails(
            title = title,
            url = url,
            posterUrl =
                poster?.let(::normalizeUrl),
            year = year,
            plot = plot,
            players = players
        )
    }

    suspend fun resolvePlayer(
        playerUrl: String
    ): String? {

        return try {
            app.get(
                playerUrl,
                headers = headers,
                allowRedirects = true
            ).url
        } catch (_: Throwable) {
            null
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

        return if (page <= 1) {
            base
        } else {
            "${base.trimEnd('/')}/page/$page"
        }
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
