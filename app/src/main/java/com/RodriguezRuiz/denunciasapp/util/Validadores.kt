package com.RodriguezRuiz.denunciasapp.util

object Validadores {

    val TIPOS_PERMITIDOS = listOf("ROBO", "ASALTO", "VANDALISMO", "ACOSO", "OTRO")

    fun esDniValido(dni: String?): Boolean {
        if (dni == null) return false
        val regex = Regex("^\\d{8}$")
        return regex.matches(dni)
    }

    fun esTipoValido(tipo: String?): Boolean {
        if (tipo.isNullOrBlank()) return false
        return TIPOS_PERMITIDOS.contains(tipo.trim().uppercase())
    }

    fun esDireccionValida(direccion: String?): Boolean {
        if (direccion == null) return false
        val trimmed = direccion.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 200
    }

    fun esDescripcionValida(descripcion: String?): Boolean {
        if (descripcion == null) return false
        val trimmed = descripcion.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 255
    }

    fun validarRegistro(
        dni: String?,
        tipo: String?,
        direccion: String?,
        descripcion: String?
    ): String? {
        if (!esDniValido(dni)) {
            return "El DNI debe tener exactamente 8 dígitos"
        }
        if (!esTipoValido(tipo)) {
            return "Debe seleccionar un tipo de denuncia válido"
        }
        if (!esDireccionValida(direccion)) {
            return "La dirección es obligatoria (máx. 200 caracteres)"
        }
        if (!esDescripcionValida(descripcion)) {
            return "La descripción es obligatoria (máx. 255 caracteres)"
        }
        return null
    }
}
