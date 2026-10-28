package com.RodriguezRuiz.denunciasapp.ui.lista

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia

sealed class ListaUiState {
    data object Cargando : ListaUiState()

    data class Exito(
        val denuncias: List<Denuncia>,
        val hayMas: Boolean,
        val cargandoMas: Boolean = false,
        val avisoDemora: Boolean = false
    ) : ListaUiState()

    data class Error(
        val mensaje: String,
        val esInicial: Boolean = true
    ) : ListaUiState()
}
