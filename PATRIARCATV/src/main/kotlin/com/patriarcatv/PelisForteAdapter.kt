package com.patriarcatv

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newMovieSearchResponse
import java.net.URLEncoder

object PelisForteAdapter {

    private const val BASE_URL =
        "https://www2.pelisforte.se"

    /*
     * pelisforte.py oficial:
     *
     * IDIOMAS = {
     *   'Subtitulado': 'VOSE',
     *   'Latino': 'LAT',
     *   'Castellano': 'CAST'
     * }
     */
    val languages = mapOf(
        "Subtitulado" to "VOSE",
        "Latino" to "LAT",
        "Castellano" to "CAST"
    )

    /*
     * pelisforte.py oficial:
     *
     * SERVER = {
     *   'swish': 'Streamwish',
     *   'vgfplay': 'Vidguard',
     *   'playpf': 'Tiwikiwi',
     *   'filemoon': 'Filemoon',
     *   'okhd': 'Okhd',
     *   'bf0skv': 'Filemoon',
     *   'byse': 'Filemoon',
     *   'w1tv': 'Kinoger'
     * }
     */
    val servers = mapOf(
        "swish" to "Streamwish",
        "vgfplay" to "Vidguard",
        "playpf" to "Tiwikiwi",
        "filemoon" to "Filemoon",
        "okhd" to "Okhd",
        "bf0skv" to "Filemoon",
        "byse" to "Filemoon",
        "w1tv" to "Kinoger"
    )

    data class MovieItem(
        val title: String,
        val url: String,
        val posterUrl: String?,
        val year: Int?,
        val tmdbId: Int?,
        val plot: String?,
        val backdropUrl: String?,
        val durationMinutes: Int?,
        val rating: Double?,
        val genres: List<String>,
        val cast: List<AlfaTmdbAdapter.CastMember>
    )

    data class PlayerItem(
        val url: String,
        val server: String,
        val language: String
    )

    data class MovieDetails(
        val title: String,
        val url: String,
        val posterUrl: String?,
        val year: Int?,
        val tmdbId: Int?,
        val plot: String?,
        val backdropUrl: String?,
        val durationMinutes: Int?,
        val rating: Double?,
        val genres: List<String>,
        val cast: List<AlfaTmdbAdapter.CastMember>,
        val players: List<PlayerItem>
    )

    /*
     * mainlist():
     * Novedades
     */
    suspend fun getLatest(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> {

        return getMovies(
            pageUrl(
                "$BASE_URL/pelicula",
                page
            )
        ).map {
            it.toSearchResponse(api)
        }
    }

    /*
     * mainlist():
     * Castellano
     */
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

    /*
     * mainlist():
     * Latino
     */
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

    /*
     * mainlist():
     * VOSE
     */
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

    /*
     * search() oficial:
     *
     * item.url = "%s/page/1?s=%s"
     */
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

    /*
     * list_all() de Alfa:
     *
     * soup.find('ul', class_='post-lst')
     *     .find_all("li", class_=re.compile(r"^post-\d+"))
     *
     * url = elem.a['href']
     * title = elem.h2.text.strip()
     * thumbnail = elem.img['src']
     * year = elem.find('span', class_='year').text.strip()
     *
     * tmdb.set_infoLabels(itemlist, True)
     */
    private suspend fun getMovies(
        url: String
    ): List<MovieItem> {

        val document =
            try {
                app.get(url).document
            } catch (_: Throwable) {
                return emptyList()
            }

        val container =
            document.selectFirst("ul.post-lst")
                ?: return emptyList()

        val rawItems =
            container
                .select("li")
                .filter { element ->

                    element.classNames().any { className ->
                        Regex("""^post-\d+""")
                            .containsMatchIn(className)
                    }
                }
                .mapNotNull { element ->

                    val href =
                        element
                            .selectFirst("a[href]")
                            ?.attr("href")
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return@mapNotNull null

                    val title =
                        element
                            .selectFirst("h2")
                            ?.text()
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return@mapNotNull null

                    /*
                     * Alfa usa elem.img['src'].
                     * Se respeta src como primera opción.
                     */
                    val image =
                        element.selectFirst("img")

                    val poster =
                        image
                            ?.attr("src")
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }

                    val yearText =
                        element
                            .selectFirst("span.year")
                            ?.text()
                            ?.trim()

                    val year =
                        yearText
                            ?.takeIf {
                                it.length == 4
                            }
                            ?.toIntOrNull()

                    MovieItem(
                        title = title,
                        url = normalizeUrl(href),
                        posterUrl =
                            poster?.let(::normalizeUrl),
                        year = year,

                        tmdbId = null,
                        plot = null,
                        backdropUrl = null,
                        durationMinutes = null,
                        rating = null,
                        genres = emptyList(),
                        cast = emptyList()
                    )
                }
                .distinctBy {
                    it.url
                }

        /*
         * Equivalente a:
         *
         * tmdb.set_infoLabels(itemlist, True)
         *
         * CloudStream dispone de amap(), que ejecuta
         * las consultas suspend de forma concurrente.
         */
        return rawItems.amap { item ->

            val tmdb =
                try {
                    AlfaTmdbAdapter.getMovie(
                        title = item.title,
                        year = item.year
                    )
                } catch (_: Throwable) {
                    null
                }

            if (tmdb == null) {
                item
            } else {
                item.copy(
                    title =
                        tmdb.title
                            .takeIf {
                                it.isNotBlank()
                            }
                            ?: item.title,

                    posterUrl =
                        tmdb.posterUrl
                            ?: item.posterUrl,

                    year =
                        tmdb.year
                            ?: item.year,

                    tmdbId =
                        tmdb.tmdbId,

                    plot =
                        tmdb.plot,

                    backdropUrl =
                        tmdb.backdropUrl,

                    durationMinutes =
                        tmdb.durationMinutes,

                    rating =
                        tmdb.rating,

                    genres =
                        tmdb.genres,

                    cast =
                        tmdb.cast
                )
            }
        }
    }

    /*
     * Ficha de película.
     *
     * Primero se leen los datos reales de la página.
     * Después se hace el mismo enriquecimiento TMDB.
     * Finalmente se ejecuta el equivalente de findvideos().
     */
    suspend fun loadMovie(
        url: String
    ): MovieDetails? {

        val document =
            try {
                app.get(url).document
            } catch (_: Throwable) {
                return null
            }

        val title =
            document
                .selectFirst("h1")
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

        val pagePoster =
            document
                .selectFirst(
                    "meta[property=og:image]"
                )
                ?.attr("content")
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let(::normalizeUrl)

        val pageYear =
            document
                .selectFirst("span.year")
                ?.text()
                ?.trim()
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

        val tmdb =
            try {
                AlfaTmdbAdapter.getMovie(
                    title = title,
                    year = pageYear
                )
            } catch (_: Throwable) {
                null
            }

        val players =
            parsePlayers(document)

        return MovieDetails(
            title =
                tmdb
                    ?.title
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: title,

            url = url,

            posterUrl =
                tmdb?.posterUrl
                    ?: pagePoster,

            year =
                tmdb?.year
                    ?: pageYear,

            tmdbId =
                tmdb?.tmdbId,

            plot =
                tmdb?.plot,

            backdropUrl =
                tmdb?.backdropUrl,

            durationMinutes =
                tmdb?.durationMinutes,

            rating =
                tmdb?.rating,

            genres =
                tmdb?.genres
                    ?: emptyList(),

            cast =
                tmdb?.cast
                    ?: emptyList(),

            players =
                players
        )
    }

    /*
     * findvideos() oficial:
     *
     * soup = create_soup(item.url)
     *     .find('section', class_='player')
     *
     * matches = soup.find_all("iframe")
     * servers = soup.find_all("span", class_="server")
     *
     * for elem, serv in zip(matches, servers):
     *     url = elem['data-src']
     *     url = url.replace("?h=", "r.php?h=")
     *
     *     srv, lang = serv.text.split(" -")
     *     srv = srv.strip().lower()
     *     lang = lang.strip().split(" ")[-1]
     *
     *     language = IDIOMAS.get(lang, lang)
     *     server = SERVER.get(srv, srv)
     */
    private fun parsePlayers(
        document: org.jsoup.nodes.Document
    ): List<PlayerItem> {

        val playerSection =
            document.selectFirst("section.player")
                ?: return emptyList()

        val iframes =
            playerSection.select("iframe")

        val serverLabels =
            playerSection.select("span.server")

        return iframes
            .mapIndexedNotNull { index, iframe ->

                val rawUrl =
                    iframe
                        .attr("data-src")
                        .ifBlank {
                            iframe.attr("src")
                        }
                        .trim()

                if (rawUrl.isBlank()) {
                    return@mapIndexedNotNull null
                }

                val label =
                    serverLabels
                        .getOrNull(index)
                        ?.text()
                        ?.trim()
                        .orEmpty()

                val parts =
                    label.split(
                        " -",
                        limit = 2
                    )

                val rawServer =
                    parts
                        .getOrNull(0)
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()

                val rawLanguage =
                    parts
                        .getOrNull(1)
                        ?.trim()
                        ?.split(" ")
                        ?.lastOrNull()
                        .orEmpty()

                val server =
                    servers[rawServer]
                        ?: rawServer

                val language =
                    languages[rawLanguage]
                        ?: rawLanguage

                PlayerItem(
                    url =
                        normalizeUrl(
                            rawUrl.replace(
                                "?h=",
                                "r.php?h="
                            )
                        ),

                    server =
                        server,

                    language =
                        language
                )
            }
            .sortedWith(
                compareBy<PlayerItem>(
                    { it.language },
                    { it.server }
                )
            )
            .distinctBy {
                Triple(
                    it.url,
                    it.server,
                    it.language
                )
            }
    }

    /*
     * play() oficial:
     *
     * url = httptools.downloadpage(item.url).url
     *
     * if "okhd" in url:
     *     url += "|Referer=%s" % item.url
     */
    suspend fun resolvePlayer(
        player: PlayerItem
    ): String? {

        val resolved =
            try {
                app.get(
                    player.url,
                    allowRedirects = true
                ).url
            } catch (_: Throwable) {
                return null
            }

        return if (
            resolved.contains(
                "okhd",
                ignoreCase = true
            )
        ) {
            "$resolved|Referer=${player.url}"
        } else {
            resolved
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
cat > PATRIARCATV/src/main/kotlin/com/patriarcatv/PelisForteAdapter.kt <<'EOF'
package com.patriarcatv

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newMovieSearchResponse
import java.net.URLEncoder

object PelisForteAdapter {

    private const val BASE_URL =
        "https://www2.pelisforte.se"

    /*
     * pelisforte.py oficial:
     *
     * IDIOMAS = {
     *   'Subtitulado': 'VOSE',
     *   'Latino': 'LAT',
     *   'Castellano': 'CAST'
     * }
     */
    val languages = mapOf(
        "Subtitulado" to "VOSE",
        "Latino" to "LAT",
        "Castellano" to "CAST"
    )

    /*
     * pelisforte.py oficial:
     *
     * SERVER = {
     *   'swish': 'Streamwish',
     *   'vgfplay': 'Vidguard',
     *   'playpf': 'Tiwikiwi',
     *   'filemoon': 'Filemoon',
     *   'okhd': 'Okhd',
     *   'bf0skv': 'Filemoon',
     *   'byse': 'Filemoon',
     *   'w1tv': 'Kinoger'
     * }
     */
    val servers = mapOf(
        "swish" to "Streamwish",
        "vgfplay" to "Vidguard",
        "playpf" to "Tiwikiwi",
        "filemoon" to "Filemoon",
        "okhd" to "Okhd",
        "bf0skv" to "Filemoon",
        "byse" to "Filemoon",
        "w1tv" to "Kinoger"
    )

    data class MovieItem(
        val title: String,
        val url: String,
        val posterUrl: String?,
        val year: Int?,
        val tmdbId: Int?,
        val plot: String?,
        val backdropUrl: String?,
        val durationMinutes: Int?,
        val rating: Double?,
        val genres: List<String>,
        val cast: List<AlfaTmdbAdapter.CastMember>
    )

    data class PlayerItem(
        val url: String,
        val server: String,
        val language: String
    )

    data class MovieDetails(
        val title: String,
        val url: String,
        val posterUrl: String?,
        val year: Int?,
        val tmdbId: Int?,
        val plot: String?,
        val backdropUrl: String?,
        val durationMinutes: Int?,
        val rating: Double?,
        val genres: List<String>,
        val cast: List<AlfaTmdbAdapter.CastMember>,
        val players: List<PlayerItem>
    )

    /*
     * mainlist():
     * Novedades
     */
    suspend fun getLatest(
        api: MainAPI,
        page: Int = 1
    ): List<SearchResponse> {

        return getMovies(
            pageUrl(
                "$BASE_URL/pelicula",
                page
            )
        ).map {
            it.toSearchResponse(api)
        }
    }

    /*
     * mainlist():
     * Castellano
     */
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

    /*
     * mainlist():
     * Latino
     */
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

    /*
     * mainlist():
     * VOSE
     */
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

    /*
     * search() oficial:
     *
     * item.url = "%s/page/1?s=%s"
     */
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

    /*
     * list_all() de Alfa:
     *
     * soup.find('ul', class_='post-lst')
     *     .find_all("li", class_=re.compile(r"^post-\d+"))
     *
     * url = elem.a['href']
     * title = elem.h2.text.strip()
     * thumbnail = elem.img['src']
     * year = elem.find('span', class_='year').text.strip()
     *
     * tmdb.set_infoLabels(itemlist, True)
     */
    private suspend fun getMovies(
        url: String
    ): List<MovieItem> {

        val document =
            try {
                app.get(url).document
            } catch (_: Throwable) {
                return emptyList()
            }

        val container =
            document.selectFirst("ul.post-lst")
                ?: return emptyList()

        val rawItems =
            container
                .select("li")
                .filter { element ->

                    element.classNames().any { className ->
                        Regex("""^post-\d+""")
                            .containsMatchIn(className)
                    }
                }
                .mapNotNull { element ->

                    val href =
                        element
                            .selectFirst("a[href]")
                            ?.attr("href")
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return@mapNotNull null

                    val title =
                        element
                            .selectFirst("h2")
                            ?.text()
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return@mapNotNull null

                    /*
                     * Alfa usa elem.img['src'].
                     * Se respeta src como primera opción.
                     */
                    val image =
                        element.selectFirst("img")

                    val poster =
                        image
                            ?.attr("src")
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }

                    val yearText =
                        element
                            .selectFirst("span.year")
                            ?.text()
                            ?.trim()

                    val year =
                        yearText
                            ?.takeIf {
                                it.length == 4
                            }
                            ?.toIntOrNull()

                    MovieItem(
                        title = title,
                        url = normalizeUrl(href),
                        posterUrl =
                            poster?.let(::normalizeUrl),
                        year = year,

                        tmdbId = null,
                        plot = null,
                        backdropUrl = null,
                        durationMinutes = null,
                        rating = null,
                        genres = emptyList(),
                        cast = emptyList()
                    )
                }
                .distinctBy {
                    it.url
                }

        /*
         * Equivalente a:
         *
         * tmdb.set_infoLabels(itemlist, True)
         *
         * CloudStream dispone de amap(), que ejecuta
         * las consultas suspend de forma concurrente.
         */
        return rawItems.amap { item ->

            val tmdb =
                try {
                    AlfaTmdbAdapter.getMovie(
                        title = item.title,
                        year = item.year
                    )
                } catch (_: Throwable) {
                    null
                }

            if (tmdb == null) {
                item
            } else {
                item.copy(
                    title =
                        tmdb.title
                            .takeIf {
                                it.isNotBlank()
                            }
                            ?: item.title,

                    posterUrl =
                        tmdb.posterUrl
                            ?: item.posterUrl,

                    year =
                        tmdb.year
                            ?: item.year,

                    tmdbId =
                        tmdb.tmdbId,

                    plot =
                        tmdb.plot,

                    backdropUrl =
                        tmdb.backdropUrl,

                    durationMinutes =
                        tmdb.durationMinutes,

                    rating =
                        tmdb.rating,

                    genres =
                        tmdb.genres,

                    cast =
                        tmdb.cast
                )
            }
        }
    }

    /*
     * Ficha de película.
     *
     * Primero se leen los datos reales de la página.
     * Después se hace el mismo enriquecimiento TMDB.
     * Finalmente se ejecuta el equivalente de findvideos().
     */
    suspend fun loadMovie(
        url: String
    ): MovieDetails? {

        val document =
            try {
                app.get(url).document
            } catch (_: Throwable) {
                return null
            }

        val title =
            document
                .selectFirst("h1")
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

        val pagePoster =
            document
                .selectFirst(
                    "meta[property=og:image]"
                )
                ?.attr("content")
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let(::normalizeUrl)

        val pageYear =
            document
                .selectFirst("span.year")
                ?.text()
                ?.trim()
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

        val tmdb =
            try {
                AlfaTmdbAdapter.getMovie(
                    title = title,
                    year = pageYear
                )
            } catch (_: Throwable) {
                null
            }

        val players =
            parsePlayers(document)

        return MovieDetails(
            title =
                tmdb
                    ?.title
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: title,

            url = url,

            posterUrl =
                tmdb?.posterUrl
                    ?: pagePoster,

            year =
                tmdb?.year
                    ?: pageYear,

            tmdbId =
                tmdb?.tmdbId,

            plot =
                tmdb?.plot,

            backdropUrl =
                tmdb?.backdropUrl,

            durationMinutes =
                tmdb?.durationMinutes,

            rating =
                tmdb?.rating,

            genres =
                tmdb?.genres
                    ?: emptyList(),

            cast =
                tmdb?.cast
                    ?: emptyList(),

            players =
                players
        )
    }

    /*
     * findvideos() oficial:
     *
     * soup = create_soup(item.url)
     *     .find('section', class_='player')
     *
     * matches = soup.find_all("iframe")
     * servers = soup.find_all("span", class_="server")
     *
     * for elem, serv in zip(matches, servers):
     *     url = elem['data-src']
     *     url = url.replace("?h=", "r.php?h=")
     *
     *     srv, lang = serv.text.split(" -")
     *     srv = srv.strip().lower()
     *     lang = lang.strip().split(" ")[-1]
     *
     *     language = IDIOMAS.get(lang, lang)
     *     server = SERVER.get(srv, srv)
     */
    private fun parsePlayers(
        document: org.jsoup.nodes.Document
    ): List<PlayerItem> {

        val playerSection =
            document.selectFirst("section.player")
                ?: return emptyList()

        val iframes =
            playerSection.select("iframe")

        val serverLabels =
            playerSection.select("span.server")

        return iframes
            .mapIndexedNotNull { index, iframe ->

                val rawUrl =
                    iframe
                        .attr("data-src")
                        .ifBlank {
                            iframe.attr("src")
                        }
                        .trim()

                if (rawUrl.isBlank()) {
                    return@mapIndexedNotNull null
                }

                val label =
                    serverLabels
                        .getOrNull(index)
                        ?.text()
                        ?.trim()
                        .orEmpty()

                val parts =
                    label.split(
                        " -",
                        limit = 2
                    )

                val rawServer =
                    parts
                        .getOrNull(0)
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()

                val rawLanguage =
                    parts
                        .getOrNull(1)
                        ?.trim()
                        ?.split(" ")
                        ?.lastOrNull()
                        .orEmpty()

                val server =
                    servers[rawServer]
                        ?: rawServer

                val language =
                    languages[rawLanguage]
                        ?: rawLanguage

                PlayerItem(
                    url =
                        normalizeUrl(
                            rawUrl.replace(
                                "?h=",
                                "r.php?h="
                            )
                        ),

                    server =
                        server,

                    language =
                        language
                )
            }
            .sortedWith(
                compareBy<PlayerItem>(
                    { it.language },
                    { it.server }
                )
            )
            .distinctBy {
                Triple(
                    it.url,
                    it.server,
                    it.language
                )
            }
    }

    /*
     * play() oficial:
     *
     * url = httptools.downloadpage(item.url).url
     *
     * if "okhd" in url:
     *     url += "|Referer=%s" % item.url
     */
    suspend fun resolvePlayer(
        player: PlayerItem
    ): String? {

        val resolved =
            try {
                app.get(
                    player.url,
                    allowRedirects = true
                ).url
            } catch (_: Throwable) {
                return null
            }

        return if (
            resolved.contains(
                "okhd",
                ignoreCase = true
            )
        ) {
            "$resolved|Referer=${player.url}"
        } else {
            resolved
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


