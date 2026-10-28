package com.RodriguezRuiz.denunciasapp.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.RodriguezRuiz.denunciasapp.data.model.DenunciaRequest
import com.RodriguezRuiz.denunciasapp.data.repository.DenunciaRepository
import com.RodriguezRuiz.denunciasapp.util.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistroViewModel(
    private val repository: DenunciaRepository = DenunciaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun registrar(
        dni: String,
        tipo: String,
        direccion: String,
        descripcion: String
    ) {
        val errorValidacion = Validadores.validarRegistro(
            dni = dni,
            tipo = tipo,
            direccion = direccion,
            descripcion = descripcion
        )

        if (errorValidacion != null) {
            _uiState.value = RegistroUiState.ErrorValidacion(errorValidacion)
            return
        }

        _uiState.value = RegistroUiState.Cargando

        val request = DenunciaRequest(
            dni = dni.trim(),
            tipo = tipo.trim().uppercase(),
            direccion = direccion.trim(),
            descripcion = descripcion.trim()
        )

        viewModelScope.launch {
            val result = repository.registrarDenuncia(request)
            result.onSuccess { denuncia ->
                _uiState.value = RegistroUiState.Exito(denuncia.id)
            }.onFailure { error ->
                _uiState.value = RegistroUiState.ErrorServidor(
                    error.message ?: "Error al registrar la denuncia"
                )
            }
        }
    }

    fun reiniciarEstado() {
        _uiState.value = RegistroUiState.Idle
    }
}
