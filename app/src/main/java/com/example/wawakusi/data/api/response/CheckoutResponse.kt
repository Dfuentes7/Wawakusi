package com.example.wawakusi.data.api.response

data class CheckoutPaypalCreateResponse(
    val rpta: Boolean,
    val mensaje: String? = null,
    val ventaId: Int? = null,
    val pedidoId: Int? = null,
    val paypalOrderId: String? = null,
    val approvalUrl: String? = null
)

data class CheckoutPaypalCaptureResponse(
    val rpta: Boolean,
    val mensaje: String? = null,
    val ventaId: Int? = null,
    val codigo: String? = null
)

