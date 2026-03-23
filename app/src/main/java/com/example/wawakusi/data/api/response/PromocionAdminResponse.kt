package com.example.wawakusi.data.api.response

data class PromocionAdminResponse(
    val id: Int,
    val nombre: String,
    val descripcion: String? = null,
    val porcentaje: Double,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: Int,
    val productoId: Int? = null,
    val productoNombre: String? = null
)

