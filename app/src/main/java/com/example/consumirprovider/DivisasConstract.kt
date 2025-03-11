package com.example.consumirprovider

import android.net.Uri

object DivisasContract {
    // Autoridad del ContentProvider (debe coincidir con la definida en el proveedor)
    const val AUTHORITY = "com.example.proyectodivisa.provider"

    const val PATH_DIVISAS_BY_CURRENCY_AND_CHANGE_AND_DATE = "divisas_by_currency_and_change_and_date"

    // Path para acceder a la tabla de divisas
    const val PATH_DIVISAS = "divisas"

    // URI base para acceder al ContentProvider
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_DIVISAS_BY_CURRENCY_AND_CHANGE_AND_DATE")

    // Define los nombres de las columnas
    object DivisasColumns {
        const val ID = "id"
        const val DATE = "date"
        const val CURRENCY = "currency"
        const val RATE = "rate"
        const val CHANGE = "change"
    }
}