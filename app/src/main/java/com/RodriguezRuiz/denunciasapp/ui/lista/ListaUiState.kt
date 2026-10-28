package com.RodriguezRuiz.denunciasapp.ui.lista

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia

sealed class ListaUiState {
    data object Cargando : ListaUiState()

    data class Exito(
        val denuncias: List<Denuncia>,
        val hayMas: Boolean,
        val cargandoMas: Boolean = false,
        val avisoDemora: Boolean = false,
        val filtroActual: FiltroEstado = FiltroEstado.TODAS
    ) : ListaUiState() {
        val totalCargadas: Int
            get() = denuncias.size

        val denunciasFiltradas: List<Denuncia>
            get() = when (filtroActual) {
                FiltroEstado.TODAS -> denuncias
                FiltroEstado.PENDIENTES -> denuncias.filter { it.isPendiente }
                FiltroEstado.ATENDIDAS -> denuncias.filter { it.isAtendida }
            }

        val totalFiltradas: Int
            get() = denunciasFiltradas.size
    }

    data class Error(
        val mensaje: String,
        val esInicial: Boolean = true
    ) : ListaUiState()
}
