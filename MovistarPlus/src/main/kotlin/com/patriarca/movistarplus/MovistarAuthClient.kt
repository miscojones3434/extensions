package com.patriarca.movistarplus

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

object MovistarAuthClient {

    var lastError: String = ""
        private set

    suspend fun login(
        username: String,
        password: String
    ): Boolean {
        lastError = ""

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

        if (response.code !in 200..299) {
            lastError = "LOGIN HTTP ${response.code}"
            return false
        }

        val loginResponse =
            tryParseJson<MovistarLoginResponse>(response.text)

        if (loginResponse == null) {
            lastError = "LOGIN: respuesta no válida"
            return false
        }

        val accessToken = loginResponse.accessToken
            ?.takeIf { it.isNotBlank() }

        if (accessToken == null) {
            lastError = "LOGIN: access_token ausente"
            return false
        }

        MovistarSessionManager.setAccessToken(accessToken)

        return loadAccount()
    }

    suspend fun loadAccount(): Boolean {
        if (!MovistarSessionManager.isAuthenticated) {
            lastError = "CUENTA: sesión no autenticada"
            return false
        }

        val response = app.get(
            MovistarApi.AUTHENTICATE,
            headers = MovistarSessionManager.bearerHeaders() + mapOf(
                "Content-Type" to "application/x-www-form-urlencoded"
            )
        )

        if (response.code !in 200..299) {
            lastError = "CUENTA HTTP ${response.code}"
            return false
        }

        val account =
            tryParseJson<MovistarAccountResponse>(response.text)

        if (account == null) {
            lastError = "CUENTA: respuesta no válida"
            return false
        }

        val offer = account.offers.firstOrNull()

        if (offer == null) {
            lastError = "CUENTA: sin ofertas"
            return false
        }

        val accountNumber = offer.accountNumber
            ?.takeIf { it.isNotBlank() }

        if (accountNumber == null) {
            lastError = "CUENTA: accountNumber ausente"
            return false
        }

        val platform = offer.profile
            ?.takeIf { it.isNotBlank() }

        if (platform == null) {
            lastError = "CUENTA: perfil ausente"
            return false
        }

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
        lastError = ""
        MovistarSessionManager.clear()
    }
}
