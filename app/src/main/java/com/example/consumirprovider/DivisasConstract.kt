package com.example.consumirprovider

import android.net.Uri

object DivisasContract {
    // Autoridad del ContentProvider (debe coincidir con la definida en el proveedor)
    const val AUTHORITY = "com.example.proyectodivisacontent.provider"

    const val PATH_DIVISAS = "divisas"

    // URI base para acceder al ContentProvider
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_DIVISAS")

    // URI para consultar por moneda y rango de fechas (Esto es la manera a llamar desde fuera del proyecto o dentro del mismo)
    //val CONTENT_URI_BY_CURRENCY_AND_CHANGE_AND_DATE: Uri = Uri.parse("content://$AUTHORITY/$PATH_DIVISAS_BY_CURRENCY_AND_CHANGE_AND_DATE")

    // const val PATH_EXCHANGE_RATES = "exchange_rates"

    const val COLUMN_ID = "id"
    const val COLUMN_TIME_LAST_UPDATE = "date"
    const val COLUMN_EXCHANGE_RATE = "rate"
}