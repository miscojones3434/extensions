package com.patriarca.movistarplus

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.nicehttp.RequestBodyTypes
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

object MovistarDeviceClient {

    var lastError: String = ""
        private set

    suspend fun prepareDevice(): Boolean {
        lastError = ""

        if (!MovistarSessionManager.isAuthenticated) {
            lastError = "DISPOSITIVO: no autenticado"
            return false
        }

        if (MovistarSessionManager.accountNumber.isBlank()) {
            lastError = "DISPOSITIVO: accountNumber vacío"
            return false
        }

        if (MovistarSessionManager.deviceId.isBlank()) {
            val deviceId = requestDeviceId()
                ?: return false

            MovistarSessionManager.setDevice(deviceId)
        }

        if (!registerDevice()) {
            return false
        }

        return initializeSession()
    }

    private suspend fun requestDeviceId(): String? {
        val response = app.post(
            MovistarApi.requestDevice(
                MovistarSessionManager.accountNumber
            ),
            headers = MovistarSessionManager
                .bearerHeaders() + mapOf(
                    "Content-Type" to "application/json"
                )
        )

        if (response.code !in 200..299) {
            lastError = "DEVICE ID HTTP ${response.code}"
            return null
        }

        return response.text
            .trim()
            .trim('"')
            .takeIf { it.isNotBlank() }
            ?: run {
                lastError = "DEVICE ID: respuesta vacía"
                null
            }
    }

    private suspend fun registerDevice(): Boolean {
        val deviceId = MovistarSessionManager.deviceId

        if (deviceId.isBlank()) {
            lastError = "REGISTRO: deviceId vacío"
            return false
        }

        val headers = MovistarSessionManager
            .bearerHeaders()
            .toMutableMap()

        headers["Content-Type"] = "application/json"
        headers["x-movistarplus-deviceid"] = deviceId

        val response = app.post(
            MovistarApi.registerDevice(
                accountNumber = MovistarSessionManager.accountNumber,
                deviceId = deviceId
            ),
            headers = headers
        )

        if (response.code !in 200..299) {
            lastError = "REGISTRO HTTP ${response.code}"
            return false
        }

        return true
    }

    suspend fun initializeSession(): Boolean {
        if (!MovistarSessionManager.isInitialized) {
            lastError = "INITDATA: sesión incompleta"
            return false
        }

        val requestJson = """
            {
              "accountNumber":"${MovistarSessionManager.accountNumber}",
              "userProfile":"${MovistarSessionManager.profileId}",
              "streamMiscellanea":"HTTPS",
              "deviceType":"${MovistarApi.DEVICE_CODE}",
              "deviceManufacturerProduct":"${MovistarApi.DEVICE_MANUFACTURER}",
              "streamDRM":"Widevine",
              "streamFormat":"DASH"
            }
        """.trimIndent()

        val requestBody = requestJson.toRequestBody(
            RequestBodyTypes.JSON.toMediaTypeOrNull()
        )

        val headers = MovistarSessionManager
            .bearerHeaders()
            .toMutableMap()

        headers["Content-Type"] = "application/json"
        headers["X-Movistarplus-Deviceid"] =
            MovistarSessionManager.deviceId

        val response = app.post(
            MovistarApi.initData(
                MovistarSessionManager.deviceId
            ),
            requestBody = requestBody,
            headers = headers
        )

        if (response.code !in 200..299) {
            lastError = "INITDATA HTTP ${response.code}"
            return false
        }

        val data =
            tryParseJson<MovistarInitDataResponse>(
                response.text
            )

        if (data == null) {
            lastError = "INITDATA: respuesta no válida"
            return false
        }

        val accessToken = data.accessToken
            ?.takeIf { it.isNotBlank() }

        if (accessToken == null) {
            lastError = "INITDATA: accessToken ausente"
            return false
        }

        MovistarSessionManager.setPlaybackSession(
            accessToken = accessToken,
            sessionToken = data.sessionToken.orEmpty(),
            sspToken = data.sspToken.orEmpty(),
            pid = data.pid.orEmpty(),
            demarcation = data.demarcation ?: 0,
            subscription = data.subscription.orEmpty(),
            distilledTvRights =
                data.distilledTvRights.orEmpty(),
            linearSubscription =
                data.linearSubscription
                    .orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
            vodSubscription =
                data.vodSubscription
                    .orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
        )

        return true
    }
}
