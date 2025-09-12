package com.br.bookrelapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GraphScreen(vm: GraphViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var from by remember { mutableStateOf<Int?>(null) }
    var to by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { vm.loadGraph(bookId = 1) }

    val ui = vm.uiState.collectAsState().value

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("BookRel 테스트", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = from?.toString() ?: "",
                onValueChange = { from = it.toIntOrNull() },
                modifier = Modifier.weight(1f),
                label = { Text("fromChapter") }
            )
            OutlinedTextField(
                value = to?.toString() ?: "",
                onValueChange = { to = it.toIntOrNull() },
                modifier = Modifier.weight(1f),
                label = { Text("toChapter") }
            )
        }

        Button(onClick = { vm.loadGraph(1, from, to) }, enabled = !ui.loading) {
            Text("불러오기")
        }

        when {
            ui.loading -> CircularProgressIndicator()
            ui.error != null -> Text("에러: ${ui.error}")
            ui.data != null -> {
                val n = ui.data.nodes.size
                val e = ui.data.edges.size
                Text("노드: $n, 엣지: $e")
                ui.data.nodes.firstOrNull()?.let { Text("첫 노드: ${it.name}") }
            }
            else -> Text("대기 중…")
        }
    }
}