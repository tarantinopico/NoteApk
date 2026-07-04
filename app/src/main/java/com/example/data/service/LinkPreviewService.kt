package com.example.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

data class LinkPreview(
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val domain: String
)

@Singleton
class LinkPreviewService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchPreview(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val domain = extractDomain(validUrl)

            val request = Request.Builder()
                .url(validUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val html = response.body?.string() ?: return@withContext null
                val document = Jsoup.parse(html)

                // Open Graph Tags
                var title = document.select("meta[property=og:title]").attr("content")
                if (title.isBlank()) {
                    title = document.title()
                }

                var description = document.select("meta[property=og:description]").attr("content")
                if (description.isBlank()) {
                    description = document.select("meta[name=description]").attr("content")
                }

                var imageUrl = document.select("meta[property=og:image]").attr("content")
                if (imageUrl.isBlank()) {
                    imageUrl = document.select("link[rel=apple-touch-icon]").attr("href")
                }
                if (imageUrl.isBlank()) {
                    imageUrl = document.select("link[rel=icon]").attr("href")
                }
                
                // Convert relative URL to absolute URL
                if (imageUrl.isNotBlank() && !imageUrl.startsWith("http")) {
                    imageUrl = if (imageUrl.startsWith("/")) {
                        "https://$domain$imageUrl"
                    } else {
                        "https://$domain/$imageUrl"
                    }
                }

                LinkPreview(
                    title = if (title.isNotBlank()) title else domain,
                    description = description.takeIf { it.isNotBlank() },
                    imageUrl = imageUrl.takeIf { it.isNotBlank() },
                    domain = domain
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(if (!url.startsWith("http")) "https://$url" else url)
            val host = uri.host ?: url
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            url
        }
    }
}
