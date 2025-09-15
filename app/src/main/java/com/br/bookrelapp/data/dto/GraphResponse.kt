// data/dto/GraphResponse.kt
package com.br.bookrelapp.data.dto

data class GraphResponse(
    val nodes: List<Node>,
    val edges: List<Edge>
) {
    data class Node(
        val id: String,
        val name: String?   // ← String? 로 변경 (한 줄)
    )
    data class Edge(
        val src: String,
        val dst: String,
        val type: String,
        val weight: Double?,
        val fromChapter: Int?,
        val toChapter: Int?
    )
}
