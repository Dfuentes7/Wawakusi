package com.example.wawakusi.data.api.response

data class CarritoResponse(
    val rpta: Boolean,
    val mensaje: String? = null,
    val carritoId: Int? = null,
    val items: List<CarritoItemResponse> = emptyList(),
    val total: Double? = null
)

data class CarritoItemResponse(
    val idDetalle: Int,
    val carritoId: Int,
    val productoId: Int,
    val productoNombre: String,
    val imagen: String? = null,
    val productoVarianteId: Int,
    val talla: String? = null,
    val color: String? = null,
    val precioUnitario: Double? = null,
    val cantidad: Int
)

