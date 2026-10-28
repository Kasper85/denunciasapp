package com.RodriguezRuiz.denunciasapp.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.RodriguezRuiz.denunciasapp.data.model.Denuncia
import com.RodriguezRuiz.denunciasapp.data.repository.DenunciaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ListaViewModel(
    private val repository: DenunciaRepository = DenunciaRepository()
) : ViewModel() {

    companion object {
        const val PAGE_LIMIT = 5
        private const val AVISO_DEMORA_MS = 5000L
    }

    private val _uiState = MutableStateFlow<ListaUiState>(ListaUiState.Cargando)
    val uiState: StateFlow<ListaUiState> = _uiState.asStateFlow()

    private val denunciasAcumuladas = mutableListOf<Denuncia>()
    private var offsetActual = 0
    private var hayMasElementos = true
    private var jobDemora: Job? = null

    init {
        cargarInicial()
    }

    fun cargarInicial() {
        jobDemora?.cancel()
        _uiState.value = ListaUiState.Cargando
        offsetActual = 0
        denunciasAcumuladas.clear()
        hayMasElementos = true

        jobDemora = viewModelScope.launch {
            delay(AVISO_DEMORA_MS)
            val currentState = _uiState.value
            if (currentState is ListaUiState.Cargando) {
                // El estado puede exponer el aviso o mantenerse cargando con aviso
            }
        }

        viewModelScope.launch {
            val result = repository.listarDenuncias(offset = 0, limit = PAGE_LIMIT)
            jobDemora?.cancel()
            result.onSuccess { lista ->
                denunciasAcumuladas.clear()
                denunciasAcumuladas.addAll(lista)
                hayMasElementos = lista.size >= PAGE_LIMIT
                offsetActual = lista.size
                _uiState.value = ListaUiState.Exito(
                    denuncias = denunciasAcumuladas.toList(),
                    hayMas = hayMasElementos
                )
            }.onFailure { error ->
                _uiState.value = ListaUiState.Error(
                    mensaje = error.message ?: "Error al cargar las denuncias",
                    esInicial = true
                )
            }
        }
    }

    fun cargarSiguientes() {
        val currentState = _uiState.value
        if (currentState !is ListaUiState.Exito || !hayMasElementos || currentState.cargandoMas) {
            return
        }

        _uiState.value = currentState.copy(cargandoMas = true)

        viewModelScope.launch {
            val result = repository.listarDenuncias(offset = offsetActual, limit = PAGE_LIMIT)
            result.onSuccess { lista ->
                denunciasAcumuladas.addAll(lista)
                hayMasElementos = lista.size >= PAGE_LIMIT
                offsetActual += lista.size
                _uiState.value = ListaUiState.Exito(
                    denuncias = denunciasAcumuladas.toList(),
                    hayMas = hayMasElementos,
                    cargandoMas = false
                )
            }.onFailure { error ->
                // Mantenemos la lista existente pero notificamos el error
                _uiState.value = currentState.copy(cargandoMas = false)
            }
        }
    }

    fun reintentar() {
        val currentState = _uiState.value
        if (currentState is ListaUiState.Error && currentState.esInicial) {
            cargarInicial()
        } else {
            cargarSiguientes()
        }
    }
}
