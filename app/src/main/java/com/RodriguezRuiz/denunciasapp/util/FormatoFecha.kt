package com.RodriguezRuiz.denunciasapp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatoFecha {

    private val LOCALE_ES = Locale.forLanguageTag("es-PE")

    fun formatear(
        fechaIso: String?,
        ahoraMillis: Long = System.currentTimeMillis()
    ): String {
        if (fechaIso.isNullOrBlank()) return fechaIso.orEmpty()

        val fechaParseada: Date = try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            parser.parse(fechaIso) ?: return fechaIso
        } catch (_: Exception) {
            return fechaIso
        }

        val calFecha = Calendar.getInstance().apply { time = fechaParseada }
        val calAhora = Calendar.getInstance().apply { timeInMillis = ahoraMillis }

        val formatHora = SimpleDateFormat("HH:mm", Locale.US)
        val horaStr = formatHora.format(fechaParseada)

        val yearFecha = calFecha.get(Calendar.YEAR)
        val yearAhora = calAhora.get(Calendar.YEAR)
        val dayOfYearFecha = calFecha.get(Calendar.DAY_OF_YEAR)
        val dayOfYearAhora = calAhora.get(Calendar.DAY_OF_YEAR)

        return when {
            yearFecha == yearAhora && dayOfYearFecha == dayOfYearAhora -> {
                "Hoy, $horaStr"
            }
            yearFecha == yearAhora && dayOfYearFecha == dayOfYearAhora - 1 -> {
                "Ayer, $horaStr"
            }
            yearFecha == yearAhora -> {
                val formatMes = SimpleDateFormat("d MMM, HH:mm", LOCALE_ES)
                formatMes.format(fechaParseada)
            }
            else -> {
                val formatCompleto = SimpleDateFormat("d MMM yyyy, HH:mm", LOCALE_ES)
                formatCompleto.format(fechaParseada)
            }
        }
    }
}
