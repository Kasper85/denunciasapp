package com.RodriguezRuiz.denunciasapp.data.model

import com.google.gson.annotations.SerializedName

data class Denuncia(
    @SerializedName("id")
    val id: Int,
    @SerializedName("dni")
    val dni: String,
    @SerializedName("fecha")
    val fecha: String,
    @SerializedName("tipo")
    val tipo: String,
    @SerializedName("direccion")
    val direccion: String,
    @SerializedName("descripcion")
    val descripcion: String,
    @SerializedName("estado")
    val estado: Int
) {
    val isPendiente: Boolean
        get() = estado == 1

    val isAtendida: Boolean
        get() = estado == 0

    val estadoTexto: String
        get() = if (isPendiente) "Pendiente" else "Atendida"
}
