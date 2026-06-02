package com.j.m3play.newpipe

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as ExtractorRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

class M3Downloader(
    private val client: OkHttpClient
) : Downloader() {

    override fun execute(request: ExtractorRequest): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = Request.Builder()
            .url(url)
            .method(httpMethod, if (dataToSend != null) {
                dataToSend.toRequestBody(null)
            } else if (httpMethod == "POST" || httpMethod == "PUT") {
                ByteArray(0).toRequestBody(null)
            } else {
                null
            })

        headers.forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        if (!headers.containsKey("User-Agent")) {
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            requestBuilder.addHeader("User-Agent", userAgent)
        }
        
        if (!headers.containsKey("Accept-Language")) {
            requestBuilder.addHeader("Accept-Language", "en-US,en;q=0.9")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            response.use {
                if (response.code == 429) {
                    throw ReCaptchaException("Rate limited", url)
                }

                val body = response.body
                val responseBodyString = if (body != null) {
                    val limit = 10 * 1024 * 1024L 
                    val contentLength = body.contentLength()
                    
                    if (contentLength > limit) "" 
                    else {
                        try {
                            val source = body.source()
                            source.request(limit)
                            if (source.buffer.size > limit) "" else body.string()
                        } catch (e: Exception) { "" }
                    }
                } else ""

                val responseHeaders = mutableMapOf<String, MutableList<String>>()
                response.headers.forEach { (name, value) ->
                    responseHeaders.getOrPut(name) { mutableListOf() }.add(value)
                }

                return Response(response.code, response.message, responseHeaders, responseBodyString, url)
            }
        } catch (e: IOException) {
            throw e
        }
    }
}
