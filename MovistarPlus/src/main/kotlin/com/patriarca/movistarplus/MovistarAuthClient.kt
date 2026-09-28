package com.patriarca.movistarplus

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

object MovistarAuthClient {

    suspend fun login(
        username: String,
        password: String
    ): Boolean {
        val response = app.post(
            MovistarApi.TOKEN,
            data = mapOf(
                "grant_type" to "password",
                "deviceClass" to MovistarApi.DEVICE_PLAYER,
                "username" to username,
                "password" to password
            ),
            headers = MovistarApi.defaultHeaders
        )

        val loginResponse =
            tryParseJson<MovistarLoginResponse>(response.text)
                ?: return false

        val accessToken = loginResponse.accessToken
            ?.takeIf { it.isNotBlank() }
            ?: return false

        MovistarSessionManager.setAccessToken(accessToken)

        return loadAccount()
    }

    suspend fun loadAccount(): Boolean {
        if (!MovistarSessionManager.isAuthenticated) {
            return false
        }

        val response = app.get(
            MovistarApi.AUTHENTICATE,
            headers = MovistarSessionManager.bearerHeaders()
        )

        val account =
            tryParseJson<MovistarAccountResponse>(response.text)
                ?: return false

        val offer = account.offers.firstOrNull()
            ?: return false

        val accountNumber = offer.accountNumber
            ?.takeIf { it.isNotBlank() }
            ?: return false

        val platform = offer.profile
            ?.takeIf { it.isNotBlank() }
            ?: return false

        MovistarSessionManager.setAccount(
            accountNumber = accountNumber,
            encodedUser = account.encodedUser.orEmpty(),
            platform = platform
        )

        return true
    }

    suspend fun loadProfiles(): List<MovistarProfile> {
        if (!MovistarSessionManager.isAuthenticated) {
            return emptyList()
        }

        val headers = MovistarSessionManager
            .bearerHeaders()
            .toMutableMap()

        if (MovistarSessionManager.sessionToken.isNotBlank()) {
            headers["X-HZId"] =
                MovistarSessionManager.sessionToken
        }

        val response = app.get(
            MovistarApi.PROFILES,
            headers = headers
        )

        return tryParseJson<MovistarProfileResponse>(
            response.text
        )?.items ?: emptyList()
    }

    fun logout() {
        MovistarSessionManager.clear()
    }
}
