// data/api/BookRelApi.kt
package com.br.bookrelapp.data.api

import com.br.bookrelapp.data.dto.GraphResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookRelApi {
    @GET("api/graph/{bookId}")
    suspend fun getGraph(
        @Path("bookId") bookId: Long,
        @Query("fromChapter") from: Int? = null,
        @Query("toChapter") to: Int? = null
    ): GraphResponse

    @POST("api/graph/seed")
    suspend fun seed(): Map<String, String>

    @GET("api/graph/snapshot")
    suspend fun snapshot(
        @Query("bookId") bookId: Long,
        @Query("progress") progress: Double,
        @Query("totalChapters") totalChapters: Int,
        @Query("window") window: Int? = null
    ): GraphResponse

    @POST("ingest/url")
    suspend fun ingestUrl(@Body request: IngestUrlRequest): GraphResponse

    @POST("ingest/text")
    suspend fun ingestText(@Body request: IngestTextRequest): GraphResponse
}