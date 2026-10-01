package com.patriarcatv

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

object AlfaTmdbAdapter {

    private const val API_URL =
        "https://api.themoviedb.org/3"

    private const val IMAGE_URL =
        "https://image.tmdb.org/t/p/original"

    private const val API_KEY =
        "a1ab8b8669da03637a4b98fa39c39228"

    data class SearchResult(
        @JsonProperty("results")
        val results: List<SearchMovie> = emptyList()
    )

    data class SearchMovie(
        @JsonProperty("id")
        val id: Int = 0,

        @JsonProperty("title")
        val title: String? = null,

        @JsonProperty("original_title")
        val originalTitle: String? = null,

        @JsonProperty("overview")
        val overview: String? = null,

        @JsonProperty("poster_path")
        val posterPath: String? = null,

        @JsonProperty("backdrop_path")
        val backdropPath: String? = null,

        @JsonProperty("release_date")
        val releaseDate: String? = null,

        @JsonProperty("vote_average")
        val voteAverage: Double? = null
    )

    data class MovieDetails(
        @JsonProperty("id")
        val id: Int = 0,

        @JsonProperty("title")
        val title: String? = null,

        @JsonProperty("original_title")
        val originalTitle: String? = null,

        @JsonProperty("overview")
        val overview: String? = null,

        @JsonProperty("poster_path")
        val posterPath: String? = null,

        @JsonProperty("backdrop_path")
        val backdropPath: String? = null,

        @JsonProperty("release_date")
        val releaseDate: String? = null,

        @JsonProperty("runtime")
        val runtime: Int? = null,

        @JsonProperty("vote_average")
        val voteAverage: Double? = null,

        @JsonProperty("genres")
        val genres: List<Genre> = emptyList(),

        @JsonProperty("credits")
        val credits: Credits? = null
    )

    data class Genre(
        @JsonProperty("id")
        val id: Int = 0,

        @JsonProperty("name")
        val name: String = ""
    )

    data class Credits(
        @JsonProperty("cast")
        val cast: List<CastMember> = emptyList()
    )

    data class CastMember(
        @JsonProperty("name")
        val name: String = "",

        @JsonProperty("character")
        val character: String? = null,

        @JsonProperty("profile_path")
        val profilePath: String? = null
    )

    data class Metadata(
        val tmdbId: Int,
        val title: String,
        val originalTitle: String?,
        val year: Int?,
        val plot: String?,
        val posterUrl: String?,
        val backdropUrl: String?,
        val durationMinutes: Int?,
        val rating: Double?,
        val genres: List<String>,
        val cast: List<CastMember>
    )

    suspend fun getMovie(
        title: String,
        year: Int?
    ): Metadata? {

        if (title.isBlank()) {
            return null
        }

        val searchParams =
            mutableMapOf(
                "api_key" to API_KEY,
                "query" to title,
                "language" to "es",
                "include_adult" to "false",
                "page" to "1"
            )

        /*
         * Igual que Tmdb.__search() de Alfa:
         * para películas añade primary_release_year.
         */
        if (year != null) {
            searchParams["primary_release_year"] =
                year.toString()
        }

        val search =
            try {
                app.get(
                    "$API_URL/search/movie",
                    params = searchParams
                ).text
            } catch (_: Throwable) {
                return null
            }

        val result =
            tryParseJson<SearchResult>(search)
                ?.results
                ?.firstOrNull()
                ?: return null

        if (result.id <= 0) {
            return null
        }

        /*
         * Igual que Tmdb.__by_id() de Alfa:
         *
         * /movie/{id}
         * language=es
         * append_to_response=images,videos,external_ids,credits
         * include_image_language=es,null
         */
        val detail =
            try {
                app.get(
                    "$API_URL/movie/${result.id}",
                    params = mapOf(
                        "api_key" to API_KEY,
                        "language" to "es",
                        "append_to_response" to
                            "images,videos,external_ids,credits",
                        "include_image_language" to
                            "es,null"
                    )
                ).text
            } catch (_: Throwable) {
                return metadataFromSearch(result)
            }

        val movie =
            tryParseJson<MovieDetails>(detail)
                ?: return metadataFromSearch(result)

        return Metadata(
            tmdbId = movie.id,
            title =
                movie.title
                    ?.takeIf { it.isNotBlank() }
                    ?: result.title
                    ?.takeIf { it.isNotBlank() }
                    ?: title,

            originalTitle =
                movie.originalTitle
                    ?.takeIf { it.isNotBlank() },

            year =
                extractYear(movie.releaseDate)
                    ?: extractYear(result.releaseDate)
                    ?: year,

            plot =
                movie.overview
                    ?.takeIf { it.isNotBlank() }
                    ?: result.overview
                        ?.takeIf { it.isNotBlank() },

            posterUrl =
                imageUrl(
                    movie.posterPath
                        ?: result.posterPath
                ),

            backdropUrl =
                imageUrl(
                    movie.backdropPath
                        ?: result.backdropPath
                ),

            durationMinutes =
                movie.runtime,

            rating =
                movie.voteAverage
                    ?: result.voteAverage,

            genres =
                movie.genres
                    .mapNotNull {
                        it.name
                            .trim()
                            .takeIf { name ->
                                name.isNotBlank()
                            }
                    },

            cast =
                movie.credits
                    ?.cast
                    ?.take(20)
                    ?: emptyList()
        )
    }

    private fun metadataFromSearch(
        movie: SearchMovie
    ): Metadata {

        return Metadata(
            tmdbId = movie.id,
            title =
                movie.title
                    ?.takeIf { it.isNotBlank() }
                    ?: movie.originalTitle
                    ?: "",

            originalTitle =
                movie.originalTitle,

            year =
                extractYear(movie.releaseDate),

            plot =
                movie.overview
                    ?.takeIf { it.isNotBlank() },

            posterUrl =
                imageUrl(movie.posterPath),

            backdropUrl =
                imageUrl(movie.backdropPath),

            durationMinutes = null,

            rating =
                movie.voteAverage,

            genres =
                emptyList(),

            cast =
                emptyList()
        )
    }

    private fun imageUrl(
        path: String?
    ): String? {

        if (path.isNullOrBlank()) {
            return null
        }

        return if (
            path.startsWith("http://") ||
            path.startsWith("https://")
        ) {
            path
        } else {
            "$IMAGE_URL$path"
        }
    }

    private fun extractYear(
        date: String?
    ): Int? {

        return date
            ?.take(4)
            ?.toIntOrNull()
    }
}
