// data/api/IngestRequests.kt
package com.br.bookrelapp.data.api

data class IngestUrlRequest(
    val bookId: Int,
    val url: String
)

data class IngestTextRequest(
    val bookId: Int,
    val text: String
)