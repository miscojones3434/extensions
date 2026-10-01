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

    /*
     * Equivalente al mainlist() de pelisforte.py:
     *
     * Novedades
     * Castellano
     * Latino
     * VOSE
     *
     * Generos, Alfabetico y Años se añadirán en el
     * siguiente bloque porque en Alfa son submenús,
     * no simples listados.
     */
    override val mainPage =
        mainPageOf(
            "$mainUrl/pelicula" to
                "PelisForte · Novedades",

            "$mainUrl/pelis/idiomas/castellano" to
                "PelisForte · Castellano",

            "$mainUrl/pelis/idiomas/espanol-latino" to
                "PelisForte · Latino",

            "$mainUrl/pelis/idiomas/subtituladas-p02" to
                "PelisForte · VOSE"
        )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val realPage =
            if (page < 1) {
                1
            } else {
                page
            }

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

    /*
     * search() de Alfa.
     */
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

    /*
     * En Alfa, list_all() llama:
     *
     * tmdb.set_infoLabels(itemlist, True)
     *
     * Aquí esos datos ya llegan desde
     * AlfaTmdbAdapter a MovieDetails.
     */
    override suspend fun load(
        url: String
    ): LoadResponse? {

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

                        if (actorName.isBlank()) {
                            return@mapNotNull null
                        }

                        val actorImage =
                            person.profilePath
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let {
                                    if (
                                        it.startsWith(
                                            "http://"
                                        ) ||
                                        it.startsWith(
                                            "https://"
                                        )
                                    ) {
                                        it
                                    } else {
                                        "https://image.tmdb.org/t/p/original$it"
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

    /*
     * Equivalente al flujo:
     *
     * findvideos()
     * -> play()
     * -> servertools.get_servers_itemlist()
     *
     * PelisForteAdapter ya:
     * - lee iframe[data-src]
     * - aplica ?h= -> r.php?h=
     * - obtiene servidor
     * - obtiene idioma
     * - ordena por idioma/servidor
     * - sigue la redirección de play()
     */
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
                PelisForteAdapter.resolvePlayer(
                    player
                )
                    ?: continue

            try {

                /*
                 * Alfa utiliza la URL del iframe como
                 * Referer especial para algunos hosts.
                 *
                 * En CloudStream el equivalente es
                 * pasar el referer al extractor.
                 */
                loadExtractor(
                    resolved,
                    player.url,
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
