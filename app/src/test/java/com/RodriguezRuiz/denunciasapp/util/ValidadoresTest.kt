package com.RodriguezRuiz.denunciasapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadoresTest {

    @Test
    fun validarDni_casosValidos() {
        assertTrue(Validadores.esDniValido("12345678"))
        assertTrue(Validadores.esDniValido("00000000"))
        assertTrue(Validadores.esDniValido("87654321"))
    }

    @Test
    fun validarDni_casosInvalidos() {
        assertFalse(Validadores.esDniValido(""))
        assertFalse(Validadores.esDniValido("1234567")) // 7 dígitos
        assertFalse(Validadores.esDniValido("123456789")) // 9 dígitos
        assertFalse(Validadores.esDniValido("1234567A")) // letras
        assertFalse(Validadores.esDniValido(" 12345678 ")) // espacios
        assertFalse(Validadores.esDniValido(null))
    }

    @Test
    fun validarTipo_casosPermitidos() {
        assertTrue(Validadores.esTipoValido("ROBO"))
        assertTrue(Validadores.esTipoValido("ASALTO"))
        assertTrue(Validadores.esTipoValido("VANDALISMO"))
        assertTrue(Validadores.esTipoValido("ACOSO"))
        assertTrue(Validadores.esTipoValido("OTRO"))
        // Case-insensitive
        assertTrue(Validadores.esTipoValido("robo"))
    }

    @Test
    fun validarTipo_casosInvalidos() {
        assertFalse(Validadores.esTipoValido(""))
        assertFalse(Validadores.esTipoValido("HOMICIDIO"))
        assertFalse(Validadores.esTipoValido(null))
    }

    @Test
    fun validarDireccion_casos() {
        assertTrue(Validadores.esDireccionValida("Av. Bolognesi 123"))
        assertFalse(Validadores.esDireccionValida(""))
        assertFalse(Validadores.esDireccionValida("   "))
        assertFalse(Validadores.esDireccionValida(null))
        assertFalse(Validadores.esDireccionValida("a".repeat(201)))
        assertTrue(Validadores.esDireccionValida("a".repeat(200)))
    }

    @Test
    fun validarDescripcion_casos() {
        assertTrue(Validadores.esDescripcionValida("Descripción del hecho"))
        assertFalse(Validadores.esDescripcionValida(""))
        assertFalse(Validadores.esDescripcionValida("   "))
        assertFalse(Validadores.esDescripcionValida(null))
        assertFalse(Validadores.esDescripcionValida("a".repeat(256)))
        assertTrue(Validadores.esDescripcionValida("a".repeat(255)))
    }

    @Test
    fun validarRegistroCompleto_retornaNullSiEsValido() {
        val error = Validadores.validarRegistro(
            dni = "72345678",
            tipo = "ROBO",
            direccion = "Calle Real 100",
            descripcion = "Sustracción de pertenencias"
        )
        assertNull(error)
    }

    @Test
    fun validarRegistroCompleto_retornaErrorEsperado() {
        assertEquals("El DNI debe tener exactamente 8 dígitos", Validadores.validarRegistro("123", "ROBO", "Dir", "Desc"))
        assertEquals("Debe seleccionar un tipo de denuncia válido", Validadores.validarRegistro("12345678", "INVALIDO", "Dir", "Desc"))
        assertEquals("La dirección es obligatoria (máx. 200 caracteres)", Validadores.validarRegistro("12345678", "ROBO", "", "Desc"))
        assertEquals("La descripción es obligatoria (máx. 255 caracteres)", Validadores.validarRegistro("12345678", "ROBO", "Dir", ""))
    }
}
