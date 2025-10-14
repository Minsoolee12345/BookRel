// app/src/main/java/com/br/bookrelapp/data/repository/GraphRepository.kt
package com.br.bookrelapp.data.repository

import com.br.bookrelapp.data.api.*
import com.br.bookrelapp.data.dto.GraphResponse

class GraphRepository(private val api: BookRelApi) {

    suspend fun getGraph(bookId: Long, from: Int?, to: Int?, mainOnly: Boolean?): GraphResponse =
        api.getGraph(bookId, from, to, mainOnly)

    suspend fun seed(): Map<String, String> = api.seed()

    suspend fun snapshot(bookId: Long, progress: Double, total: Int, window: Int?): GraphResponse =
        api.snapshot(bookId, progress, total, window)

    // 기존 명시 bookId
    suspend fun ingestUrl(bookId: Int, url: String): GraphResponse =
        api.ingestUrl(IngestUrlRequest(bookId, url))
    suspend fun ingestText(bookId: Int, text: String): GraphResponse =
        api.ingestText(IngestTextRequest(bookId, text))

    // 신규 auto
    suspend fun ingestUrlAuto(url: String): AutoIngestResponse =
        api.ingestUrlAuto(IngestUrlOnly(url))
    suspend fun ingestTextAuto(text: String): AutoIngestResponse =
        api.ingestTextAuto(IngestTextOnly(text))

    suspend fun markMain(bookId: Long, names: List<String>, exclusive: Boolean = true): Map<String, Any> =
        api.markMain(MarkMainRequest(bookId, names, exclusive))
}
