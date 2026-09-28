package com.patriarca.movistarplus

object MovistarSessionManager {

    var accessToken: String = ""
        private set

    var sessionToken: String = ""
        private set

    var sspToken: String = ""
        private set

    var accountNumber: String = ""
        private set

    var encodedUser: String = ""
        private set

    var platform: String = ""
        private set

    var profileId: String = "0"
        private set

    var deviceId: String = ""
        private set

    var pid: String = ""
        private set

    var demarcation: Int = 0
        private set

    var subscription: String = ""
        private set

    var distilledTvRights: String = ""
        private set

    var linearSubscription: List<String> = emptyList()
        private set

    var vodSubscription: List<String> = emptyList()
        private set

    val isAuthenticated: Boolean
        get() = accessToken.isNotBlank()

    val isInitialized: Boolean
        get() =
            accessToken.isNotBlank() &&
            accountNumber.isNotBlank() &&
            deviceId.isNotBlank()

    fun setAccessToken(token: String) {
        accessToken = token
    }

    fun setAccount(
        accountNumber: String,
        encodedUser: String,
        platform: String
    ) {
        this.accountNumber = accountNumber
        this.encodedUser = encodedUser
        this.platform = platform
    }

    fun setProfile(profileId: String) {
        this.profileId = profileId
    }

    fun setDevice(deviceId: String) {
        this.deviceId = deviceId
    }

    fun setPlaybackSession(
        accessToken: String,
        sessionToken: String,
        sspToken: String,
        pid: String,
        demarcation: Int,
        subscription: String,
        distilledTvRights: String,
        linearSubscription: List<String>,
        vodSubscription: List<String>
    ) {
        this.accessToken = accessToken
        this.sessionToken = sessionToken
        this.sspToken = sspToken
        this.pid = pid
        this.demarcation = demarcation
        this.subscription = subscription
        this.distilledTvRights = distilledTvRights
        this.linearSubscription = linearSubscription
        this.vodSubscription = vodSubscription
    }

    fun clearPlaybackSession() {
        sessionToken = ""
        sspToken = ""
        pid = ""
        demarcation = 0
        subscription = ""
        distilledTvRights = ""
        linearSubscription = emptyList()
        vodSubscription = emptyList()
    }

    fun clear() {
        accessToken = ""
        sessionToken = ""
        sspToken = ""
        accountNumber = ""
        encodedUser = ""
        platform = ""
        profileId = "0"
        deviceId = ""
        pid = ""
        demarcation = 0
        subscription = ""
        distilledTvRights = ""
        linearSubscription = emptyList()
        vodSubscription = emptyList()
    }

    fun bearerHeaders(): Map<String, String> {
        if (accessToken.isBlank()) {
            return MovistarApi.defaultHeaders
        }

        return MovistarApi.defaultHeaders + mapOf(
            "Authorization" to "Bearer $accessToken"
        )
    }

    fun playbackHeaders(): Map<String, String> {
        val headers = bearerHeaders().toMutableMap()

        if (deviceId.isNotBlank()) {
            headers["X-Movistarplus-Deviceid"] = deviceId
        }

        if (sessionToken.isNotBlank()) {
            headers["X-HZId"] = sessionToken
        }

        return headers
    }
}
