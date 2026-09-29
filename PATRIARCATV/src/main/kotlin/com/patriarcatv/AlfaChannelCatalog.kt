package com.patriarcatv

object AlfaChannelCatalog {

    data class ChannelDefinition(
        val id: String,
        val title: String,
        val category: Category
    )

    enum class Category {
        MOVIES,
        SERIES,
        ANIME,
        DOCUMENTARIES,
        ASIAN,
        CARTOONS,
        OTHER
    }

    val channels: List<ChannelDefinition> = listOf(
        ChannelDefinition(
            id = "allcalidad",
            title = "AllCalidad",
            category = Category.MOVIES
        ),
        ChannelDefinition(
            id = "allpeliculas",
            title = "AllPeliculas",
            category = Category.MOVIES
        ),
        ChannelDefinition(
            id = "cine24h",
            title = "Cine24h",
            category = Category.MOVIES
        ),
        ChannelDefinition(
            id = "cineasiaenlinea",
            title = "CineAsiaEnLinea",
            category = Category.ASIAN
        ),
        ChannelDefinition(
            id = "cinecalidad",
            title = "CineCalidad",
            category = Category.MOVIES
        ),
        ChannelDefinition(
            id = "cinelibreonline",
            title = "CineLibreOnline",
            category = Category.MOVIES
        ),
        ChannelDefinition(
            id = "cinemundo",
            title = "CineMundo",
            category = Category.MOVIES
        ),
        ChannelDefinition(
            id = "documentalesonline",
            title = "DocumentalesOnline",
            category = Category.DOCUMENTARIES
        ),
        ChannelDefinition(
            id = "doramasflix",
            title = "DoramasFlix",
            category = Category.ASIAN
        ),
        ChannelDefinition(
            id = "doramasqueen",
            title = "DoramasQueen",
            category = Category.ASIAN
        ),
        ChannelDefinition(
            id = "doramasyt",
            title = "DoramasYT",
            category = Category.ASIAN
        ),
        ChannelDefinition(
            id = "hdfull",
            title = "HDFull",
            category = Category.SERIES
        ),
        ChannelDefinition(
            id = "hdfulls",
            title = "HDFulls",
            category = Category.SERIES
        ),
        ChannelDefinition(
            id = "jkanime",
            title = "JKAnime",
            category = Category.ANIME
        ),
        ChannelDefinition(
            id = "animeflv",
            title = "AnimeFLV",
            category = Category.ANIME
        ),
        ChannelDefinition(
            id = "animefenix",
            title = "AnimeFenix",
            category = Category.ANIME
        ),
        ChannelDefinition(
            id = "animejara",
            title = "AnimeJara",
            category = Category.ANIME
        ),
        ChannelDefinition(
            id = "animejl",
            title = "AnimeJL",
            category = Category.ANIME
        ),
        ChannelDefinition(
            id = "lacartoons",
            title = "LACartoons",
            category = Category.CARTOONS
        ),
        ChannelDefinition(
            id = "legalmentegratis",
            title = "LegalmenteGratis",
            category = Category.OTHER
        )
    )
}
