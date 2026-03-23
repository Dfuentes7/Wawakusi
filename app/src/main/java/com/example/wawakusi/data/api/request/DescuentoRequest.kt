package com.example.wawakusi.data.api.request

data class CrearDescuentoRequest(
    val nombre: String,
    val descripcion: String? = null,
    val porcentaje: Double,
    val fechaInicio: String,
    val fechaFin: String,
    val productoId: Int
)

