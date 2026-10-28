package com.RodriguezRuiz.denunciasapp.util

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.RodriguezRuiz.denunciasapp.R

enum class TipoDenuncia(
    val idTipo: String,
    val etiqueta: String,
    @property:DrawableRes val iconoRes: Int,
    @property:ColorRes val colorRes: Int,
    @property:ColorRes val colorBgRes: Int
) {
    ROBO(
        idTipo = "ROBO",
        etiqueta = "Robo",
        iconoRes = R.drawable.ic_robo,
        colorRes = R.color.tipo_robo,
        colorBgRes = R.color.tipo_robo_bg
    ),
    ASALTO(
        idTipo = "ASALTO",
        etiqueta = "Asalto",
        iconoRes = R.drawable.ic_asalto,
        colorRes = R.color.tipo_asalto,
        colorBgRes = R.color.tipo_asalto_bg
    ),
    VANDALISMO(
        idTipo = "VANDALISMO",
        etiqueta = "Vandalismo",
        iconoRes = R.drawable.ic_vandalismo,
        colorRes = R.color.tipo_vandalismo,
        colorBgRes = R.color.tipo_vandalismo_bg
    ),
    ACOSO(
        idTipo = "ACOSO",
        etiqueta = "Acoso",
        iconoRes = R.drawable.ic_acoso,
        colorRes = R.color.tipo_acoso,
        colorBgRes = R.color.tipo_acoso_bg
    ),
    OTRO(
        idTipo = "OTRO",
        etiqueta = "Otro",
        iconoRes = R.drawable.ic_otro,
        colorRes = R.color.tipo_otro,
        colorBgRes = R.color.tipo_otro_bg
    );

    companion object {
        fun desdeString(tipo: String?): TipoDenuncia {
            if (tipo.isNullOrBlank()) return OTRO
            val normalizado = tipo.trim().uppercase()
            return entries.find { it.idTipo == normalizado } ?: OTRO
        }
    }
}
