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
        val plot: String?,
        val playerUrls: List<String>
    )

    suspend fun getLatest(api: MainAPI): List<SearchResponse> =
        getMovies("$BASE_URL/pelicula")
            .map { it.toSearchResponse(api) }

    suspend fun getCastellano(api: MainAPI): List<SearchResponse> =
        getMovies("$BASE_URL/pelis/idiomas/castellano")
            .map { it.toSearchResponse(api) }

    suspend fun getLatino(api: MainAPI): List<SearchResponse> =
        getMovies("$BASE_URL/pelis/idiomas/espanol-latino")
            .map { it.toSearchResponse(api) }

    suspend fun getVose(api: MainAPI): List<SearchResponse> =
        getMovies("$BASE_URL/pelis/idiomas/subtituladas-p02")
            .map { it.toSearchResponse(api) }

    suspend fun search(
        api: MainAPI,
        query: String
    ): List<SearchResponse> {

        if (query.isBlank()) return emptyList()

        val encoded = URLEncoder.encode(
            query.trim(),
            Charsets.UTF_8.name()
        )

        return getMovies("$BASE_URL/page/1?s=$encoded")
            .map { it.toSearchResponse(api) }
    }

    suspend fun loadMovie(
        url: String
    ): MovieDetails? {

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
                ?: document.selectFirst("meta[property=og:title]")
                    ?.attr("content")
                    ?.trim()
                ?: return null

        val poster =
            document.selectFirst("meta[property=og:image]")
                ?.attr("content")
                ?.takeIf { it.isNotBlank() }
                ?: document.selectFirst("img")
                    ?.attr("src")
                    ?.takeIf { it.isNotBlank() }

        val description =
            document.selectFirst("meta[name=description]")
                ?.attr("content")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val year =
            Regex("""\b(19|20)\d{2}\b""")
                .find(document.text())
                ?.value
                ?.toIntOrNull()

        val player =
            document.selectFirst("section.player")

        val urls =
            player
                ?.select("iframe")
                ?.mapNotNull { iframe ->
                    val raw =
                        iframe.attr("data-src")
                            .ifBlank { iframe.attr("src") }
                            .trim()

                    if (raw.isBlank()) {
                        null
                    } else {
                        normalizeUrl(
                            raw.replace("?h=", "r.php?h=")
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
            plot = description,
            playerUrls = urls
        )
    }

    private suspend fun getMovies(
        url: String
    ): List<MovieItem> {

        val document = try {
            app.get(url).document
        } catch (_: Throwable) {
            return emptyList()
        }

        return document
            .select("ul.post-lst li[class^=post-]")
            .mapNotNull { item ->

                val link =
                    item.selectFirst("a[href]")
                        ?.attr("href")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val title =
                    item.selectFirst("h2")
                        ?.text()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                val poster =
                    item.selectFirst("img")
                        ?.attr("src")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }

                val year =
                    item.selectFirst("span.year")
                        ?.text()
                        ?.trim()
                        ?.toIntOrNull()

                MovieItem(
                    title = title,
                    url = normalizeUrl(link),
                    posterUrl = poster?.let(::normalizeUrl),
                    year = year
                )
            }
            .distinctBy { it.url }
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
}
