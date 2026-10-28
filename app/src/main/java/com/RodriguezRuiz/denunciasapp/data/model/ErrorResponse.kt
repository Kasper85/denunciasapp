package com.RodriguezRuiz.denunciasapp.data.model

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("timestamp")
    val timestamp: String? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("mensaje")
    val mensaje: String? = null,
    @SerializedName("detalles")
    val detalles: Map<String, String>? = null
)
