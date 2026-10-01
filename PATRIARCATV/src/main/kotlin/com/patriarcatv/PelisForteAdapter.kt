package com.patriarcatv

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newMovieSearchResponse
import java.net.URLDecoder
import java.net.URLEncoder

object PelisForteAdapter {

    const val BASE_URL = "https://www2.pelisforte.se"

    private val IDIOMAS = mapOf(
        "Subtitulado" to "VOSE",
        "Latino" to "LAT",
        "Castellano" to "CAST"
    )

    private val SERVER = mapOf(
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

    data class CatalogPage(
        val items: List<MovieItem>,
        val nextUrl: String?
    )

    data class NavigationItem(
        val title: String,
        val url: String,
        val extra: String = ""
    )

    data class PlayerItem(
        val url: String,
        val server: String,
        val language: String
    )

    data class ResolvedPlayer(
        val url: String,
        val referer: String
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
        val cast: List<AlfaTmdbAdapter.CastMember>
    )

    /*
     * Alfa:
     *
     * def create_soup(url, ...)
     *     data = httptools.downloadpage(url, canonical=canonical).data
     *     soup = BeautifulSoup(data, "html5lib")
     */
    private suspend fun createDocument(
        url: String
    ) = app.get(url).document

    /*
     * Alfa list_all(item)
     */
    suspend fun listAll(
        api: MainAPI,
        url: String,
        extra: String = ""
    ): CatalogPage {

        val document =
            try {
                createDocument(url)
            } catch (_: Throwable) {
                return CatalogPage(
                    emptyList(),
                    null
                )
            }

        val container =
            document.selectFirst("ul.post-lst")
                ?: return CatalogPage(
                    emptyList(),
                    null
                )

        /*
         * Alfa:
         *
         * matches = soup.find(
         *     'ul',
         *     class_='post-lst'
         * ).find_all(
         *     "li",
         *     class_=re.compile(r"^post-\d+")
         * )
         */
        val matches =
            container
                .select("li")
                .filter { element ->
                    element.classNames().any { className ->
                        Regex("""^post-\d+""")
                            .matches(className)
                    }
                }

        val basicItems =
            matches.mapNotNull { elem ->

                val link =
                    elem.selectFirst("a[href]")
                        ?: return@mapNotNull null

                val title =
                    elem.selectFirst("h2")
                        ?.text()
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                val href =
                    link.attr("href")
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                /*
                 * Alfa:
                 * thumbnail = elem.img['src']
                 */
                val thumbnail =
                    elem.selectFirst("img")
                        ?.attr("src")
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }

                /*
                 * Alfa:
                 * year = elem.find(
                 *     'span',
                 *     class_='year'
                 * ).text.strip()
                 */
                val year =
                    elem.selectFirst("span.year")
                        ?.text()
                        ?.trim()
                        ?.takeIf {
                            it.length == 4
                        }
                        ?.toIntOrNull()

                MovieItem(
                    title = title,
                    url = mediaUrl(
                        normalizeUrl(href),
                        extra
                    ),
                    posterUrl =
                        thumbnail?.let(
                            ::normalizeUrl
                        ),
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

        /*
         * Alfa:
         *
         * tmdb.set_infoLabels(
         *     itemlist,
         *     True
         * )
         */
        val enriched =
            basicItems.amap { item ->

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

        /*
         * Alfa:
         *
         * next_page = soup.find(
         *     'a',
         *     class_='current'
         * )
         *
         * if next_page and
         * next_page.find_next_sibling("a"):
         *     next_page =
         *       next_page.find_next_sibling("a")['href']
         */
        val current =
            document.selectFirst("a.current")

        val next =
            current
                ?.nextElementSibling()
                ?.takeIf {
                    it.tagName() == "a"
                }
                ?.attr("href")
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let(::normalizeUrl)

        return CatalogPage(
            items = enriched,
            nextUrl = next
        )
    }

    /*
     * Alfa section(item)
     */
    suspend fun section(
        url: String,
        title: String
    ): List<NavigationItem> {

        val document =
            try {
                createDocument(url)
            } catch (_: Throwable) {
                return emptyList()
            }

        val selector =
            if (
                title.contains(
                    "Sagas",
                    ignoreCase = true
                )
            ) {
                "li#menu-item-11504 li"
            } else {
                "li#menu-item-77 li"
            }

        return document
            .select(selector)
            .mapNotNull { elem ->

                val anchor =
                    elem.selectFirst("a[href]")
                        ?: return@mapNotNull null

                val itemTitle =
                    anchor.text()
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                val itemUrl =
                    anchor.attr("href")
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                NavigationItem(
                    title = itemTitle,
                    url = normalizeUrl(itemUrl)
                )
            }
    }

    /*
     * Alfa alphabet(item)
     */
    suspend fun alphabet(
        url: String,
        years: Boolean
    ): List<NavigationItem> {

        val document =
            try {
                createDocument(url)
            } catch (_: Throwable) {
                return emptyList()
            }

        val elements =
            if (years) {
                document
                    .select(
                        "section#torofilm_movies_annee-2 li"
                    )
                    .reversed()
            } else {
                document.select(
                    "section#wdgt_letter-2 li"
                )
            }

        return elements.mapNotNull { elem ->

            val anchor =
                elem.selectFirst("a[href]")
                    ?: return@mapNotNull null

            val itemTitle =
                anchor.text()
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: return@mapNotNull null

            val itemUrl =
                anchor.attr("href")
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: return@mapNotNull null

            NavigationItem(
                title = itemTitle,
                url = normalizeUrl(itemUrl)
            )
        }
    }

    /*
     * Alfa search(item, texto)
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
                .replace("+", "%20")

        return listAll(
            api = api,
            url = "$BASE_URL/page/1?s=$encoded"
        )
            .items
            .map {
                it.toSearchResponse(api)
            }
    }

    /*
     * Alfa findvideos(item)
     */
    suspend fun findVideos(
        mediaData: String
    ): List<PlayerItem> {

        val decoded =
            decodeMediaUrl(mediaData)

        val document =
            try {
                createDocument(decoded.url)
            } catch (_: Throwable) {
                return emptyList()
            }

        val playerSection =
            document.selectFirst(
                "section.player"
            )
                ?: return emptyList()

        val matches =
            playerSection.select("iframe")

        val serverLabels =
            playerSection.select("span.server")

        val list =
            matches.mapIndexedNotNull {
                    index,
                    elem ->

                /*
                 * Alfa:
                 * url = elem['data-src']
                 */
                val rawUrl =
                    elem.attr("data-src")
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapIndexedNotNull null

                val serv =
                    serverLabels
                        .getOrNull(index)
                        ?: return@mapIndexedNotNull null

                /*
                 * Alfa:
                 *
                 * srv, lang =
                 *     serv.text.split(" -")
                 */
                val parts =
                    serv.text()
                        .split(
                            " -",
                            limit = 2
                        )

                if (parts.size != 2) {
                    return@mapIndexedNotNull null
                }

                val srv =
                    parts[0]
                        .trim()
                        .lowercase()

                val lang =
                    parts[1]
                        .trim()
                        .split(" ")
                        .last()

                val language =
                    IDIOMAS[lang]
                        ?: lang

                val server =
                    SERVER[srv]
                        ?: srv

                PlayerItem(
                    url =
                        normalizeUrl(
                            rawUrl.replace(
                                "?h=",
                                "r.php?h="
                            )
                        ),
                    server = server,
                    language = language
                )
            }
                .sortedWith(
                    compareBy<PlayerItem>(
                        { it.language },
                        { it.server }
                    )
                )

        /*
         * Alfa:
         *
         * if item.extra:
         *     itemlist = [
         *         i for i in itemlist
         *         if i.language == item.extra
         *     ]
         */
        return if (
            decoded.extra.isNotBlank()
        ) {
            list.filter {
                it.language ==
                    decoded.extra
            }
        } else {
            list
        }
    }

    /*
     * Alfa play(item)
     */
    suspend fun play(
        player: PlayerItem
    ): ResolvedPlayer? {

        val resolved =
            try {
                app.get(
                    player.url,
                    allowRedirects = true
                ).url
            } catch (_: Throwable) {
                return null
            }

        /*
         * Alfa:
         *
         * if "okhd" in url:
         *     url +=
         *       "|Referer=%s" % item.url
         *
         * CloudStream separa URL y Referer.
         */
        val referer =
            if (
                resolved.contains(
                    "okhd",
                    ignoreCase = true
                )
            ) {
                player.url
            } else {
                player.url
            }

        return ResolvedPlayer(
            url = resolved,
            referer = referer
        )
    }

    suspend fun loadMovie(
        mediaData: String
    ): MovieDetails? {

        val decoded =
            decodeMediaUrl(mediaData)

        val document =
            try {
                createDocument(decoded.url)
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

        val year =
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

        val sitePoster =
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

        val tmdb =
            try {
                AlfaTmdbAdapter.getMovie(
                    title = title,
                    year = year
                )
            } catch (_: Throwable) {
                null
            }

        return MovieDetails(
            title =
                tmdb?.title
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: title,

            url = mediaData,

            posterUrl =
                tmdb?.posterUrl
                    ?: sitePoster,

            year =
                tmdb?.year
                    ?: year,

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
                    ?: emptyList()
        )
    }

    fun MovieItem.toSearchResponse(
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

    /*
     * CloudStream necesita transportar item.extra
     * junto a la URL.
     *
     * El fragmento #... no se envía al servidor.
     */
    private fun mediaUrl(
        url: String,
        extra: String
    ): String {

        if (extra.isBlank()) {
            return url
        }

        return "$url#patriarcatv_extra=${
            URLEncoder.encode(
                extra,
                Charsets.UTF_8.name()
            )
        }"
    }

    private data class MediaData(
        val url: String,
        val extra: String
    )

    private fun decodeMediaUrl(
        value: String
    ): MediaData {

        val url =
            value.substringBefore(
                "#patriarcatv_extra="
            )

        val rawExtra =
            value.substringAfter(
                "#patriarcatv_extra=",
                ""
            )

        val extra =
            if (rawExtra.isBlank()) {
                ""
            } else {
                URLDecoder.decode(
                    rawExtra,
                    Charsets.UTF_8.name()
                )
            }

        return MediaData(
            url = url,
            extra = extra
        )
    }

    fun normalizeUrl(
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
