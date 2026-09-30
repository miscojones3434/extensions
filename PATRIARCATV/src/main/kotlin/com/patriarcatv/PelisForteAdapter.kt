package com.patriarcatv

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newMovieSearchResponse
import java.net.URLEncoder

object PelisForteAdapter {

    private const val BASE_URL =
        "https://www2.pelisforte.se"

    data class MovieItem(
        val title: String,
        val url: String,
        val posterUrl: String?,
        val year: Int?
    )

    suspend fun getLatest(
        api: MainAPI
    ): List<SearchResponse> {
        return getMovies(
            "$BASE_URL/pelicula"
        ).map { movie ->
            movie.toSearchResponse(api)
        }
    }

    suspend fun getCastellano(
        api: MainAPI
    ): List<SearchResponse> {
        return getMovies(
            "$BASE_URL/pelis/idiomas/castellano"
        ).map { movie ->
            movie.toSearchResponse(api)
        }
    }

    suspend fun getLatino(
        api: MainAPI
    ): List<SearchResponse> {
        return getMovies(
            "$BASE_URL/pelis/idiomas/espanol-latino"
        ).map { movie ->
            movie.toSearchResponse(api)
        }
    }

    suspend fun getVose(
        api: MainAPI
    ): List<SearchResponse> {
        return getMovies(
            "$BASE_URL/pelis/idiomas/subtituladas-p02"
        ).map { movie ->
            movie.toSearchResponse(api)
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
        ).map { movie ->
            movie.toSearchResponse(api)
        }
    }

    private suspend fun getMovies(
        url: String
    ): List<MovieItem> {

        val response =
            try {
                app.get(url)
            } catch (_: Throwable) {
                return emptyList()
            }

        if (response.code !in 200..299) {
            return emptyList()
        }

        val html = response.text

        val itemRegex = Regex(
            """<li[^>]+class=["'][^"']*post-\d+[^"']*["'][^>]*>(.*?)</li>""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        )

        return itemRegex
            .findAll(html)
            .mapNotNull { match ->

                val block =
                    match.groupValues[1]

                val pageUrl =
                    Regex(
                        """<a[^>]+href=["']([^"']+)["']""",
                        RegexOption.IGNORE_CASE
                    )
                        .find(block)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val title =
                    Regex(
                        """<h2[^>]*>(.*?)</h2>""",
                        setOf(
                            RegexOption.IGNORE_CASE,
                            RegexOption.DOT_MATCHES_ALL
                        )
                    )
                        .find(block)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.let(::cleanText)
                        ?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val poster =
                    Regex(
                        """<img[^>]+(?:src|data-src)=["']([^"']+)["']""",
                        RegexOption.IGNORE_CASE
                    )
                        .find(block)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.trim()

                val year =
                    Regex(
                        """<span[^>]+class=["'][^"']*year[^"']*["'][^>]*>(.*?)</span>""",
                        setOf(
                            RegexOption.IGNORE_CASE,
                            RegexOption.DOT_MATCHES_ALL
                        )
                    )
                        .find(block)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.let(::cleanText)
                        ?.filter { it.isDigit() }
                        ?.takeIf { it.length == 4 }
                        ?.toIntOrNull()

                MovieItem(
                    title = title,
                    url = normalizeUrl(pageUrl),
                    posterUrl = poster?.let(::normalizeUrl),
                    year = year
                )
            }
            .distinctBy { it.url }
            .toList()
    }

    private fun MovieItem.toSearchResponse(
        api: MainAPI
    ): SearchResponse {
        return api.newMovieSearchResponse(
            name = title,
            url = url,
            type = TvType.Movie
        ) {
            posterUrl = this@toSearchResponse.posterUrl
            year = this@toSearchResponse.year
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

    private fun cleanText(
        value: String
    ): String {
        return value
            .replace(
                Regex("<[^>]+>"),
                ""
            )
            .replace("&amp;", "&")
            .replace("&#8217;", "'")
            .replace("&#039;", "'")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
            .trim()
    }
}
