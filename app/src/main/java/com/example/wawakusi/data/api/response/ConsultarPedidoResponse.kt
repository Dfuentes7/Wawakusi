package com.example.wawakusi.data.api.response

data class ConsultarPedidoResponse(
    val rpta: Boolean,
    val mensaje: String? = null,
    val pedido: VentaItemResponse? = null
)

