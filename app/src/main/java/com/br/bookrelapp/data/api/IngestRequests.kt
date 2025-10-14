// app/src/main/java/com/br/bookrelapp/data/api/IngestRequests.kt
package com.br.bookrelapp.data.api

import com.br.bookrelapp.data.dto.GraphResponse

// (기존) 명시 bookId 버전
data class IngestUrlRequest(
    val bookId: Int,
    val url: String
)
data class IngestTextRequest(
    val bookId: Int,
    val text: String
)

// (신규) bookId 자동 생성 버전
data class IngestUrlOnly(val url: String)
data class IngestTextOnly(val text: String)

data class AutoIngestResponse(
    val bookId: Long,
    val graph: GraphResponse
)

// 주요 인물 표시
data class MarkMainRequest(
    val bookId: Long,
    val names: List<String>,
    val exclusive: Boolean = true
)
