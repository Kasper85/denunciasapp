package com.RodriguezRuiz.denunciasapp.ui.registro

sealed class RegistroUiState {
    data object Idle : RegistroUiState()
    data object Cargando : RegistroUiState()
    data class ErrorValidacion(val mensaje: String) : RegistroUiState()
    data class ErrorServidor(val mensaje: String) : RegistroUiState()
    data class Exito(val idDenuncia: Int) : RegistroUiState()
}
