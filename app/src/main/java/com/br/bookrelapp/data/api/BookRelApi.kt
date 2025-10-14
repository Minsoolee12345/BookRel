// app/src/main/java/com/br/bookrelapp/data/api/BookRelApi.kt
package com.br.bookrelapp.data.api

import com.br.bookrelapp.data.dto.GraphResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 요청/응답 바디들
import com.br.bookrelapp.data.api.IngestUrlRequest
import com.br.bookrelapp.data.api.IngestTextRequest
import com.br.bookrelapp.data.api.MarkMainRequest
import com.br.bookrelapp.data.api.IngestUrlOnly
import com.br.bookrelapp.data.api.IngestTextOnly
import com.br.bookrelapp.data.api.AutoIngestResponse

interface BookRelApi {

    @GET("api/graph/{bookId}")
    suspend fun getGraph(
        @Path("bookId") bookId: Long,
        @Query("fromChapter") from: Int? = null,
        @Query("toChapter") to: Int? = null,
        @Query("mainOnly") mainOnly: Boolean? = null
    ): GraphResponse

    @POST("api/graph/seed")
    suspend fun seed(): Map<String, String>

    @GET("api/graph/snapshot")
    suspend fun snapshot(
        @Query("bookId") bookId: Long,
        @Query("progress") progress: Double,
        @Query("totalChapters") totalChapters: Int,
        @Query("window") window: Int? = null,
        @Query("mainOnly") mainOnly: Boolean? = null
    ): GraphResponse

    // 기존 명시 bookId 버전(유지)
    @POST("api/nlp/ingestUrl")
    suspend fun ingestUrl(@Body request: IngestUrlRequest): GraphResponse

    @POST("api/nlp/ingestText")
    suspend fun ingestText(@Body request: IngestTextRequest): GraphResponse

    // 신규: bookId 자동 생성 버전
    @POST("api/nlp/ingestUrlAuto")
    suspend fun ingestUrlAuto(@Body request: IngestUrlOnly): AutoIngestResponse

    @POST("api/nlp/ingestTextAuto")
    suspend fun ingestTextAuto(@Body request: IngestTextOnly): AutoIngestResponse

    // 주요 인물 표시
    @POST("api/admin/markMain")
    suspend fun markMain(@Body body: MarkMainRequest): Map<String, Any>
}