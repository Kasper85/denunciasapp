package com.RodriguezRuiz.denunciasapp.ui.detalle

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia

sealed class DetalleUiState {
    data object Cargando : DetalleUiState()

    data class Exito(
        val denuncia: Denuncia,
        val atendiendo: Boolean = false,
        val huboCambios: Boolean = false
    ) : DetalleUiState()

    data class Error(
        val mensaje: String
    ) : DetalleUiState()
}
