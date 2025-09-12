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

    fun loadGraph(bookId: Long, from: Int? = null, to: Int? = null) {
        viewModelScope.launch {
            _uiState.value = GraphUiState(loading = true)
            runCatching { repo.getGraph(bookId, from, to) }
                .onSuccess { _uiState.value = GraphUiState(data = it) }
                .onFailure { _uiState.value = GraphUiState(error = it.message ?: "unknown") }
        }
    }
}