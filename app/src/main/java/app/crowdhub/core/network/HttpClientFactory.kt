package app.crowdhub.core.network

import app.crowdhub.BuildConfig
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClientFactory {
    val userAgent: String
        get() = "CrowdHub/${BuildConfig.VERSION_NAME} (Android)"

    fun create(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val req = chain.request()
                require(req.url.isHttps) { "HTTPS only: cleartext requests are blocked" }
                val tagged = req.newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
                chain.proceed(tagged)
            }
            .build()
    }
}
