package com.example.wawakusi.data.api.request

data class AgregarCarritoItemRequest(
    val productoVarianteId: Int,
    val cantidad: Int
)

data class ActualizarCarritoItemRequest(
    val cantidad: Int
)

