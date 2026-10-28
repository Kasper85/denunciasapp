package com.RodriguezRuiz.denunciasapp.ui.busqueda

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia

sealed class BusquedaUiState {
    data object Idle : BusquedaUiState()
    data object Cargando : BusquedaUiState()
    data class Exito(val denuncias: List<Denuncia>) : BusquedaUiState()
    data object Vacio : BusquedaUiState()
    data class ErrorValidacion(val mensaje: String) : BusquedaUiState()
    data class ErrorServidor(val mensaje: String) : BusquedaUiState()
}
