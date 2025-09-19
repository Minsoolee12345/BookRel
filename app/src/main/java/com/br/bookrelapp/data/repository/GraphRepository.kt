package com.br.bookrelapp.data.repository

import com.br.bookrelapp.data.api.BookRelApi
import com.br.bookrelapp.data.dto.GraphResponse
import com.br.bookrelapp.data.api.IngestUrlRequest
import com.br.bookrelapp.data.api.IngestTextRequest

class GraphRepository(private val api: BookRelApi) {
    suspend fun getGraph(bookId: Long, from: Int?, to: Int?): GraphResponse =
        api.getGraph(bookId, from, to)

    suspend fun seed(): Map<String, String> = api.seed()

    suspend fun snapshot(bookId: Long, progress: Double, total: Int, window: Int?): GraphResponse =
        api.snapshot(bookId, progress, total, window)

    suspend fun ingestUrl(bookId: Int, url: String): GraphResponse =
        api.ingestUrl(IngestUrlRequest(bookId, url))

    suspend fun ingestText(bookId: Int, text: String): GraphResponse =
        api.ingestText(IngestTextRequest(bookId, text))
}