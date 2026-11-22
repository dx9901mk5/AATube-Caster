package com.aayt.caster.data

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

class OkHttpNewPipeDownloader(
    client: OkHttpClient? = null
) : Downloader() {

    private val httpClient: OkHttpClient = client ?: OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = if (dataToSend != null) {
            dataToSend.toRequestBody(null)
        } else if (httpMethod == "POST" || httpMethod == "PUT") {
            ByteArray(0).toRequestBody(null)
        } else {
            null
        }

        val builder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .header("User-Agent", USER_AGENT)

        var hasCookieHeader = false
        for ((headerName, headerValueList) in headers) {
            if (headerName.equals("Cookie", ignoreCase = true)) {
                hasCookieHeader = true
            }
            builder.removeHeader(headerName)
            for (headerValue in headerValueList) {
                builder.addHeader(headerName, headerValue)
            }
        }

        if (!hasCookieHeader && url.contains("youtube.com")) {
            builder.addHeader("Cookie", CONSENT_COOKIE)
        }

        httpClient.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }
            val bodyString = response.body?.string()
            val latestUrl = response.request.url.toString()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                bodyString,
                latestUrl
            )
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val CONSENT_COOKIE = "SOCS=CAISEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg"
    }
}
