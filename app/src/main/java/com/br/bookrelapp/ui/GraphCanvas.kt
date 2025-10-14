package com.br.bookrelapp.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.br.bookrelapp.data.dto.GraphResponse
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------- 필터 모델 (여기에 둬야 GraphScreen 에서도 바로 사용 가능) ----------
data class GraphFilters(
    val showGood: Boolean = true,
    val showNeutral: Boolean = true,
    val showBad: Boolean = true,
    val minWeightNorm: Float = 0.2f, // 0~1 정규화 기준 최소 가중치
    val topNEdges: Int = 100,        // 상위 N개 엣지만
    val labelsEnabled: Boolean = true
)

// ---------- 유틸 ----------
private fun typeToSentiment(type: String?): String = when (type?.uppercase()) {
    "ALLY", "FRIEND", "GOOD" -> "GOOD"
    "ENEMY", "RIVAL", "BAD"  -> "BAD"
    else                     -> "NEUTRAL"
}

private fun colorForSentiment(s: String): Color = when (s) {
    "GOOD"   -> Color(0xFF2E7D32) // 초록
    "BAD"    -> Color(0xFFC62828) // 빨강
    else     -> Color(0xFF616161) // 회색
}

// ---------- 캔버스 ----------
@Composable
fun GraphCanvas(
    data: GraphResponse,
    modifier: Modifier = Modifier,
    nodeRadius: Dp = 18.dp,
    filters: GraphFilters = GraphFilters()   // ✅ filters 추가
) {
    val nodes = data.nodes
    if (nodes.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("노드가 없습니다.") }
        return
    }

    // 1) 엣지 필터링: 정서(=type 매핑) + 가중치(정규화) + 상위 N
    val edgesWithSent = data.edges.map { e ->
        Triple(e, typeToSentiment(e.type), (e.weight ?: 0.0))
    }

    // 정규화용 max
    val maxWAll = edgesWithSent.maxOfOrNull { it.third }?.takeIf { it > 0.0 } ?: 1.0

    val filteredEdges = edgesWithSent
        // sentiment 필터
        .filter { (_, s, _) ->
            (s == "GOOD" && filters.showGood) ||
                    (s == "BAD" && filters.showBad) ||
                    (s == "NEUTRAL" && filters.showNeutral)
        }
        // 최소 가중치(정규화) 필터
        .filter { (_, _, w) -> (w / maxWAll) >= filters.minWeightNorm }
        // 강한 엣지 우선
        .sortedByDescending { it.third }
        // 상위 N 제한
        .take(filters.topNEdges)

    // 2) 실제 그리기
    Card(modifier = modifier) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
            Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val R = (min(w, h) / 2f) * 0.78f
                val n = nodes.size
                val nodePx = nodeRadius.toPx()

                // id → index
                val idToIdx = nodes.mapIndexed { idx, n -> n.id to idx }.toMap()

                // 원형 좌표
                val pos = Array(n) { Offset.Zero }
                for (i in 0 until n) {
                    val theta = (2 * PI * i / n).toFloat()
                    pos[i] = Offset(cx + R * cos(theta), cy + R * sin(theta))
                }

                // 두께 정규화 기준: 필터링된 엣지 중 max
                val maxW = filteredEdges.maxOfOrNull { it.third }?.takeIf { it > 0.0 } ?: 1.0
                fun edgeWidth(wVal: Double): Float {
                    val norm = (wVal / maxW).toFloat()
                    return (1.5f + (norm * 4.5f)) // 1.5~6.0
                }

                // 엣지
                for ((e, s, wVal) in filteredEdges) {
                    val a = idToIdx[e.src] ?: continue
                    val b = idToIdx[e.dst] ?: continue
                    val p1 = pos[a]
                    val p2 = pos[b]
                    drawLine(
                        color = colorForSentiment(s),
                        start = p1,
                        end = p2,
                        strokeWidth = edgeWidth(wVal)
                    )
                }

                // 노드 (원)
                nodes.forEachIndexed { idx, _ ->
                    val p = pos[idx]
                    drawCircle(
                        color = Color(0xFF374151),
                        radius = nodePx,
                        center = p,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = nodePx - 2f,
                        center = p
                    )
                }

                // 라벨(이름)
                if (filters.labelsEnabled) {
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 28f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    }
                    nodes.forEachIndexed { idx, node ->
                        val name = node.name ?: ""
                        if (name.isBlank()) return@forEachIndexed
                        val p = pos[idx]
                        val textWidth = paint.measureText(name)
                        val x = p.x - textWidth / 2f
                        val y = p.y + (paint.textSize / 3f)
                        drawContext.canvas.nativeCanvas.drawText(name, x, y, paint)
                    }
                }
            }
        }
    }
}

@Composable
fun GraphLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(Color(0xFF2E7D32), "좋음(GOOD)")
        LegendItem(Color(0xFF616161), "중립(NEUTRAL)")
        LegendItem(Color(0xFFC62828), "나쁨(BAD)")
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(14.dp).background(color))
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
