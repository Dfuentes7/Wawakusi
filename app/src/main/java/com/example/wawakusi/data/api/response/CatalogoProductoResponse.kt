package com.example.wawakusi.data.api.response

data class CatalogoProductoResponse(
    val id: Int,
    val nombre: String,
    val descripcion: String? = null,
    val imagen: String? = null,
    val stock: Int? = null,
    val precioBase: Double? = null,
    val precioFinal: Double? = null,
    val descuento: DescuentoResponse? = null,
    val variantes: List<ProductoVarianteResponse> = emptyList()
)

data class ProductoVarianteResponse(
    val idVariante: Int,
    val talla: String? = null,
    val color: String? = null,
    val precio: Double? = null,
    val stock: Int? = null
)

data class DescuentoResponse(
    val id: Int,
    val nombre: String? = null,
    val porcentaje: Double? = null
)
