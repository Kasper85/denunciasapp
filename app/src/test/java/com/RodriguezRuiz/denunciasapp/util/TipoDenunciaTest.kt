package com.RodriguezRuiz.denunciasapp.util

import com.RodriguezRuiz.denunciasapp.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TipoDenunciaTest {

    @Test
    fun desdeString_tiposValidos_mapeanCorrectamente() {
        val robo = TipoDenuncia.desdeString("ROBO")
        assertEquals(TipoDenuncia.ROBO, robo)
        assertEquals("Robo", robo.etiqueta)
        assertEquals(R.drawable.ic_robo, robo.iconoRes)
        assertEquals(R.color.tipo_robo, robo.colorRes)
        assertEquals(R.color.tipo_robo_bg, robo.colorBgRes)

        assertEquals(TipoDenuncia.ASALTO, TipoDenuncia.desdeString("ASALTO"))
        assertEquals(TipoDenuncia.VANDALISMO, TipoDenuncia.desdeString("VANDALISMO"))
        assertEquals(TipoDenuncia.ACOSO, TipoDenuncia.desdeString("ACOSO"))
        assertEquals(TipoDenuncia.OTRO, TipoDenuncia.desdeString("OTRO"))
    }

    @Test
    fun desdeString_caseInsensitive_mapeaCorrectamente() {
        assertEquals(TipoDenuncia.ROBO, TipoDenuncia.desdeString("robo"))
        assertEquals(TipoDenuncia.ASALTO, TipoDenuncia.desdeString("Asalto "))
    }

    @Test
    fun desdeString_tipoDesconocidoONulo_retornaOtro() {
        assertEquals(TipoDenuncia.OTRO, TipoDenuncia.desdeString("HOMICIDIO"))
        assertEquals(TipoDenuncia.OTRO, TipoDenuncia.desdeString(""))
        assertEquals(TipoDenuncia.OTRO, TipoDenuncia.desdeString(null))
    }
}
