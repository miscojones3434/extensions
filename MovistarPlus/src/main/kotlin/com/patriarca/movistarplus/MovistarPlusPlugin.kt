package com.patriarca.movistarplus

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class MovistarPlusPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(MovistarPlusProvider())
    }
}
