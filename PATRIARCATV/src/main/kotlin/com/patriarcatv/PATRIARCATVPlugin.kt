package com.patriarcatv

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PATRIARCATVPlugin : BasePlugin() {

    override fun load() {
        registerMainAPI(
            PATRIARCATVProvider()
        )
    }
}
