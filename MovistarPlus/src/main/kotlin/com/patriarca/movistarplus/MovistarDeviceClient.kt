package com.patriarca.movistarplus

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.nicehttp.RequestBodyTypes
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

object MovistarDeviceClient {

    suspend fun prepareDevice(): Boolean {
        if (!MovistarSessionManager.isAuthenticated) {
            return false
        }

        if (MovistarSessionManager.accountNumber.isBlank()) {
            return false
        }

        if (MovistarSessionManager.deviceId.isBlank()) {
            val deviceId = requestDeviceId()
                ?: return false

            MovistarSessionManager.setDevice(deviceId)
        }

        registerDevice()

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

        return response.text
            .trim()
            .trim('"')
            .takeIf { it.isNotBlank() }
    }

    private suspend fun registerDevice(): Boolean {
        val deviceId = MovistarSessionManager.deviceId

        if (deviceId.isBlank()) {
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

        return response.code in 200..299
    }

    suspend fun initializeSession(): Boolean {
        if (!MovistarSessionManager.isInitialized) {
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

        val data =
            tryParseJson<MovistarInitDataResponse>(
                response.text
            ) ?: return false

        val accessToken = data.accessToken
            ?.takeIf { it.isNotBlank() }
            ?: return false

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
