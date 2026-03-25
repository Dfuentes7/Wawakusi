package com.example.wawakusi.data.api.response

data class VentaListResponse(
    val rpta: Boolean,
    val mensaje: String? = null,
    val ventas: List<VentaItemResponse> = emptyList()
)

data class VentaItemResponse(
    val idVenta: Int,
    val codigo: String? = null,
    val total: Double? = null,
    val estado: Int? = null,
    val createdAt: String? = null,
    val envioEstado: Int? = null,
    val direccionEnvio: String? = null,
    val pagoEstado: Int? = null,
    val metodoPago: String? = null,
    val clienteId: Int? = null,
    val clienteNombre: String? = null,
    val clienteEmail: String? = null,
    val clienteTelefono: String? = null,
    val totalItems: Int? = null,
    val detalles: List<VentaDetalleItemResponse> = emptyList()
)

data class VentaDetalleItemResponse(
    val productoNombre: String? = null,
    val imagen: String? = null,
    val talla: String? = null,
    val color: String? = null,
    val cantidad: Int? = null,
    val precioUnitario: Double? = null
)

