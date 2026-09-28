package com.patriarca.movistarplus

object MovistarApi {

    const val WEB_URL = "https://ver.movistarplus.es"

    const val DEVICE_PLAYER = "amazon.tv"
    const val WEB_PLAYER = "webplayer"

    const val DEVICE_CODE = "SMARTTV_OTT"
    const val DEVICE_MANUFACTURER = "LG"

    const val TOKEN =
        "https://auth.dof6.com/auth/oauth2/token?deviceClass=$DEVICE_PLAYER"

    const val AUTHENTICATE =
        "https://auth.dof6.com/movistarplus/api/devices/$DEVICE_PLAYER/users/authenticate"

    const val PROFILES =
        "https://grmovistar.imagenio.telefonica.net/asfe/rest/users/profiles?state=0&isForKids=0"

    const val RECORDINGS =
        "https://perso.dof6.com/movistarplus/npvr/$DEVICE_PLAYER/users/{userId}/recordings"

    const val LICENSE_WIDEVINE =
        "https://wv-ottlic-f3.imagenio.telefonica.net/TFAESP/wvls/contentlicenseservice/v1/licenses"

    fun initData(deviceId: String): String =
        "https://clientservices.dof6.com/movistarplus/$DEVICE_PLAYER/sdp/mediaPlayers/$deviceId/initData?qspVersion=ssp&version=8&status=default"

    fun channels(
        profile: String,
        demarcation: Int
    ): String =
        "https://ottcache.dof6.com/movistarplus/$WEB_PLAYER/$profile/contents/channels" +
            "?mdrm=true" +
            "&tlsstream=true" +
            "&demarcation=$demarcation" +
            "&version=8"

    fun epg(
        profile: String,
        utcDateTime: String,
        duration: Int,
        channels: String,
        network: String,
        demarcation: Int
    ): String =
        "https://ottcache.dof6.com/movistarplus/$WEB_PLAYER/$profile/epg" +
            "?from=$utcDateTime" +
            "&span=$duration" +
            "&channel=$channels" +
            "&network=$network" +
            "&version=8" +
            "&mdrm=true" +
            "&tlsstream=true" +
            "&demarcation=$demarcation"

    fun browse(
        profile: String,
        sort: String,
        start: Int,
        end: Int,
        demarcation: Int
    ): String =
        "https://ottcache.dof6.com/movistarplus/$DEVICE_PLAYER/contents/browse" +
            "?profile=$profile" +
            "&sort=$sort" +
            "&version=8" +
            "&start=$start" +
            "&end=$end" +
            "&mdrm=true" +
            "&tlsstream=true" +
            "&demarcation=$demarcation"

    fun contentDetails(
        id: String,
        profile: String,
        mediaType: String,
        mode: String,
        catalog: String,
        channels: String,
        state: String,
        demarcation: Int,
        legacyBoxOffice: String
    ): String =
        "https://ottcache.dof6.com/movistarplus/$DEVICE_PLAYER/contents/$id/details" +
            "?profile=$profile" +
            "&mediaType=$mediaType" +
            "&version=8" +
            "&mode=$mode" +
            "&catalog=$catalog" +
            "&channels=$channels" +
            "&state=$state" +
            "&mdrm=true" +
            "&tlsstream=true" +
            "&demarcation=$demarcation" +
            "&legacyBoxOffice=$legacyBoxOffice"

    fun search(
        accountNumber: String,
        profile: String,
        query: String,
        rights: String,
        demarcation: Int
    ): String =
        "https://perso.dof6.com/movistarplus/$DEVICE_PLAYER/users/contents/search" +
            "?accountnumber=$accountNumber" +
            "&profile=$profile" +
            "&term=$query" +
            "&mode=VODRU7D" +
            "&showSeries=series" +
            "&distilledTvRights=$rights" +
            "&version=8" +
            "&mdrm=true" +
            "&tlsstream=true" +
            "&demarcation=$demarcation" +
            "&scope=DAZN"

    fun devices(accountNumber: String): String =
        "https://clientservices.dof6.com/movistarplus/accounts/$accountNumber/devices?qspVersion=ssp"

    fun registerDevice(
        accountNumber: String,
        deviceId: String
    ): String =
        "https://auth.dof6.com/movistarplus/$DEVICE_PLAYER/accounts/$accountNumber/devices/$deviceId?qspVersion=ssp"

    fun requestDevice(accountNumber: String): String =
        "https://auth.dof6.com/movistarplus/$DEVICE_PLAYER/accounts/$accountNumber/devices/?qspVersion=ssp"

    fun hzToken(deviceId: String): String =
        "https://clientservices.dof6.com/movistarplus/$DEVICE_PLAYER/mediaPlayers/$deviceId/hz-token"

    fun sspToken(
        accountNumber: String,
        deviceId: String
    ): String =
        "https://clientservices.dof6.com/movistarplus/$DEVICE_PLAYER/accounts/$accountNumber/ssp-token?mediaPlayerId=$deviceId"

    fun cdnToken(accountNumber: String): String =
        "https://idserver.dof6.com/$accountNumber/devices/$DEVICE_PLAYER/cdn/token/refresh"

    fun openStreamSession(
        pid: String,
        deviceId: String
    ): String =
        "https://alkasvaspub.imagenio.telefonica.net/asvas/ccs/$pid/$DEVICE_CODE/$deviceId/Session"

    fun closeStreamSession(
        pid: String,
        deviceId: String,
        sessionId: String
    ): String =
        "https://alkasvaspub.imagenio.telefonica.net/asvas/ccs/$pid/$DEVICE_CODE/$deviceId/Session/$sessionId"

    val defaultHeaders: Map<String, String>
        get() = mapOf(
            "Accept" to "application/json, text/javascript, */*; q=0.01",
            "Accept-Language" to "es-ES,es;q=0.9",
            "Origin" to WEB_URL,
            "Referer" to "$WEB_URL/"
        )
}
