package com.RodriguezRuiz.denunciasapp.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class FormatoFechaTest {

    private fun obtenerMillisReferencia(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0
    ): Long {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun formatear_mismoDia_retornaHoyConHora() {
        // Referencia: 28 Octubre 2026, 12:00
        val ahora = obtenerMillisReferencia(2026, 10, 28, 12, 0)
        val fechaIso = "2026-10-28T09:52:00"

        val resultado = FormatoFecha.formatear(fechaIso, ahora)
        assertEquals("Hoy, 09:52", resultado)
    }

    @Test
    fun formatear_diaAnterior_retornaAyerConHora() {
        val ahora = obtenerMillisReferencia(2026, 10, 28, 12, 0)
        val fechaIso = "2026-10-27T18:30:00"

        val resultado = FormatoFecha.formatear(fechaIso, ahora)
        assertEquals("Ayer, 18:30", resultado)
    }

    @Test
    fun formatear_mismoAno_retornaDiaMesHora() {
        val ahora = obtenerMillisReferencia(2026, 10, 28, 12, 0)
        val fechaIso = "2026-05-15T14:05:00"

        val resultado = FormatoFecha.formatear(fechaIso, ahora)
        // Ejemplo: "15 may, 14:05" o "15 may., 14:05"
        assert(resultado.contains("15") && resultado.contains("14:05") && (resultado.contains("may") || resultado.contains("May")))
    }

    @Test
    fun formatear_otroAno_retornaDiaMesAnoHora() {
        val ahora = obtenerMillisReferencia(2026, 10, 28, 12, 0)
        val fechaIso = "2025-10-27T14:05:00"

        val resultado = FormatoFecha.formatear(fechaIso, ahora)
        assert(resultado.contains("2025") && resultado.contains("14:05"))
    }

    @Test
    fun formatear_textoInvalido_devuelveTextoOriginalSinLanzarExcepcion() {
        val ahora = obtenerMillisReferencia(2026, 10, 28, 12, 0)
        assertEquals("fecha-invalida", FormatoFecha.formatear("fecha-invalida", ahora))
        assertEquals("", FormatoFecha.formatear("", ahora))
        assertEquals("", FormatoFecha.formatear(null, ahora))
    }
}
