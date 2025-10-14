// app/src/main/java/com/br/bookrelapp/ui/GraphScreen.kt
package com.br.bookrelapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(vm: GraphViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    // 장 범위 상태
    var from by remember { mutableStateOf<Int?>(null) }
    var to by remember { mutableStateOf<Int?>(null) }

    // URL 입력
    var url by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // txt 파일 선택 → ingestTextAuto
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use(BufferedReader::readText)
                } ?: ""
                if (text.isNotBlank()) vm.ingestTextAuto(text)
            }
        }
    }

    // 최초 로드(초기 bookId가 있으면 불러옴)
    LaunchedEffect(Unit) { vm.loadGraph() }
    val ui = vm.uiState.collectAsState().value

    // --------- 필터 상태 ---------
    var showGood by remember { mutableStateOf(true) }
    var showNeutral by remember { mutableStateOf(true) }
    var showBad by remember { mutableStateOf(true) }
    var minWeightNorm by remember { mutableStateOf(0.2f) }   // 약한 엣지 제거
    var topNEdges by remember { mutableStateOf(100f) }       // 상위 N개 엣지만
    var labelsEnabled by remember { mutableStateOf(true) }
    var filterExpanded by remember { mutableStateOf(false) }

    // --------- 검색 상태 ---------
    var query by remember { mutableStateOf("") }

    // “오만과 편견” 주요 인물(화이트리스트)
    val ppMain = remember {
        listOf(
            "Elizabeth Bennet","Fitzwilliam Darcy","Mr. Darcy","Jane Bennet",
            "Charles Bingley","Mr. Bingley","Mr. Bennet","Mrs. Bennet",
            "Lydia Bennet","Mary Bennet","Kitty Bennet",
            "George Wickham","Mr. Wickham","William Collins","Mr. Collins",
            "Charlotte Lucas","Caroline Bingley","Lady Catherine de Bourgh",
            "Georgiana Darcy","Mr. Gardiner","Mrs. Gardiner"
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filterExpanded = !filterExpanded },
                text = { Text("필터") },
                icon = { Icon(Icons.Filled.Menu, contentDescription = null) }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("BookRel 테스트", style = MaterialTheme.typography.titleLarge)

            // URL 입력
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL 입력 (.txt.utf-8)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // URL 로드 / txt 업로드
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.ingestUrlAuto(url) },
                    enabled = url.isNotBlank() && !ui.loading
                ) { Text("URL 로드") }

                Button(
                    onClick = { launcher.launch(arrayOf("text/plain")) },
                    enabled = !ui.loading
                ) { Text("txt 파일 업로드") }
            }

            // 장 범위 입력
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

            // 불러오기 + 주요 인물 모드 + 범례
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { vm.loadGraph(from = from, to = to) },
                    enabled = !ui.loading && vm.currentBookId != null
                ) { Text("불러오기") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = vm.mainOnly,
                        onCheckedChange = {
                            vm.mainOnly = it
                            vm.loadGraph(from = from, to = to)
                        },
                        enabled = vm.currentBookId != null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("주요 인물만")
                }
                GraphLegend(Modifier.padding(start = 8.dp))
            }

            // 화이트리스트 적용(선택)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { vm.applyWhitelist(names = ppMain) },
                    enabled = !ui.loading && vm.currentBookId != null
                ) { Text("화이트리스트 적용") }

                Text(
                    "노드/엣지가 많으면 먼저 적용하고 '주요 인물만'으로 보자.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 필터 패널(토글)
            if (filterExpanded) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("보기 옵션", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(
                                selected = showGood,
                                onClick = { showGood = !showGood },
                                label = { Text("좋음") }
                            )
                            FilterChip(
                                selected = showNeutral,
                                onClick = { showNeutral = !showNeutral },
                                label = { Text("중립") }
                            )
                            FilterChip(
                                selected = showBad,
                                onClick = { showBad = !showBad },
                                label = { Text("나쁨") }
                            )
                            FilterChip(
                                selected = labelsEnabled,
                                onClick = { labelsEnabled = !labelsEnabled },
                                label = { Text("라벨") }
                            )
                        }
                        Text("최소 강도(정규화): ${"%.2f".format(minWeightNorm)}")
                        Slider(
                            value = minWeightNorm,
                            onValueChange = { minWeightNorm = it },
                            valueRange = 0f..1f
                        )
                        Text("상위 엣지 개수: ${topNEdges.toInt()}")
                        Slider(
                            value = topNEdges,
                            onValueChange = { topNEdges = it },
                            valueRange = 0f..500f
                        )
                    }
                }
            }

            // 본문
            when {
                ui.loading -> CircularProgressIndicator()
                ui.error != null -> Text("에러: ${ui.error}")
                ui.data != null -> {
                    val n = ui.data.nodes.size
                    val e = ui.data.edges.size
                    Text(
                        "노드: $n, 엣지: $e",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    GraphCanvas(
                        data = ui.data,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(440.dp),
                        filters = GraphFilters(
                            showGood = showGood,
                            showNeutral = showNeutral,
                            showBad = showBad,
                            minWeightNorm = minWeightNorm,
                            topNEdges = topNEdges.toInt(),
                            labelsEnabled = labelsEnabled
                        )
                    )

                    var names by remember(ui.data) {
                        mutableStateOf(
                            ui.data.nodes.mapNotNull { it.name?.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct()
                                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                        )
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            names = ui.data.nodes.mapNotNull { n -> n.name?.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct()
                                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                                .let { list -> if (query.isBlank()) list else list.filter { nm -> nm.contains(query, true) } }
                        },
                        singleLine = true,
                        label = { Text("이름 검색") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(Modifier.fillMaxWidth().weight(1f)) {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(names) { idx, name ->
                                Text(
                                    "${idx + 1}. $name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
                else -> Text("대기 중… URL을 로드하거나 txt를 업로드하세요.")
            }
        }
    }
}
