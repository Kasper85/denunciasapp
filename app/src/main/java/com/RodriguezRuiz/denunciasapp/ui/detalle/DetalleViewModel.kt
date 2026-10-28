package com.RodriguezRuiz.denunciasapp.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.RodriguezRuiz.denunciasapp.data.repository.DenunciaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetalleViewModel(
    private val repository: DenunciaRepository = DenunciaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    private var idActual: Int? = null
    private var huboCambiosAcumulados: Boolean = false

    fun cargarDenuncia(id: Int) {
        idActual = id
        _uiState.value = DetalleUiState.Cargando

        viewModelScope.launch {
            val result = repository.obtenerDenunciaPorId(id)
            result.onSuccess { denuncia ->
                _uiState.value = DetalleUiState.Exito(
                    denuncia = denuncia,
                    huboCambios = huboCambiosAcumulados
                )
            }.onFailure { error ->
                _uiState.value = DetalleUiState.Error(
                    mensaje = error.message ?: "No se pudo cargar la denuncia"
                )
            }
        }
    }

    fun atenderDenuncia() {
        val currentState = _uiState.value
        if (currentState !is DetalleUiState.Exito || !currentState.denuncia.isPendiente || currentState.atendiendo) {
            return
        }

        _uiState.value = currentState.copy(atendiendo = true)

        viewModelScope.launch {
            val result = repository.atenderDenuncia(currentState.denuncia.id)
            result.onSuccess { atendida ->
                huboCambiosAcumulados = true
                _uiState.value = DetalleUiState.Exito(
                    denuncia = atendida,
                    atendiendo = false,
                    huboCambios = true
                )
            }.onFailure { _ ->
                _uiState.value = currentState.copy(atendiendo = false)
            }
        }
    }

    fun reintentar() {
        idActual?.let { cargarDenuncia(it) }
    }
}
