dependencies {
    val cloudstream by configurations

    cloudstream("com.lagradost:cloudstream3:pre-release")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

version = 2

cloudstream {
    description = "Movistar Plus+ España"
    authors = listOf("PATRIARCA")

    status = 3

    tvTypes = listOf(
        "Live",
        "Movie",
        "TvSeries",
        "Documentary"
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=movistarplus.es&sz=%size%"

    isCrossPlatform = false
}
