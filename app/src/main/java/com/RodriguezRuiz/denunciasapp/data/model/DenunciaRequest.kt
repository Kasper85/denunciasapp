package com.RodriguezRuiz.denunciasapp.data.model

import com.google.gson.annotations.SerializedName

data class DenunciaRequest(
    @SerializedName("dni")
    val dni: String,
    @SerializedName("tipo")
    val tipo: String,
    @SerializedName("direccion")
    val direccion: String,
    @SerializedName("descripcion")
    val descripcion: String
)
