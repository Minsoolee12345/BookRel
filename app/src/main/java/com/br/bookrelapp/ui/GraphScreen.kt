// ui/GraphScreen.kt
package com.br.bookrelapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 그래프 화면. Pride and Prejudice(bookId=2) 기본 로드.
 */
@Composable
fun GraphScreen(vm: GraphViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var from by remember { mutableStateOf<Int?>(null) }
    var to by remember { mutableStateOf<Int?>(null) }

    // 최초 1회 기본 로드
    LaunchedEffect(Unit) { vm.loadGraph(bookId = 2) }

    val ui = vm.uiState.collectAsState().value

    // 하단 목록 검색어
    var query by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BookRel 테스트", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = from?.toString() ?: "",
                onValueChange = { from = it.filter { ch -> ch.isDigit() }.toIntOrNull() },
                modifier = Modifier.weight(1f),
                label = { Text("fromChapter") },
                singleLine = true
            )
            OutlinedTextField(
                value = to?.toString() ?: "",
                onValueChange = { to = it.filter { ch -> ch.isDigit() }.toIntOrNull() },
                modifier = Modifier.weight(1f),
                label = { Text("toChapter") },
                singleLine = true
            )
        }

        Button(onClick = { vm.loadGraph(2, from, to) }, enabled = !ui.loading) {
            Text("불러오기")
        }

        when {
            ui.loading -> CircularProgressIndicator()
            ui.error != null -> Text("에러: ${ui.error}")
            ui.data != null -> {
                val n = ui.data.nodes.size
                val e = ui.data.edges.size

                // 기존 요약은 그대로
                Text("노드: $n, 엣지: $e", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // ▼ 추가: 이름 목록(범위 필터 결과에서 추출)
                val names = remember(ui.data, query) {
                    ui.data.nodes
                        .mapNotNull { it.name?.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                        .let { list -> if (query.isBlank()) list else list.filter { it.contains(query, true) } }
                }

                // 간단 검색 + 리스트
                Card(Modifier.fillMaxWidth().weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "등장 인물 (${names.size}명)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                label = { Text("이름 검색") },
                                modifier = Modifier.widthIn(min = 160.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(names) { idx, name ->
                                Text("${idx + 1}. $name", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            else -> Text("대기 중…")
        }
    }
}