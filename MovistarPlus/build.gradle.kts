dependencies {
    val cloudstream by configurations

    cloudstream("com.lagradost:cloudstream3:pre-release")
}

version = 4

cloudstream {
    description = "Movistar Plus+ desde Vu+/OpenWebif"
    authors = listOf("PATRIARCA")

    status = 3

    tvTypes = listOf(
        "Live"
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=movistarplus.es&sz=%size%"

    isCrossPlatform = false
}
