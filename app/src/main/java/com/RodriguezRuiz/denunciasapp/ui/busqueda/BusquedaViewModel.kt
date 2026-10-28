package com.RodriguezRuiz.denunciasapp.ui.busqueda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.RodriguezRuiz.denunciasapp.data.repository.DenunciaRepository
import com.RodriguezRuiz.denunciasapp.util.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BusquedaViewModel(
    private val repository: DenunciaRepository = DenunciaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<BusquedaUiState>(BusquedaUiState.Idle)
    val uiState: StateFlow<BusquedaUiState> = _uiState.asStateFlow()

    private var ultimoDniBuscado: String? = null

    fun buscar(dni: String) {
        val trimmedDni = dni.trim()
        if (!Validadores.esDniValido(trimmedDni)) {
            _uiState.value = BusquedaUiState.ErrorValidacion("El DNI debe tener exactamente 8 dígitos")
            return
        }

        ultimoDniBuscado = trimmedDni
        _uiState.value = BusquedaUiState.Cargando

        viewModelScope.launch {
            val result = repository.listarPorCiudadano(trimmedDni)
            result.onSuccess { lista ->
                if (lista.isEmpty()) {
                    _uiState.value = BusquedaUiState.Vacio
                } else {
                    _uiState.value = BusquedaUiState.Exito(lista)
                }
            }.onFailure { error ->
                _uiState.value = BusquedaUiState.ErrorServidor(
                    error.message ?: "Error al buscar las denuncias"
                )
            }
        }
    }

    fun reintentar() {
        ultimoDniBuscado?.let { buscar(it) }
    }
}
