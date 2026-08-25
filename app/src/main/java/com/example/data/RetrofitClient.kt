package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun getApiService(baseUrl: String): ApiService {
        // Sanitize the Google Apps Script Web App URL to make it compatible with Retrofit base URL rules
        var cleanUrl = baseUrl.trim()
        if (cleanUrl.endsWith("/exec")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 5)
        } else if (cleanUrl.endsWith("/exec/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 6)
        }
        val url = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"
        
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
