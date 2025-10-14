// app/src/main/java/com/br/bookrelapp/ui/GraphViewModel.kt
package com.br.bookrelapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.bookrelapp.data.api.RetrofitProvider
import com.br.bookrelapp.data.dto.GraphResponse
import com.br.bookrelapp.data.repository.GraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GraphUiState(
    val loading: Boolean = false,
    val data: GraphResponse? = null,
    val error: String? = null
)

class GraphViewModel : ViewModel() {
    private val repo = GraphRepository(RetrofitProvider.api)

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState

    var mainOnly: Boolean = false
    var currentBookId: Long? = 2L  // 초기에는 2L로(없어도 됨)

    fun loadGraph(bookId: Long? = currentBookId, from: Int? = null, to: Int? = null) {
        val id = bookId ?: return
        viewModelScope.launch {
            _uiState.value = GraphUiState(loading = true)
            runCatching { repo.getGraph(id, from, to, mainOnly) }
                .onSuccess { _uiState.value = GraphUiState(data = it) }
                .onFailure { _uiState.value = GraphUiState(error = it.message ?: "unknown") }
        }
    }

    // 기존: 명시 bookId 사용 (원하면 계속 사용 가능)
    fun ingestUrl(bookId: Int, url: String) {
        viewModelScope.launch {
            _uiState.value = GraphUiState(loading = true)
            runCatching { repo.ingestUrl(bookId, url) }
                .onSuccess { data ->
                    currentBookId = bookId.toLong()
                    _uiState.value = GraphUiState(data = data)
                }
                .onFailure { ex -> _uiState.value = GraphUiState(error = ex.message) }
        }
    }

    fun ingestText(bookId: Int, text: String) {
        viewModelScope.launch {
            _uiState.value = GraphUiState(loading = true)
            runCatching { repo.ingestText(bookId, text) }
                .onSuccess { data ->
                    currentBookId = bookId.toLong()
                    _uiState.value = GraphUiState(data = data)
                }
                .onFailure { ex -> _uiState.value = GraphUiState(error = ex.message) }
        }
    }

    // 신규: 자동 bookId 버전
    fun ingestUrlAuto(url: String) {
        viewModelScope.launch {
            _uiState.value = GraphUiState(loading = true)
            runCatching { repo.ingestUrlAuto(url) }
                .onSuccess { resp ->
                    currentBookId = resp.bookId
                    _uiState.value = GraphUiState(data = resp.graph)
                }
                .onFailure { ex -> _uiState.value = GraphUiState(error = ex.message) }
        }
    }

    fun ingestTextAuto(text: String) {
        viewModelScope.launch {
            _uiState.value = GraphUiState(loading = true)
            runCatching { repo.ingestTextAuto(text) }
                .onSuccess { resp ->
                    currentBookId = resp.bookId
                    _uiState.value = GraphUiState(data = resp.graph)
                }
                .onFailure { ex -> _uiState.value = GraphUiState(error = ex.message) }
        }
    }

    /** 주요 인물 표시 후 즉시 재로딩 */
    fun applyWhitelist(bookId: Long? = currentBookId, names: List<String>, exclusive: Boolean = true) {
        val id = bookId ?: return
        viewModelScope.launch {
            runCatching { repo.markMain(id, names, exclusive) }
            loadGraph(id)
        }
    }
}
