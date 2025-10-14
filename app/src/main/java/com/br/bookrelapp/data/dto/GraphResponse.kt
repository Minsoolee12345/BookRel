package com.br.bookrelapp.data.dto

data class GraphResponse(
    val nodes: List<Node>,
    val edges: List<Edge>
) {
    data class Node(
        val id: String,
        val name: String?   // 서버에서 null일 수도 있으므로 그대로 유지
    )
    data class Edge(
        val src: String,
        val dst: String,
        val type: String,
        val weight: Double?,
        val fromChapter: Int?,
        val toChapter: Int?,
        val sentiment: String?    // ✅ "GOOD" | "NEUTRAL" | "BAD"
    )
}