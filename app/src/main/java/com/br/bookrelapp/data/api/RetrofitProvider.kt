// data/api/RetrofitProvider.kt
package com.br.bookrelapp.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        // ✅ 타임아웃 상향 (필요시 더 늘려도 됨)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS) // 전체 호출 제한 없음
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: BookRelApi = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")   // 에뮬레이터→호스트
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(BookRelApi::class.java)
}
