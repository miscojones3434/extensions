package com.patriarca.movistarplus

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarLoginResponse(
    @JsonProperty("access_token")
    val accessToken: String? = null,

    @JsonProperty("token_type")
    val tokenType: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarAccountResponse(
    @JsonProperty("ofertas")
    val offers: List<MovistarOffer> = emptyList(),

    @JsonProperty("cod_usuario_cifrado")
    val encodedUser: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarOffer(
    @JsonProperty("accountNumber")
    val accountNumber: String? = null,

    @JsonProperty("@id_perfil")
    val profile: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarInitDataResponse(
    @JsonProperty("accessToken")
    val accessToken: String? = null,

    @JsonProperty("token")
    val sessionToken: String? = null,

    @JsonProperty("sspToken")
    val sspToken: String? = null,

    @JsonProperty("demarcation")
    val demarcation: Int? = null,

    @JsonProperty("pid")
    val pid: String? = null,

    @JsonProperty("suscripcion")
    val subscription: String? = null,

    @JsonProperty("distilledTvRights")
    val distilledTvRights: String? = null,

    @JsonProperty("linearSubscription")
    val linearSubscription: String? = null,

    @JsonProperty("vodSubscription")
    val vodSubscription: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarProfileResponse(
    @JsonProperty("items")
    val items: List<MovistarProfile> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarProfile(
    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("isForKids")
    val isForKids: Boolean? = null,

    @JsonProperty("typeID")
    val typeId: String? = null,

    @JsonProperty("imageID")
    val imageId: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarChannel(
    @JsonProperty("CodCadenaTv")
    val id: String? = null,

    @JsonProperty("Nombre")
    val name: String? = null,

    @JsonProperty("PuntoReproduccion")
    val playbackUrl: String? = null,

    @JsonProperty("Imagenes")
    val images: List<MovistarImage> = emptyList(),

    @JsonProperty("tvProducts")
    val products: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarImage(
    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("uri")
    val uri: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarVodItem(
    @JsonProperty("UrlVideo")
    val url: String? = null,

    @JsonProperty("CasId")
    val casId: String? = null,

    @JsonProperty("Tipo")
    val type: String? = null,

    @JsonProperty("Canal")
    val channel: MovistarVodChannel? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarVodChannel(
    @JsonProperty("CasId")
    val casId: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarEditorialData(
    @JsonProperty("Id")
    val id: String? = null,

    @JsonProperty("Titulo")
    val title: String? = null,

    @JsonProperty("TituloEpisodio")
    val episodeTitle: String? = null,

    @JsonProperty("NumeroEpisodio")
    val episodeNumber: Int? = null,

    @JsonProperty("DuracionEnSegundos")
    val durationSeconds: Int? = null,

    @JsonProperty("Temporada")
    val season: String? = null,

    @JsonProperty("Sinopsis")
    val plot: String? = null,

    @JsonProperty("Anno")
    val year: Int? = null,

    @JsonProperty("Imagenes")
    val images: List<MovistarImage> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarContentDetails(
    @JsonProperty("Id")
    val id: String? = null,

    @JsonProperty("Titulo")
    val title: String? = null,

    @JsonProperty("TituloEpisodio")
    val episodeTitle: String? = null,

    @JsonProperty("NumeroEpisodio")
    val episodeNumber: Int? = null,

    @JsonProperty("Duracion")
    val durationMinutes: Int? = null,

    @JsonProperty("DuracionEnSegundos")
    val durationSeconds: Int? = null,

    @JsonProperty("Sinopsis")
    val plot: String? = null,

    @JsonProperty("Anno")
    val year: Int? = null,

    @JsonProperty("Nacionalidad")
    val country: String? = null,

    @JsonProperty("Actores")
    val actors: String? = null,

    @JsonProperty("Directores")
    val directors: String? = null,

    @JsonProperty("Imagenes")
    val images: List<MovistarImage> = emptyList(),

    @JsonProperty("VodItems")
    val vodItems: List<MovistarVodItem> = emptyList(),

    @JsonProperty("Temporadas")
    val seasons: List<MovistarSeason> = emptyList(),

    @JsonProperty("Episodios")
    val episodes: List<MovistarEpisode> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarSeason(
    @JsonProperty("Id")
    val id: String? = null,

    @JsonProperty("Titulo")
    val title: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarEpisode(
    @JsonProperty("DatosEditoriales")
    val editorial: MovistarEditorialData? = null,

    @JsonProperty("VodItems")
    val vodItems: List<MovistarVodItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarEpgProgram(
    @JsonProperty("FechaHoraInicio")
    val start: Long? = null,

    @JsonProperty("FechaHoraFin")
    val end: Long? = null,

    @JsonProperty("Titulo")
    val title: String? = null,

    @JsonProperty("TituloHorLinea2")
    val subtitle: String? = null,

    @JsonProperty("ShowId")
    val showId: String? = null,

    @JsonProperty("SerialId")
    val seriesId: String? = null,

    @JsonProperty("Id")
    val id: String? = null,

    @JsonProperty("Canal")
    val channel: MovistarEpgChannel? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovistarEpgChannel(
    @JsonProperty("CodCadenaTv")
    val id: String? = null
)

data class MovistarPlaybackData(
    val contentId: String,
    val streamType: String,
    val url: String,
    val drmMediaId: String? = null
)
