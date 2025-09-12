// data/api/RetrofitProvider.kt
package com.br.bookrelapp.data.api

import com.br.bookrelapp.data.dto.GraphResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitProvider {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // ✅ Moshi에 Kotlin 어댑터 등록
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: BookRelApi = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")   // 에뮬레이터 → 로컬 서버
        .addConverterFactory(MoshiConverterFactory.create(moshi)) // ✅
        .client(client)
        .build()
        .create(BookRelApi::class.java)
}